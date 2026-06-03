const { query, withTransaction } = require('../services/neon.service');
const { registrarEvento } = require('../services/eventos.service');
const { uploadImage, configurado: cloudinaryOk } = require('../services/cloudinary.service');
const { generarCodigoUnico, responderError } = require('../utils/helpers');
const {
  otorgarBonoBienvenida,
  otorgarBonoCompartir,
  debitarPuntos,
  asegurarCodigoReferido,
  saldoSeguro,
  resumenSaldoPuntos,
} = require('../services/puntos.service');

async function asegurarBonoInicial(usuarioId) {
  try {
    return await withTransaction(async (client) => {
      await asegurarCodigoReferido(client, usuarioId);
      return otorgarBonoBienvenida(client, usuarioId);
    });
  } catch (error) {
    console.warn('asegurarBonoInicial:', error.message);
    return { otorgado: false, puntos: 0 };
  }
}

/**
 * Saldo de puntos del cliente autenticado.
 */
async function getSaldoPuntos(req, res) {
  try {
    const usuarioId = req.user.id;
    await asegurarBonoInicial(usuarioId);

    const result = await query(
      'SELECT puntos_saldo FROM piku_usuarios WHERE id = $1',
      [usuarioId]
    );
    const puntos = saldoSeguro(result.rows[0]?.puntos_saldo);
    const resumen = await resumenSaldoPuntos(puntos);

    return res.json({
      puntos,
      equivalenciaDescuento: resumen.equivalenciaDescuentoArs,
      equivalenciaDescuentoArs: resumen.equivalenciaDescuentoArs,
      descuentoUsd: resumen.descuentoUsd,
      pesosPorDolar: resumen.pesosPorDolar,
      valorPuntoUsd: resumen.valorPuntoUsd,
      tasaReintegro: resumen.tasaReintegro,
      mensaje: `Tus ${puntos} PP valen $${resumen.equivalenciaDescuentoArs.toLocaleString('es-AR')} ARS`,
    });
  } catch (error) {
    console.error('getSaldoPuntos:', error);
    return responderError(res, 500, 'Error al obtener saldo');
  }
}

/**
 * Historial de transacciones de puntos.
 */
async function getHistorialPuntos(req, res) {
  try {
    const limite = Math.min(parseInt(req.query.limite, 10) || 50, 100);
    try {
      const result = await query(
        `SELECT t.id::text AS id, t.tipo, t.puntos, t.descripcion, t.created_at,
                c.nombre AS comercio_nombre
         FROM piku_transacciones_puntos t
         LEFT JOIN piku_comercios c ON c.id = t.comercio_id
         WHERE t.usuario_id = $1
         ORDER BY t.created_at DESC
         LIMIT $2`,
        [req.user.id, limite]
      );
      return res.json({ transacciones: result.rows });
    } catch (dbError) {
      if (/piku_transacciones_puntos|does not exist|no existe/i.test(dbError.message)) {
        console.warn('getHistorialPuntos: tabla ausente, devolviendo vacío');
        return res.json({ transacciones: [] });
      }
      throw dbError;
    }
  } catch (error) {
    console.error('getHistorialPuntos:', error);
    return responderError(res, 500, 'Error al obtener historial');
  }
}

/**
 * Recompensas activas disponibles para canjear.
 */
async function getRecompensasDisponibles(req, res) {
  try {
    const result = await query(
      `SELECT r.id, r.comercio_id, r.nombre, r.descripcion, r.puntos_requeridos,
              r.icono, r.imagen_url, r.stock, c.nombre AS comercio_nombre
       FROM piku_recompensas r
       INNER JOIN piku_comercios c ON c.id = r.comercio_id AND COALESCE(c.suscripcion_activa, TRUE) = TRUE
       WHERE r.activo = TRUE
         AND (r.stock IS NULL OR r.stock > 0)
         AND (r.fecha_inicio IS NULL OR r.fecha_inicio <= NOW())
         AND (r.fecha_fin IS NULL OR r.fecha_fin >= NOW())
         AND (
           r.max_usos_totales IS NULL OR r.max_usos_totales = 0
           OR r.usos_actuales < r.max_usos_totales
         )
       ORDER BY r.puntos_requeridos ASC`
    );

    const puntos = req.user.puntos_saldo;
    const recompensas = result.rows.map((r) => ({
      ...r,
      puedeCanjear: puntos >= r.puntos_requeridos,
    }));

    return res.json({ puntosDisponibles: puntos, recompensas });
  } catch (error) {
    console.error('getRecompensasDisponibles:', error);
    return responderError(res, 500, 'Error al listar recompensas');
  }
}

