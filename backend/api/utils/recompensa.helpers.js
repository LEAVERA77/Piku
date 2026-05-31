const { columnasTabla, tiene } = require('./schema.util');

const TIPOS_VALIDOS = ['descuento', 'producto_gratis', '2x1', 'envio_gratis'];

function parseFecha(val) {
  if (val == null || val === '') return null;
  const d = new Date(val);
  return Number.isNaN(d.getTime()) ? null : d.toISOString();
}

function parseIntOrNull(val) {
  if (val == null || val === '') return null;
  const n = parseInt(val, 10);
  return Number.isNaN(n) ? null : n;
}

function parseFloatOrNull(val) {
  if (val == null || val === '') return null;
  const n = parseFloat(val);
  return Number.isNaN(n) ? null : n;
}

function normalizarTipo(tipo) {
  const t = String(tipo || 'producto_gratis').toLowerCase();
  return TIPOS_VALIDOS.includes(t) ? t : 'producto_gratis';
}

function recompensaVigenteSql(alias = 'r') {
  return `(
    (${alias}.fecha_inicio IS NULL OR ${alias}.fecha_inicio <= NOW())
    AND (${alias}.fecha_fin IS NULL OR ${alias}.fecha_fin >= NOW())
    AND (
      ${alias}.max_usos_totales IS NULL
      OR ${alias}.max_usos_totales = 0
      OR ${alias}.usos_actuales < ${alias}.max_usos_totales
    )
  )`;
}

/**
 * INSERT dinámico compatible con esquemas Neon (p. ej. puntos_necesarios NOT NULL).
 */
async function insertRecompensa(query, comercioId, data) {
  const cols = await columnasTabla('piku_recompensas');
  const puntos = data.puntos;
  const columnas = [
    'comercio_id',
    'nombre',
    'descripcion',
    'puntos_requeridos',
    'icono',
    'stock',
    'imagen_url',
    'tipo',
    'porcentaje_descuento',
    'monto_maximo_descuento',
    'producto_nombre',
    'fecha_inicio',
    'fecha_fin',
    'horarios_validos',
    'max_usos_por_usuario',
    'max_usos_totales',
    'usos_actuales',
    'activo',
  ];
  const valores = [
    comercioId,
    data.nombre,
    data.descripcion,
    puntos,
    data.icono,
    data.stock,
    data.imagenUrl,
    data.tipo,
    data.porcentajeDescuento,
    data.montoMaximoDescuento,
    data.productoNombre,
    data.fechaInicio,
    data.fechaFin,
    data.horariosValidos,
    data.maxUsosPorUsuario,
    data.maxUsosTotales,
    0,
    true,
  ];

  if (tiene(cols, 'puntos_necesarios')) {
    columnas.splice(4, 0, 'puntos_necesarios');
    valores.splice(4, 0, puntos);
  }

  const placeholders = columnas.map((_, i) => `$${i + 1}`).join(',');
  const sql = `INSERT INTO piku_recompensas (${columnas.join(', ')}) VALUES (${placeholders}) RETURNING *`;
  return query(sql, valores);
}

module.exports = {
  TIPOS_VALIDOS,
  parseFecha,
  parseIntOrNull,
  parseFloatOrNull,
  normalizarTipo,
  recompensaVigenteSql,
  insertRecompensa,
};
