const { query } = require('../services/neon.service');
const { responderError } = require('../utils/helpers');
const { calcularDistancia } = require('../services/nominatim.service');
const {
  selectComerciosColumnas,
  selectRecompensasPublicas,
} = require('../utils/comercio.sql.util');
const { columnasTabla } = require('../utils/schema.util');
const { adjuntarImagenesARecompensa } = require('../utils/recompensa.imagenes.util');
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
 * Comercios cercanos a un punto (radio en metros, default 5000).
 */
async function comerciosCercanos(req, res) {
  try {
    const lat = parseFloat(req.query.lat);
    const lon = parseFloat(req.query.lon);
    const radio = parseInt(req.query.radio || '5000', 10) || 5000;
    if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
      return responderError(res, 400, 'Parámetros lat y lon requeridos');
    }

    const columnas = await selectComerciosColumnas();
    const result = await query(
      `SELECT ${columnas}
       FROM piku_comercios c
       WHERE COALESCE(c.suscripcion_activa, TRUE) = TRUE
         AND c.lat IS NOT NULL AND c.lon IS NOT NULL`,
      []
    );

    const comercios = result.rows
      .map((c) => ({
        ...c,
        distancia_metros: calcularDistancia(lat, lon, c.lat, c.lon),
      }))
      .filter((c) => c.distancia_metros <= radio)
      .sort((a, b) => a.distancia_metros - b.distancia_metros);

    return res.json({ comercios });
  } catch (error) {
    console.error('comerciosCercanos:', error);
    return responderError(res, 500, 'Error al buscar comercios cercanos', { detail: error.message });
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

    const recQuery = await selectRecompensasPublicas();
    let recompensas = { rows: [] };
    if (recQuery.sql) {
      recompensas = await query(recQuery.sql, [id]);
    }

    const recompensasConImagenes = await Promise.all(
      recompensas.rows.map((r) => adjuntarImagenesARecompensa(r))
    );

    return res.json({
      comercio: result.rows[0],
      recompensas: recompensasConImagenes,
    });
  } catch (error) {
    console.error('detalleComercio:', error);
    return responderError(res, 500, 'Error al obtener comercio', { detail: error.message });
  }
}

/**
 * Ofertas activas de un comercio.
 */
async function ofertasComercio(req, res) {
  try {
    const { id } = req.params;
    const recQuery = await selectRecompensasPublicas();
    if (!recQuery.sql) return res.json({ ofertas: [] });
    const recompensas = await query(recQuery.sql, [id]);
    const ofertas = await Promise.all(recompensas.rows.map((r) => adjuntarImagenesARecompensa(r)));
    return res.json({ ofertas });
  } catch (error) {
    console.error('ofertasComercio:', error);
    return responderError(res, 500, 'Error al listar ofertas', { detail: error.message });
  }
}

/**
 * Detalle público de una recompensa/oferta.
 */
async function detalleRecompensa(req, res) {
  try {
    const { id } = req.params;
    const cols = await columnasTabla('piku_recompensas');
    if (!cols.size) return responderError(res, 404, 'Oferta no encontrada');

    const meta = await query('SELECT comercio_id FROM piku_recompensas WHERE id = $1', [id]);
    if (!meta.rows.length) return responderError(res, 404, 'Oferta no encontrada');

    const recQuery = await selectRecompensasPublicas();
    if (!recQuery.sql) return responderError(res, 404, 'Oferta no encontrada');

    const lista = await query(recQuery.sql, [meta.rows[0].comercio_id]);
    const recompensa = lista.rows.find((r) => String(r.id) === String(id));
    if (!recompensa) return responderError(res, 404, 'Oferta no encontrada o no vigente');

    const columnas = await selectComerciosColumnas();
    const cRes = await query(
      `SELECT ${columnas} FROM piku_comercios c WHERE c.id = $1`,
      [meta.rows[0].comercio_id]
    );

    const recompensaConImagenes = await adjuntarImagenesARecompensa(recompensa);

    return res.json({
      recompensa: recompensaConImagenes,
      comercio: cRes.rows[0] || null,
    });
  } catch (error) {
    console.error('detalleRecompensa:', error);
    return responderError(res, 500, 'Error al obtener oferta', { detail: error.message });
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

module.exports = {
  listarComercios,
  comerciosCercanos,
  detalleComercio,
  ofertasComercio,
  detalleRecompensa,
  listarRecompensasPublicas,
  listarRubros,
};