/**
 * Canjear puntos por una recompensa.
 */
async function canjearRecompensa(req, res) {
  try {
    const recompensaId = req.body.recompensaId || req.body.recompensa_id;
    if (!recompensaId) return responderError(res, 400, 'recompensaId requerido');

    const resultado = await withTransaction(async (client) => {
      const rec = await client.query(
        `SELECT r.*, c.nombre AS comercio_nombre
         FROM piku_recompensas r
         INNER JOIN piku_comercios c ON c.id = r.comercio_id
         WHERE r.id = $1 AND r.activo = TRUE
         FOR UPDATE`,
        [recompensaId]
      );
      if (!rec.rows.length) throw new Error('Recompensa no encontrada');

      const recompensa = rec.rows[0];
      if (recompensa.stock != null && recompensa.stock <= 0) {
        throw new Error('Recompensa sin stock');
      }
      const ahora = new Date();
      if (recompensa.fecha_inicio && new Date(recompensa.fecha_inicio) > ahora) {
        throw new Error('Esta oferta aún no está vigente');
      }
      if (recompensa.fecha_fin && new Date(recompensa.fecha_fin) < ahora) {
        throw new Error('Esta oferta ya venció');
      }
      if (
        recompensa.max_usos_totales > 0 &&
        recompensa.usos_actuales >= recompensa.max_usos_totales
      ) {
        throw new Error('Esta oferta alcanzó el límite de canjes');
      }
      if (recompensa.max_usos_por_usuario > 0) {
        const usosUsuario = await client.query(
          'SELECT COUNT(*)::int AS n FROM piku_canjes WHERE recompensa_id = $1 AND usuario_id = $2',
          [recompensa.id, req.user.id]
        );
        if (usosUsuario.rows[0].n >= recompensa.max_usos_por_usuario) {
          throw new Error('Ya alcanzaste el límite de canjes para esta oferta');
        }
      }

      const user = await client.query(
        'SELECT puntos_saldo FROM piku_usuarios WHERE id = $1 FOR UPDATE',
        [req.user.id]
      );
      const saldo = saldoSeguro(user.rows[0]?.puntos_saldo);
      if (saldo < recompensa.puntos_requeridos) {
        throw new Error('Puntos insuficientes');
      }

      const codigoCanje = generarCodigoUnico('CANJE');

      const debito = await debitarPuntos(client, {
        usuarioId: req.user.id,
        comercioId: recompensa.comercio_id,
        puntos: recompensa.puntos_requeridos,
        descripcion: `Canje: ${recompensa.nombre}`,
        extras: {
          recompensaId: recompensa.id,
          codigoCanje,
        },
      });

      await client.query(
        `INSERT INTO piku_canjes (usuario_id, recompensa_id, puntos_usados, codigo_canje)
         VALUES ($1, $2, $3, $4)`,
        [req.user.id, recompensa.id, recompensa.puntos_requeridos, codigoCanje]
      );

      if (recompensa.stock != null) {
        await client.query(
          'UPDATE piku_recompensas SET stock = stock - 1, updated_at = NOW() WHERE id = $1',
          [recompensa.id]
        );
      }

      await client.query(
        'UPDATE piku_recompensas SET usos_actuales = COALESCE(usos_actuales, 0) + 1, updated_at = NOW() WHERE id = $1',
        [recompensa.id]
      );

      return { recompensa, codigoCanje, nuevoSaldo: debito.saldo };
    });

    try {
      await registrarEvento(req.user.id, 'canje', resultado.recompensa.comercio_id, {
        recompensa_id: resultado.recompensa.id,
      });
    } catch (e) {
      console.warn('evento canje:', e.message);
    }

    return res.json({
      mensaje: `¡Canjeaste ${resultado.recompensa.nombre}!`,
      codigoCanje: resultado.codigoCanje,
      puntosRestantes: resultado.nuevoSaldo,
      recompensa: {
        id: resultado.recompensa.id,
        nombre: resultado.recompensa.nombre,
        comercio: resultado.recompensa.comercio_nombre,
      },
    });
  } catch (error) {
    console.error('canjearRecompensa:', error);
    const status = /insuficientes|stock|encontrada/i.test(error.message) ? 400 : 500;
    return responderError(res, status, error.message);
  }
}

