const fs = require('fs');
const path = require('path');
const { query, pool } = require('./neon.service');

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

  for (const file of MIGRATION_FILES) {
    const filePath = path.join(sqlDir, file);
    if (!fs.existsSync(filePath)) continue;

    const raw = fs.readFileSync(filePath, 'utf8');
    const statements = raw
      .split(';')
      .map((s) => s.replace(/--[^\n]*/g, '').trim())
      .filter((s) => s.length > 0);

    for (const statement of statements) {
      try {
        await query(statement);
      } catch (error) {
        // Ignorar errores benignos (tabla/columna ya existe con otro tipo, etc.)
        const benign =
          /already exists/i.test(error.message) ||
          /duplicate column/i.test(error.message);
        if (!benign) {
          console.warn(`⚠️ Migración ${file}:`, error.message);
        }
      }
    }
  }

  console.log('✅ Migraciones SQL verificadas');
}

module.exports = { runStartupMigrations };
