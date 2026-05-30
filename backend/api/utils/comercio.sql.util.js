const { columnasTabla, tiene } = require('./schema.util');
const { recompensaVigenteSql } = require('./recompensa.helpers');

let cacheSelect = null;

/**
 * SELECT de comercios compatible con Neon desactualizado (sin fecha_inicio, categoria, etc.).
 */
async function selectComerciosColumnas() {
  if (cacheSelect) return cacheSelect;

  const colsComercio = await columnasTabla('piku_comercios');
  const colsRecomp = await columnasTabla('piku_recompensas');

  const categoriaSql = tiene(colsComercio, 'categoria')
    ? 'c.categoria'
    : 'NULL::varchar AS categoria';

  let filtroRecomp = 'r.comercio_id = c.id AND r.activo = TRUE AND (r.stock IS NULL OR r.stock > 0)';
  if (tiene(colsRecomp, 'fecha_inicio') && tiene(colsRecomp, 'fecha_fin')) {
    filtroRecomp += ` AND ${recompensaVigenteSql('r')}`;
  } else if (tiene(colsRecomp, 'max_usos_totales') && tiene(colsRecomp, 'usos_actuales')) {
    filtroRecomp += ` AND (
      r.max_usos_totales IS NULL OR r.max_usos_totales = 0
      OR r.usos_actuales < r.max_usos_totales
    )`;
  }

  const puntosMin = `(SELECT MIN(r.puntos_requeridos)
    FROM piku_recompensas r
    WHERE ${filtroRecomp}) AS puntos_min_canje`;

  const suscripcion = tiene(colsComercio, 'suscripcion_activa')
    ? 'c.suscripcion_activa'
    : 'TRUE AS suscripcion_activa';

  cacheSelect = `
    c.id, c.usuario_id, c.nombre, c.direccion, c.lat, c.lon, c.logo_url,
    ${suscripcion}, ${categoriaSql}, c.created_at, ${puntosMin}
  `.trim();

  return cacheSelect;
}

function invalidarCacheComerciosSelect() {
  cacheSelect = null;
}

module.exports = { selectComerciosColumnas, invalidarCacheComerciosSelect };
