const fs = require('fs');
const path = require('path');
const { pool } = require('./neon.service');

const MIGRATION_FILES = [
  'migration_neon_production_sync.sql',
  'migration_google_auth.sql',
  'migration_recompensas_extend.sql',
  'migration_eventos_usuario.sql',
];

/** Columnas críticas que la API necesita (por si falla el SQL completo). */
const CRITICAL_ALTERS = [
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS puntos_saldo INTEGER NOT NULL DEFAULT 0',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS avatar_url TEXT',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS google_id VARCHAR(255)',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS comercio_id UUID',
  'ALTER TABLE piku_usuarios ALTER COLUMN password_hash DROP NOT NULL',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS usuario_id UUID',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS suscripcion_activa BOOLEAN NOT NULL DEFAULT TRUE',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS categoria VARCHAR(50)',
  `CREATE TABLE IF NOT EXISTS piku_eventos_usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES piku_usuarios(id) ON DELETE CASCADE,
    tipo_evento VARCHAR(40) NOT NULL,
    comercio_id UUID REFERENCES piku_comercios(id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
  )`,
  'CREATE INDEX IF NOT EXISTS idx_piku_eventos_usuario_user ON piku_eventos_usuario(usuario_id, created_at DESC)',
  'ALTER TABLE piku_invitaciones_comercio ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ',
  'ALTER TABLE piku_invitaciones_comercio ADD COLUMN IF NOT EXISTS usado BOOLEAN NOT NULL DEFAULT FALSE',
];

function splitSqlStatements(sql) {
  return sql
    .replace(/--[^\n]*/g, '')
    .split(';')
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}

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
    for (const stmt of CRITICAL_ALTERS) {
      try {
        await client.query(stmt);
      } catch (error) {
        console.error('❌ Alter crítico:', stmt.slice(0, 60), '→', error.message);
      }
    }

    for (const file of MIGRATION_FILES) {
      const filePath = path.join(sqlDir, file);
      if (!fs.existsSync(filePath)) continue;

      const statements = splitSqlStatements(fs.readFileSync(filePath, 'utf8'));
      for (const statement of statements) {
        try {
          await client.query(statement);
        } catch (error) {
          console.error(`❌ ${file}:`, error.message);
        }
      }
      console.log(`✅ Migración procesada: ${file}`);
    }
  } finally {
    client.release();
  }

  const { refrescarCache } = require('../utils/schema.util');
  await refrescarCache();
}

module.exports = { runStartupMigrations };
