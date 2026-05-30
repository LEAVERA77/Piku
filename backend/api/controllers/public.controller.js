const { query } = require('../services/neon.service');
const { responderError } = require('../utils/helpers');
const { selectComerciosColumnas } = require('../utils/comercio.sql.util');
const { RUBROS } = require('../constants/rubros');

function parseBbox(req) {
  const minLat = parseFloat(req.query.minLat ?? req.query.min_lat);
  const maxLat = parseFloat(req.query.maxLat ?? req.query.max_lat);
  const minLon = parseFloat(req.query.minLon ?? req.query.min_lon);
  const maxLon = parseFloat(req.query.maxLon ?? req.query.max_lon);
  if (
    [minLat, maxLat, minLon, maxLon].every((n) => Number.isFinite(n)) &&
    minLat <= maxLat &&
    minLon <= maxLon
  ) {
    return { minLat, maxLat, minLon, maxLon };
  }
  return null;
}

/**
 * Lista comercios activos (público). Opcional: bbox (viewport) para carga rápida.
 */
async function listarComercios(req, res) {
  try {
    const columnas = await selectComerciosColumnas();
    const bbox = parseBbox(req);
    const params = [];
    let where = 'WHERE COALESCE(c.suscripcion_activa, TRUE) = TRUE';

    if (bbox) {
      params.push(bbox.minLat, bbox.maxLat, bbox.minLon, bbox.maxLon);
      where += ` AND c.lat IS NOT NULL AND c.lon IS NOT NULL
        AND c.lat BETWEEN $1 AND $2 AND c.lon BETWEEN $3 AND $4`;
    }

    const result = await query(
      `SELECT ${columnas}
       FROM piku_comercios c
       ${where}
       ORDER BY c.nombre ASC
       LIMIT 200`,
      params
    );
    return res.json({ comercios: result.rows });
  } catch (error) {
    console.error('listarComercios:', error);
    return responderError(res, 500, 'Error al listar comercios', { detail: error.message });
  }
}

/**
 * Catálogo de rubros para filtros del mapa.
 */
async function listarRubros(req, res) {
  return res.json({
    rubros: RUBROS.map(({ id, label, categorias }) => ({ id, label, categorias })),
  });
}

/**
 * Detalle de un comercio (público).
 */
async function detalleComercio(req, res) {
  try {
    const { id } = req.params;
    const columnas = await selectComerciosColumnas();
    const result = await query(
      `SELECT ${columnas}
       FROM piku_comercios c
       WHERE c.id = $1 AND COALESCE(c.suscripcion_activa, TRUE) = TRUE`,
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
    return responderError(res, 500, 'Error al obtener comercio', { detail: error.message });
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
      INNER JOIN piku_comercios c ON c.id = r.comercio_id AND COALESCE(c.suscripcion_activa, TRUE) = TRUE
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

module.exports = { listarComercios, detalleComercio, listarRecompensasPublicas, listarRubros };
