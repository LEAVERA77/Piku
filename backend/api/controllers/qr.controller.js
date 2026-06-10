const { query, withTransaction } = require('../services/neon.service');
const { calcularDistancia } = require('../services/nominatim.service');
const { generarCodigoUnico, responderError } = require('../utils/helpers');
const { calcularPuntosCompra, saldoSeguro, resumenPuntosGanados } = require('../services/puntos.service');
const {
  verificarLimitePuntos,
  incrementarUsoMensual,
} = require('../services/suscripcion.service');

const MINUTOS_EXPIRACION_QR = parseInt(process.env.QR_EXPIRACION_MINUTOS, 10) || 15;

/** @deprecated use calcularPuntosCompra */
async function calcularPuntos(reglas, montoTransaccion) {
  return calcularPuntosCompra(montoTransaccion, reglas);
}

/**
 * Verifica que el cliente esté dentro del radio del comercio (anti-fraude GPS).
 */
function verificarGPS(comercio, latCliente, lonCliente) {
  if (comercio.lat == null || comercio.lon == null) {
    return { ok: true, distancia: null, motivo: 'Comercio sin coordenadas configuradas' };
  }
  if (latCliente == null || lonCliente == null) {
    return { ok: false, distancia: null, motivo: 'Ubicación del cliente requerida' };
  }

  const distancia = calcularDistancia(
    parseFloat(comercio.lat),
    parseFloat(comercio.lon),
    parseFloat(latCliente),
    parseFloat(lonCliente)
  );
  const radio = parseInt(comercio.radio_metros, 10) || 100;
  return {
    ok: distancia <= radio,
    distancia,
    radioPermitido: radio,
    motivo: distancia <= radio ? 'OK' : `Estás a ${distancia}m (máx ${radio}m)`,
  };
}

/**
 * Evita doble uso del mismo código QR.
 */
async function verificarQRUnico(client, codigo) {
  const r = await client.query(
    `SELECT id, usado, expira_at, comercio_id, monto_transaccion, puntos_calculados
     FROM piku_qr_dinamicos WHERE codigo = $1 FOR UPDATE`,
    [codigo]
  );
  if (!r.rows.length) return { valido: false, motivo: 'Código QR no encontrado' };
  const qr = r.rows[0];
  if (qr.usado) return { valido: false, motivo: 'Este QR ya fue utilizado' };
  if (new Date(qr.expira_at) < new Date()) {
    return { valido: false, motivo: 'El código QR expiró' };
  }
  return { valido: true, qr };
}

/**
 * Valida escaneo de QR y acredita puntos al cliente.
 */
