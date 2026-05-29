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

module.exports = {
  TIPOS_VALIDOS,
  parseFecha,
  parseIntOrNull,
  parseFloatOrNull,
  normalizarTipo,
  recompensaVigenteSql,
};
