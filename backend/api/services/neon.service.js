const { Pool } = require('pg');

const connectionString = process.env.DATABASE_URL;

if (!connectionString) {
  console.warn('⚠️ DATABASE_URL no configurada. Las rutas que usan BD fallarán.');
}

const pool = connectionString
  ? new Pool({
      connectionString,
      ssl: connectionString.includes('neon.tech')
        ? { rejectUnauthorized: false }
        : false,
    })
  : null;

/**
 * Ejecuta una consulta SQL parametrizada.
 */
async function query(text, params = []) {
  if (!pool) {
    throw new Error('Base de datos no configurada (DATABASE_URL)');
  }
  return pool.query(text, params);
}

/**
 * Ejecuta una función dentro de una transacción.
 */
async function withTransaction(fn) {
  if (!pool) {
    throw new Error('Base de datos no configurada (DATABASE_URL)');
  }
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await fn(client);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

module.exports = { pool, query, withTransaction };