async function validarEscaneo(req, res) {
  try {
    const codigo = String(req.body.codigo || req.body.qr || '').trim();
    const lat = req.body.lat != null ? parseFloat(req.body.lat) : null;
    const lon = req.body.lon != null ? parseFloat(req.body.lon) : null;

    if (!codigo) return responderError(res, 400, 'Código QR requerido');

    const resultado = await withTransaction(async (client) => {
      const verificacion = await verificarQRUnico(client, codigo);
      if (!verificacion.valido) throw new Error(verificacion.motivo);

      const qr = verificacion.qr;

      const comercioRes = await client.query(
        `SELECT id, nombre, lat, lon, suscripcion_activa, radio_metros
         FROM piku_comercios WHERE id = $1`,
        [qr.comercio_id]
      );
      const fila = comercioRes.rows[0];
      if (!fila || fila.suscripcion_activa === false) {
        throw new Error('Comercio no disponible');
      }
      const comercio = fila;

      const gps = verificarGPS(comercio, lat, lon);
      if (!gps.ok) throw new Error(gps.motivo);

      const reglasRes = await client.query(
        'SELECT * FROM piku_reglas_puntos WHERE comercio_id = $1',
        [comercio.id]
      );
      const reglas = reglasRes.rows[0] || {
        monto_minimo: 0,
        max_puntos_por_dia: 500,
      };
      if (reglas.activo === false) {
        throw new Error('El programa de puntos de este comercio está pausado');
      }

      let puntos = qr.puntos_calculados;
      if (puntos == null) {
        puntos = await calcularPuntosCompra(qr.monto_transaccion, reglas);
      } else {
        puntos = parseInt(puntos, 10) || 0;
      }

      // Límite diario por comercio
      const hoy = await client.query(
        `SELECT COALESCE(SUM(puntos), 0)::int AS total
         FROM piku_transacciones_puntos
         WHERE usuario_id = $1 AND comercio_id = $2 AND tipo = 'ganado'
           AND created_at >= CURRENT_DATE`,
        [req.user.id, comercio.id]
      );
      const acumuladoHoy = hoy.rows[0].total;
      const maxDia = parseInt(reglas.max_puntos_por_dia, 10) || 500;
      if (acumuladoHoy + puntos > maxDia) {
        puntos = Math.max(0, maxDia - acumuladoHoy);
      }
      if (puntos <= 0) throw new Error('Alcanzaste el límite de puntos diarios en este comercio');

      const limitePlan = await verificarLimitePuntos(comercio.id, puntos, client);
      if (!limitePlan.permitido) {
        throw new Error(
          'Límite de puntos mensual alcanzado. Actualizá tu plan en Más → Suscripción'
        );
      }

      const user = await client.query(
        'SELECT puntos_saldo FROM piku_usuarios WHERE id = $1 FOR UPDATE',
        [req.user.id]
      );
      const saldoActual = saldoSeguro(user.rows[0]?.puntos_saldo);
      const nuevoSaldo = saldoActual + puntos;

      await client.query(
        'UPDATE piku_usuarios SET puntos_saldo = $1, updated_at = NOW() WHERE id = $2',
        [nuevoSaldo, req.user.id]
      );

      await client.query(
        `UPDATE piku_qr_dinamicos SET usado = TRUE, usado_por = $1, usado_at = NOW() WHERE id = $2`,
        [req.user.id, qr.id]
      );

      await client.query(
        `INSERT INTO piku_transacciones_puntos
         (usuario_id, comercio_id, tipo, puntos, descripcion, qr_codigo_id)
         VALUES ($1, $2, 'ganado', $3, $4, $5)`,
        [
          req.user.id,
          comercio.id,
          puntos,
          `Compra en ${comercio.nombre}`,
          qr.id,
        ]
      );

      await incrementarUsoMensual(comercio.id, puntos, client);

      return { puntos, nuevoSaldo, comercio, distancia: gps.distancia };
    });

    const resumen = await resumenPuntosGanados(resultado.puntos);

    return res.json({
      mensaje: `¡Ganaste ${resultado.puntos} PP! Valen $${resumen.valorCanjeArs.toLocaleString('es-AR')} ARS en tu próxima compra`,
      puntosGanados: resultado.puntos,
      saldoActual: resultado.nuevoSaldo,
      comercio: resultado.comercio.nombre,
      distanciaMetros: resultado.distancia,
      valorCanjeArs: resumen.valorCanjeArs,
      pesosPorDolar: resumen.pesosPorDolar,
      valorPuntoUsd: resumen.valorPuntoUsd,
    });
  } catch (error) {
    console.error('validarEscaneo:', error);
    const status = /QR|ubicación|límite|Comercio|Suscripción|mensual|pausado/i.test(error.message) ? 400 : 500;
    if (/mensual/i.test(error.message)) {
      return responderError(res, 403, error.message);
    }
    return responderError(res, status, error.message);
  }
}

/**
 * Genera un QR dinámico (solo comercio autenticado).
 */
async function generarQR(req, res) {
  try {
    const comercioId = req.comercioId || req.user.comercio_id;
    if (!comercioId) return responderError(res, 403, 'Usuario sin comercio asociado');

    const monto = parseFloat(req.body.montoTransaccion ?? req.body.monto ?? 0) || 0;
    const lat = req.body.lat != null ? parseFloat(req.body.lat) : null;
    const lon = req.body.lon != null ? parseFloat(req.body.lon) : null;

    const reglasRes = await query(
      'SELECT * FROM piku_reglas_puntos WHERE comercio_id = $1',
      [comercioId]
    );
    const reglas = reglasRes.rows[0] || { monto_minimo: 0 };
    if (reglas.activo === false) {
      return responderError(
        res,
        400,
        'Tu programa de puntos está pausado. Activalo en Más → Reglas de puntos'
      );
    }
    const puntosCalculados = await calcularPuntosCompra(monto, reglas);

    const limitePlan = await verificarLimitePuntos(comercioId, puntosCalculados);
    if (!limitePlan.permitido) {
      return responderError(
        res,
        403,
        'Límite de puntos mensual alcanzado. Actualizá tu plan en Más → Suscripción',
        {
          puntos_usados_mes: limitePlan.puntosUsados,
          puntos_limite: limitePlan.puntosLimite,
          plan: limitePlan.plan,
        }
      );
    }
    const codigo = generarCodigoUnico('QR');
    const expiraAt = new Date(Date.now() + MINUTOS_EXPIRACION_QR * 60 * 1000);

    const insert = await query(
      `INSERT INTO piku_qr_dinamicos
       (comercio_id, codigo, monto_transaccion, puntos_calculados, expira_at, lat_generacion, lon_generacion)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING id, codigo, monto_transaccion, puntos_calculados, expira_at`,
      [comercioId, codigo, monto, puntosCalculados, expiraAt, lat, lon]
    );

    return res.status(201).json({
      mensaje: 'QR generado correctamente',
      qr: insert.rows[0],
      expiraEnMinutos: MINUTOS_EXPIRACION_QR,
    });
  } catch (error) {
    console.error('generarQR:', error);
    return responderError(res, 500, 'Error al generar QR');
  }
}

module.exports = {
  validarEscaneo,
  generarQR,
  calcularPuntos,
  verificarGPS,
  verificarQRUnico,
};
