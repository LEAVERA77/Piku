const fs = require('fs');
const path = require('path');
const { pool } = require('./neon.service');

const MIGRATION_FILES = [
  'migration_neon_production_sync.sql',
  'migration_google_auth.sql',
  'migration_recompensas_extend.sql',
];

/**
 * Aplica migraciones SQL idempotentes al arrancar (Neon/producción desactualizada).
 */
async function runStartupMigrations() {
  if (!pool) {
    console.warn('⚠️ Sin DATABASE_URL: migraciones omitidas');
    return;
  }

  const sqlDir = path.join(__dirname, '..', 'sql');
  const client = await pool.connect();

  try {
    for (const file of MIGRATION_FILES) {
      const filePath = path.join(sqlDir, file);
      if (!fs.existsSync(filePath)) continue;

      const sql = fs.readFileSync(filePath, 'utf8');
      try {
        await client.query(sql);
        console.log(`✅ Migración aplicada: ${file}`);
      } catch (error) {
        console.error(`❌ Migración ${file}:`, error.message);
      }
    }
  } finally {
    client.release();
  }
}

module.exports = { runStartupMigrations };
