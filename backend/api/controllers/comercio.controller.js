const { query } = require('../services/neon.service');
const { sanitizarInput, responderError } = require('../utils/helpers');

function getComercioId(req) {
  return req.user.comercio_id;
}

/**
 * Obtiene reglas de puntos del comercio.
 */
async function getReglasPuntos(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const result = await query('SELECT * FROM piku_reglas_puntos WHERE comercio_id = $1', [comercioId]);
    if (!result.rows.length) {
      return res.json({
        reglas: {
          comercio_id: comercioId,
          puntos_por_peso: 1,
          monto_minimo: 0,
          puntos_fijos: 10,
          max_puntos_por_dia: 500,
          activo: true,
        },
      });
    }
    return res.json({ reglas: result.rows[0] });
  } catch (error) {
    console.error('getReglasPuntos:', error);
    return responderError(res, 500, 'Error al obtener reglas');
  }
}

/**
 * Actualiza reglas de puntos.
 */
async function updateReglasPuntos(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const puntosPorPeso = req.body.puntosPorPeso ?? req.body.puntos_por_peso ?? 1;
    const montoMinimo = req.body.montoMinimo ?? req.body.monto_minimo ?? 0;
    const puntosFijos = req.body.puntosFijos ?? req.body.puntos_fijos ?? 10;
    const maxDia = req.body.maxPuntosPorDia ?? req.body.max_puntos_por_dia ?? 500;
    const activo = req.body.activo !== false;

    const result = await query(
      `INSERT INTO piku_reglas_puntos
       (comercio_id, puntos_por_peso, monto_minimo, puntos_fijos, max_puntos_por_dia, activo, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6, NOW())
       ON CONFLICT (comercio_id) DO UPDATE SET
         puntos_por_peso = EXCLUDED.puntos_por_peso,
         monto_minimo = EXCLUDED.monto_minimo,
         puntos_fijos = EXCLUDED.puntos_fijos,
         max_puntos_por_dia = EXCLUDED.max_puntos_por_dia,
         activo = EXCLUDED.activo,
         updated_at = NOW()
       RETURNING *`,
      [comercioId, puntosPorPeso, montoMinimo, puntosFijos, maxDia, activo]
    );

    return res.json({ mensaje: 'Reglas actualizadas', reglas: result.rows[0] });
  } catch (error) {
    console.error('updateReglasPuntos:', error);
    return responderError(res, 500, 'Error al actualizar reglas');
  }
}

/**
 * Lista recompensas del comercio.
 */
async function getRecompensas(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const result = await query(
      'SELECT * FROM piku_recompensas WHERE comercio_id = $1 ORDER BY created_at DESC',
      [comercioId]
    );
    return res.json({ recompensas: result.rows });
  } catch (error) {
    console.error('getRecompensas:', error);
    return responderError(res, 500, 'Error al listar recompensas');
  }
}

/**
 * Crea una recompensa.
 */
