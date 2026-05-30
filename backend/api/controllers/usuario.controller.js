const { query, withTransaction } = require('../services/neon.service');
const { registrarEvento } = require('../services/eventos.service');
const { generarCodigoUnico, responderError } = require('../utils/helpers');

/**
 * Saldo de puntos del cliente autenticado.
 */
async function getSaldoPuntos(req, res) {
  try {
    const usuarioId = req.user.id;
    const result = await query(
      'SELECT puntos_saldo FROM piku_usuarios WHERE id = $1',
      [usuarioId]
    );
    const puntos = result.rows[0]?.puntos_saldo ?? 0;
    const equivalenciaDescuento = Math.floor(puntos / 10);

    return res.json({
      puntos,
      equivalenciaDescuento,
      mensaje: `≈ $${equivalenciaDescuento} en descuentos`,
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
    const result = await query(
      `SELECT t.id, t.tipo, t.puntos, t.descripcion, t.created_at,
              c.nombre AS comercio_nombre
       FROM piku_transacciones_puntos t
       LEFT JOIN piku_comercios c ON c.id = t.comercio_id
       WHERE t.usuario_id = $1
       ORDER BY t.created_at DESC
       LIMIT $2`,
      [req.user.id, limite]
    );

    return res.json({ transacciones: result.rows });
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
      const saldo = user.rows[0].puntos_saldo;
      if (saldo < recompensa.puntos_requeridos) {
        throw new Error('Puntos insuficientes');
      }

      const codigoCanje = generarCodigoUnico('CANJE');
      const nuevoSaldo = saldo - recompensa.puntos_requeridos;

      await client.query(
        'UPDATE piku_usuarios SET puntos_saldo = $1, updated_at = NOW() WHERE id = $2',
        [nuevoSaldo, req.user.id]
      );

      await client.query(
        `INSERT INTO piku_canjes (usuario_id, recompensa_id, puntos_usados, codigo_canje)
         VALUES ($1, $2, $3, $4)`,
        [req.user.id, recompensa.id, recompensa.puntos_requeridos, codigoCanje]
      );

      await client.query(
        `INSERT INTO piku_transacciones_puntos
         (usuario_id, comercio_id, tipo, puntos, descripcion, recompensa_id, codigo_canje)
         VALUES ($1, $2, 'canjeado', $3, $4, $5, $6)`,
        [
          req.user.id,
          recompensa.comercio_id,
          -recompensa.puntos_requeridos,
          `Canje: ${recompensa.nombre}`,
          recompensa.id,
          codigoCanje,
        ]
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

      return { recompensa, codigoCanje, nuevoSaldo };
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

module.exports = {
  getSaldoPuntos,
  getHistorialPuntos,
  getRecompensasDisponibles,
  canjearRecompensa,
};
