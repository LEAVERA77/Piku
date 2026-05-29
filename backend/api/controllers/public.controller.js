const { query } = require('../services/neon.service');
const { responderError } = require('../utils/helpers');

const COLUMNAS_COMERCIO = `
  id, usuario_id, nombre, direccion, lat, lon, logo_url, suscripcion_activa, created_at
`.trim();

/**
 * Lista comercios activos (público).
 */
async function listarComercios(req, res) {
  try {
    const result = await query(
      `SELECT ${COLUMNAS_COMERCIO}
       FROM piku_comercios WHERE suscripcion_activa = TRUE ORDER BY nombre ASC`
    );
    return res.json({ comercios: result.rows });
  } catch (error) {
    console.error('listarComercios:', error);
    return responderError(res, 500, 'Error al listar comercios');
  }
}

/**
 * Detalle de un comercio (público).
 */
async function detalleComercio(req, res) {
  try {
    const { id } = req.params;
    const result = await query(
      `SELECT ${COLUMNAS_COMERCIO}
       FROM piku_comercios WHERE id = $1 AND suscripcion_activa = TRUE`,
      [id]
    );
    if (!result.rows.length) return responderError(res, 404, 'Comercio no encontrado');

    const recompensas = await query(
      `SELECT id, nombre, descripcion, puntos_requeridos, icono, imagen_url
       FROM piku_recompensas WHERE comercio_id = $1 AND activo = TRUE
         AND (stock IS NULL OR stock > 0)
       ORDER BY puntos_requeridos`,
      [id]
    );

    return res.json({
      comercio: result.rows[0],
      recompensas: recompensas.rows,
    });
  } catch (error) {
    console.error('detalleComercio:', error);
    return responderError(res, 500, 'Error al obtener comercio');
  }
}

/**
 * Recompensas activas de todos los comercios (público).
 */
async function listarRecompensasPublicas(req, res) {
  try {
    const comercioId = req.query.comercioId || req.query.comercio_id;
    let sql = `
      SELECT r.id, r.nombre, r.descripcion, r.puntos_requeridos, r.icono, r.imagen_url,
             c.id AS comercio_id, c.nombre AS comercio_nombre
      FROM piku_recompensas r
      INNER JOIN piku_comercios c ON c.id = r.comercio_id AND c.suscripcion_activa = TRUE
      WHERE r.activo = TRUE AND (r.stock IS NULL OR r.stock > 0)`;
    const params = [];

    if (comercioId) {
      params.push(comercioId);
      sql += ` AND r.comercio_id = $1`;
    }
    sql += ' ORDER BY r.puntos_requeridos ASC';

    const result = await query(sql, params);
    return res.json({ recompensas: result.rows });
  } catch (error) {
    console.error('listarRecompensasPublicas:', error);
    return responderError(res, 500, 'Error al listar recompensas');
  }
}

module.exports = { listarComercios, detalleComercio, listarRecompensasPublicas };