async function createRecompensa(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const nombre = sanitizarInput(req.body.nombre, 255);
    const descripcion = sanitizarInput(req.body.descripcion, 1000);
    const puntos = parseInt(req.body.puntosRequeridos ?? req.body.puntos_requeridos, 10);
    const icono = sanitizarInput(req.body.icono, 16) || '🎁';
    const stock = req.body.stock != null ? parseInt(req.body.stock, 10) : null;
    const imagenUrl = sanitizarInput(req.body.imagenUrl ?? req.body.imagen_url, 500);

    if (!nombre || !puntos || puntos <= 0) {
      return responderError(res, 400, 'Nombre y puntos requeridos válidos');
    }

    const insert = await query(
      `INSERT INTO piku_recompensas
       (comercio_id, nombre, descripcion, puntos_requeridos, icono, stock, imagen_url)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [comercioId, nombre, descripcion, puntos, icono, stock, imagenUrl || null]
    );

    return res.status(201).json({ mensaje: 'Recompensa creada', recompensa: insert.rows[0] });
  } catch (error) {
    console.error('createRecompensa:', error);
    return responderError(res, 500, 'Error al crear recompensa');
  }
}

/**
 * Actualiza una recompensa.
 */
async function updateRecompensa(req, res) {
  try {
    const comercioId = getComercioId(req);
    const { id } = req.params;

    const campos = [];
    const valores = [];
    let idx = 1;

    const mapa = {
      nombre: sanitizarInput(req.body.nombre, 255),
      descripcion: sanitizarInput(req.body.descripcion, 1000),
      icono: sanitizarInput(req.body.icono, 16),
      imagen_url: sanitizarInput(req.body.imagenUrl ?? req.body.imagen_url, 500),
      activo: req.body.activo,
      stock: req.body.stock != null ? parseInt(req.body.stock, 10) : undefined,
      puntos_requeridos:
        req.body.puntosRequeridos != null
          ? parseInt(req.body.puntosRequeridos, 10)
          : req.body.puntos_requeridos != null
            ? parseInt(req.body.puntos_requeridos, 10)
            : undefined,
    };

    for (const [col, val] of Object.entries(mapa)) {
      if (val !== undefined && val !== '') {
        campos.push(`${col} = $${idx++}`);
        valores.push(val);
      }
    }

    if (!campos.length) return responderError(res, 400, 'Sin datos para actualizar');

    campos.push('updated_at = NOW()');
    valores.push(id, comercioId);

    const sql = `UPDATE piku_recompensas SET ${campos.join(', ')}
                 WHERE id = $${idx++} AND comercio_id = $${idx} RETURNING *`;
    const updated = await query(sql, valores);

    if (!updated.rows.length) return responderError(res, 404, 'Recompensa no encontrada');
    return res.json({ mensaje: 'Recompensa actualizada', recompensa: updated.rows[0] });
  } catch (error) {
    console.error('updateRecompensa:', error);
    return responderError(res, 500, 'Error al actualizar recompensa');
  }
}

/**
 * Elimina (desactiva) una recompensa.
 */
async function deleteRecompensa(req, res) {
  try {
    const comercioId = getComercioId(req);
    const { id } = req.params;

    const result = await query(
      `UPDATE piku_recompensas SET activo = FALSE, updated_at = NOW()
       WHERE id = $1 AND comercio_id = $2 RETURNING id`,
      [id, comercioId]
    );
    if (!result.rows.length) return responderError(res, 404, 'Recompensa no encontrada');
    return res.json({ mensaje: 'Recompensa eliminada' });
  } catch (error) {
    console.error('deleteRecompensa:', error);
    return responderError(res, 500, 'Error al eliminar recompensa');
  }
}

/**
 * Genera QR dinámico (alias de qr.controller generarQR).
 */
async function generarQRComercio(req, res) {
  const { generarQR } = require('./qr.controller');
  return generarQR(req, res);
}

/**
 * Estadísticas del dashboard del comercio.
 */
async function getEstadisticas(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const [escaneos, puntos, canjes, clientes] = await Promise.all([
      query(
        `SELECT COUNT(*)::int AS total FROM piku_qr_dinamicos
         WHERE comercio_id = $1 AND usado = TRUE`,
        [comercioId]
      ),
      query(
        `SELECT COALESCE(SUM(puntos), 0)::int AS total FROM piku_transacciones_puntos
         WHERE comercio_id = $1 AND tipo = 'ganado'`,
        [comercioId]
      ),
      query(
        `SELECT COUNT(*)::int AS total FROM piku_canjes c
         INNER JOIN piku_recompensas r ON r.id = c.recompensa_id
         WHERE r.comercio_id = $1`,
        [comercioId]
      ),
      query(
        `SELECT COUNT(DISTINCT usuario_id)::int AS total FROM piku_transacciones_puntos
         WHERE comercio_id = $1`,
        [comercioId]
      ),
    ]);

    const ultimos = await query(
      `SELECT t.puntos, t.tipo, t.descripcion, t.created_at, u.nombre AS cliente
       FROM piku_transacciones_puntos t
       LEFT JOIN piku_usuarios u ON u.id = t.usuario_id
       WHERE t.comercio_id = $1
       ORDER BY t.created_at DESC LIMIT 10`,
      [comercioId]
    );

    return res.json({
      estadisticas: {
        qrUsados: escaneos.rows[0].total,
        puntosOtorgados: puntos.rows[0].total,
        canjesRealizados: canjes.rows[0].total,
        clientesUnicos: clientes.rows[0].total,
      },
      ultimasTransacciones: ultimos.rows,
    });
  } catch (error) {
    console.error('getEstadisticas:', error);
    return responderError(res, 500, 'Error al obtener estadísticas');
  }
}

module.exports = {
  getReglasPuntos,
  updateReglasPuntos,
  getRecompensas,
  createRecompensa,
  updateRecompensa,
  deleteRecompensa,
  generarQR: generarQRComercio,
  getEstadisticas,
};
