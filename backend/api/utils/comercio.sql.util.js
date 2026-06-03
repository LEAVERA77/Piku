const { columnasTabla, tiene } = require('./schema.util');
const { recompensaVigenteSql } = require('./recompensa.helpers');

let cacheSelect = null;

function columnaPuntosRecompensa(colsRecomp) {
  if (tiene(colsRecomp, 'puntos_requeridos')) return 'puntos_requeridos';
  if (tiene(colsRecomp, 'puntos_necesarios')) return 'puntos_necesarios';
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
  const tipoComercioSql = tiene(colsComercio, 'tipo_comercio')
    ? 'c.tipo_comercio'
    : 'NULL::varchar AS tipo_comercio';
  const iconoEmojiSql = tiene(colsComercio, 'icono_emoji')
    ? 'c.icono_emoji'
    : 'NULL::varchar AS icono_emoji';

  const colPuntos = columnaPuntosRecompensa(colsRecomp);
  let puntosMin = 'NULL::int AS puntos_min_canje';
  let cantidadOfertas = '0::int AS cantidad_ofertas';
  let ofertasNuevas = '0::int AS ofertas_nuevas';
  if (colsRecomp.size > 0 && colPuntos) {
    const filtro = filtrosRecompensaVigente(colsRecomp);
    puntosMin = `(SELECT MIN(r.${colPuntos})
      FROM piku_recompensas r
      WHERE ${filtro}) AS puntos_min_canje`;
    cantidadOfertas = `(SELECT COUNT(*)::int
      FROM piku_recompensas r
      WHERE ${filtro}) AS cantidad_ofertas`;
    if (tiene(colsRecomp, 'created_at')) {
      ofertasNuevas = `(SELECT COUNT(*)::int
        FROM piku_recompensas r
        WHERE ${filtro} AND r.created_at > NOW() - INTERVAL '14 days') AS ofertas_nuevas`;
    }
  } else if (colsRecomp.size > 0) {
    const filtro = filtrosRecompensaVigente(colsRecomp);
    cantidadOfertas = `(SELECT COUNT(*)::int
      FROM piku_recompensas r
      WHERE ${filtro}) AS cantidad_ofertas`;
    if (tiene(colsRecomp, 'created_at')) {
      ofertasNuevas = `(SELECT COUNT(*)::int
        FROM piku_recompensas r
        WHERE ${filtro} AND r.created_at > NOW() - INTERVAL '14 days') AS ofertas_nuevas`;
    }
  }

  const suscripcion = tiene(colsComercio, 'suscripcion_activa')
    ? 'c.suscripcion_activa'
    : 'TRUE AS suscripcion_activa';

  const envioPartes = [];
  if (tiene(colsComercio, 'realiza_envios')) {
    envioPartes.push('COALESCE(c.realiza_envios, FALSE) AS realiza_envios');
  } else {
    envioPartes.push('FALSE AS realiza_envios');
  }
  if (tiene(colsComercio, 'envio_gratis')) {
    envioPartes.push('COALESCE(c.envio_gratis, FALSE) AS envio_gratis');
  } else {
    envioPartes.push('FALSE AS envio_gratis');
  }
  if (tiene(colsComercio, 'costo_envio')) {
    envioPartes.push('c.costo_envio');
  } else {
    envioPartes.push('NULL::numeric AS costo_envio');
  }
  if (tiene(colsComercio, 'envio_minimo_compra')) {
    envioPartes.push('c.envio_minimo_compra');
  } else {
    envioPartes.push('NULL::numeric AS envio_minimo_compra');
  }
  if (tiene(colsComercio, 'telefono_contacto')) {
    envioPartes.push('c.telefono_contacto');
  } else {
    envioPartes.push('NULL::varchar AS telefono_contacto');
  }

  cacheSelect = `
    c.id, c.usuario_id, c.nombre, c.direccion, c.lat, c.lon, c.logo_url,
    ${suscripcion}, ${categoriaSql}, ${tipoComercioSql}, ${iconoEmojiSql},
    ${envioPartes.join(', ')},
    c.created_at, ${puntosMin}, ${cantidadOfertas}, ${ofertasNuevas}
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
  const vigenciaDesdeSql = tiene(cols, 'vigencia_desde')
    ? 'r.vigencia_desde AS vigencia_desde'
    : tiene(cols, 'fecha_inicio')
      ? 'r.fecha_inicio AS vigencia_desde'
      : 'NULL::timestamptz AS vigencia_desde';
  const vigenciaHastaSql = tiene(cols, 'vigencia_hasta')
    ? 'r.vigencia_hasta AS vigencia_hasta'
    : tiene(cols, 'fecha_fin')
      ? 'r.fecha_fin AS vigencia_hasta'
      : 'NULL::timestamptz AS vigencia_hasta';
  const campos = [
    'r.id',
    'r.nombre',
    tiene(cols, 'descripcion') ? 'r.descripcion' : 'NULL::text AS descripcion',
    puntosSql,
    tiene(cols, 'icono') ? 'r.icono' : "NULL::varchar AS icono",
    tiene(cols, 'imagen_url') ? 'r.imagen_url' : 'NULL::text AS imagen_url',
    tiene(cols, 'tipo') ? 'r.tipo' : "NULL::varchar AS tipo",
    tiene(cols, 'porcentaje_descuento')
      ? 'r.porcentaje_descuento'
      : 'NULL::int AS porcentaje_descuento',
    tiene(cols, 'producto_nombre') ? 'r.producto_nombre' : 'NULL::varchar AS producto_nombre',
    tiene(cols, 'condiciones') ? 'r.condiciones' : 'NULL::text AS condiciones',
    vigenciaDesdeSql,
    vigenciaHastaSql,
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