async function uploadAvatar(req, res) {
  try {
    if (!req.file) return responderError(res, 400, 'Archivo de imagen requerido');
    if (!cloudinaryOk) return responderError(res, 503, 'Cloudinary no configurado en el servidor');

    const dataUri = `data:${req.file.mimetype};base64,${req.file.buffer.toString('base64')}`;
    const { url } = await uploadImage(dataUri, 'avatars');

    const updated = await query(
      'UPDATE piku_usuarios SET avatar_url = $1, updated_at = NOW() WHERE id = $2 RETURNING avatar_url',
      [url, req.user.id]
    );

    return res.json({ mensaje: 'Avatar actualizado', avatar_url: updated.rows[0].avatar_url });
  } catch (error) {
    console.error('uploadAvatar:', error);
    return responderError(res, 500, 'Error al subir avatar', { detail: error.message });
  }
}

async function getDesglosePuntos(req, res) {
  try {
    const usuarioId = req.user.id;
    await asegurarBonoInicial(usuarioId);

    const saldoRes = await query(
      'SELECT puntos_saldo FROM piku_usuarios WHERE id = $1',
      [usuarioId]
    );
    const saldo = saldoSeguro(saldoRes.rows[0]?.puntos_saldo);

    let row = { compras: 0, bonos: 0, canjes: 0 };
    try {
      const agg = await query(
        `SELECT
           COALESCE(SUM(CASE
             WHEN tipo = 'ganado' AND (
               descripcion ILIKE 'Compra%' OR descripcion ILIKE 'Compra en%'
             ) THEN puntos ELSE 0 END), 0)::int AS compras,
           COALESCE(SUM(CASE
             WHEN tipo = 'ganado' AND NOT (
               descripcion ILIKE 'Compra%' OR descripcion ILIKE 'Compra en%'
             ) THEN puntos ELSE 0 END), 0)::int AS bonos,
           COALESCE(SUM(CASE
             WHEN tipo = 'canjeado' THEN ABS(puntos) ELSE 0 END), 0)::int AS canjes
         FROM piku_transacciones_puntos
         WHERE usuario_id = $1`,
        [usuarioId]
      );
      row = agg.rows[0] || row;
    } catch (dbError) {
      if (!/piku_transacciones_puntos|does not exist|no existe/i.test(dbError.message)) {
        throw dbError;
      }
    }

    return res.json({
      saldo,
      compras: row.compras,
      bonos: row.bonos,
      canjes: row.canjes,
    });
  } catch (error) {
    console.error('getDesglosePuntos:', error);
    return responderError(res, 500, 'Error al obtener desglose de puntos');
  }
}

async function bonificacionBienvenida(req, res) {
  try {
    const resultado = await asegurarBonoInicial(req.user.id);
    if (!resultado.otorgado) {
      const saldoRes = await query(
        'SELECT puntos_saldo FROM piku_usuarios WHERE id = $1',
        [req.user.id]
      );
      return res.json({
        otorgado: false,
        puntos: 0,
        saldo: saldoSeguro(saldoRes.rows[0]?.puntos_saldo),
        mensaje: 'Ya recibiste tu bono de bienvenida',
      });
    }
    return res.json({
      otorgado: true,
      puntos: resultado.puntos,
      saldo: resultado.saldo,
      mensaje: `¡Te regalamos ${resultado.puntos} puntos de bienvenida!`,
    });
  } catch (error) {
    console.error('bonificacionBienvenida:', error);
    return responderError(res, 500, 'Error al acreditar bienvenida');
  }
}

async function bonificacionCompartir(req, res) {
  try {
    const resultado = await otorgarBonoCompartir(req.user.id);
    if (!resultado.otorgado) {
      return responderError(res, 400, resultado.mensaje);
    }
    return res.json(resultado);
  } catch (error) {
    console.error('bonificacionCompartir:', error);
    return responderError(res, 500, 'Error al acreditar puntos');
  }
}

module.exports = {
  getSaldoPuntos,
  getHistorialPuntos,
  getDesglosePuntos,
  bonificacionBienvenida,
  bonificacionCompartir,
  getRecompensasDisponibles,
  canjearRecompensa,
  uploadAvatar,
};
