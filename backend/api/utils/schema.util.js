const { query, pool } = require('../services/neon.service');

const cache = {
  piku_usuarios: null,
  piku_comercios: null,
  piku_invitaciones_comercio: null,
  piku_recompensas: null,
  piku_eventos_usuario: null,
};

async function columnasTabla(tableName) {
  if (!pool) return new Set();
  if (cache[tableName]) return cache[tableName];

  const result = await query(
    `SELECT column_name FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = $1`,
    [tableName]
  );
  cache[tableName] = new Set(result.rows.map((r) => r.column_name));
  return cache[tableName];
}

function tiene(set, col) {
  return set && set.has(col);
}

async function refrescarCache() {
  Object.keys(cache).forEach((k) => {
    cache[k] = null;
  });
  const { invalidarCacheComerciosSelect } = require('./comercio.sql.util');
  invalidarCacheComerciosSelect();
}

module.exports = { columnasTabla, tiene, refrescarCache };
