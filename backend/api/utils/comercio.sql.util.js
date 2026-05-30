const { columnasTabla, tiene } = require('./schema.util');
const { recompensaVigenteSql } = require('./recompensa.helpers');

let cacheSelect = null;

function columnaPuntosRecompensa(colsRecomp) {
  if (tiene(colsRecomp, 'puntos_requeridos')) return 'puntos_requeridos';
  if (tiene(colsRecomp, 'puntos')) return 'puntos';
  if (tiene(colsRecomp, 'costo_puntos')) return 'costo_puntos';
  return null;
}

function filtrosRecompensaVigente(colsRecomp) {
  let filtro = 'r.comercio_id = c.id';
  if (tiene(colsRecomp, 'activo')) {
    filtro += ' AND r.activo = TRUE';
  }
  if (tiene(colsRecomp, 'stock')) {
    filtro += ' AND (r.stock IS NULL OR r.stock > 0)';
  }
  if (tiene(colsRecomp, 'fecha_inicio') && tiene(colsRecomp, 'fecha_fin')) {
    filtro += ` AND ${recompensaVigenteSql('r')}`;
  } else if (tiene(colsRecomp, 'max_usos_totales') && tiene(colsRecomp, 'usos_actuales')) {
    filtro += ` AND (
      r.max_usos_totales IS NULL OR r.max_usos_totales = 0
      OR r.usos_actuales < r.max_usos_totales
    )`;
  }
  return filtro;
}

/**
 * SELECT de comercios compatible con Neon desactualizado.
 */
async function selectComerciosColumnas() {
  if (cacheSelect) return cacheSelect;

  const colsComercio = await columnasTabla('piku_comercios');
  const colsRecomp = await columnasTabla('piku_recompensas');

  const categoriaSql = tiene(colsComercio, 'categoria')
    ? 'c.categoria'
    : 'NULL::varchar AS categoria';

  const colPuntos = columnaPuntosRecompensa(colsRecomp);
  let puntosMin = 'NULL::int AS puntos_min_canje';
  if (colsRecomp.size > 0 && colPuntos) {
    const filtro = filtrosRecompensaVigente(colsRecomp);
    puntosMin = `(SELECT MIN(r.${colPuntos})
      FROM piku_recompensas r
      WHERE ${filtro}) AS puntos_min_canje`;
  }

  const suscripcion = tiene(colsComercio, 'suscripcion_activa')
    ? 'c.suscripcion_activa'
    : 'TRUE AS suscripcion_activa';

  cacheSelect = `
    c.id, c.usuario_id, c.nombre, c.direccion, c.lat, c.lon, c.logo_url,
    ${suscripcion}, ${categoriaSql}, c.created_at, ${puntosMin}
  `.trim();

  return cacheSelect;
}

/** Columnas para listar recompensas en detalle público. */
async function selectRecompensasPublicas() {
  const cols = await columnasTabla('piku_recompensas');
  if (!cols.size) {
    return { sql: null, orderBy: 'nombre' };
  }
  const colPuntos = columnaPuntosRecompensa(cols);
  const puntosSql = colPuntos ? `r.${colPuntos} AS puntos_requeridos` : 'NULL::int AS puntos_requeridos';
  const campos = [
    'r.id',
    'r.nombre',
    tiene(cols, 'descripcion') ? 'r.descripcion' : 'NULL::text AS descripcion',
    puntosSql,
    tiene(cols, 'icono') ? 'r.icono' : "NULL::varchar AS icono",
    tiene(cols, 'imagen_url') ? 'r.imagen_url' : 'NULL::text AS imagen_url',
  ];
  let where = 'r.comercio_id = $1';
  if (tiene(cols, 'activo')) where += ' AND r.activo = TRUE';
  if (tiene(cols, 'stock')) where += ' AND (r.stock IS NULL OR r.stock > 0)';
  const orderBy = colPuntos ? colPuntos : 'r.nombre';
  return { sql: `SELECT ${campos.join(', ')} FROM piku_recompensas r WHERE ${where} ORDER BY ${orderBy}`, orderBy };
}

function invalidarCacheComerciosSelect() {
  cacheSelect = null;
}

module.exports = {
  selectComerciosColumnas,
  selectRecompensasPublicas,
  invalidarCacheComerciosSelect,
  columnaPuntosRecompensa,
};
