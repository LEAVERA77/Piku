const fs = require('fs');
const path = require('path');
const { pool } = require('./neon.service');

const MIGRATION_FILES = [
  'migration_neon_production_sync.sql',
  'migration_google_auth.sql',
  'migration_recompensas_extend.sql',
  'migration_eventos_usuario.sql',
  'migration_usuario_envio.sql',
  'migration_comercio_osm_note.sql',
  'migration_registro_mapa_ofertas.sql',
  'migration_comercio_envios.sql',
  'migration_radio_metros.sql',
  'migration_reglas_puntos_extend.sql',
  'migration_recompensa_imagenes.sql',
  'migration_notificaciones.sql',
  'migration_fcm_token.sql',
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
  'ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS puntos_requeridos INTEGER DEFAULT 100',
  'ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE',
  'ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS stock INTEGER',
  'ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS icono VARCHAR(16) DEFAULT \'🎁\'',
  'ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS imagen_url TEXT',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS direccion_entrega TEXT',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS ciudad VARCHAR(100)',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS provincia VARCHAR(100)',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS codigo_postal VARCHAR(20)',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS notas_entrega TEXT',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS osm_note_id BIGINT',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS osm_note_created_at TIMESTAMPTZ',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS lat DOUBLE PRECISION',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS lon DOUBLE PRECISION',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS direccion TEXT',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS tipo_comercio VARCHAR(50)',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS icono_emoji VARCHAR(10)',
  'ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS condiciones TEXT',
  'ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS vigencia_desde TIMESTAMPTZ',
  'ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS vigencia_hasta TIMESTAMPTZ',
  'CREATE INDEX IF NOT EXISTS idx_piku_comercios_coords ON piku_comercios(lat, lon)',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS realiza_envios BOOLEAN DEFAULT false',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS envio_gratis BOOLEAN DEFAULT false',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS costo_envio DECIMAL(10,2) DEFAULT 0',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS envio_minimo_compra DECIMAL(10,2)',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS telefono_contacto VARCHAR(20)',
  'ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS radio_metros INTEGER NOT NULL DEFAULT 100',
  'ALTER TABLE piku_reglas_puntos ADD COLUMN IF NOT EXISTS puntos_por_peso NUMERIC(10, 2) NOT NULL DEFAULT 1',
  'ALTER TABLE piku_reglas_puntos ADD COLUMN IF NOT EXISTS monto_minimo NUMERIC(10, 2) NOT NULL DEFAULT 0',
  'ALTER TABLE piku_reglas_puntos ADD COLUMN IF NOT EXISTS puntos_fijos INTEGER NOT NULL DEFAULT 0',
  'ALTER TABLE piku_reglas_puntos ADD COLUMN IF NOT EXISTS max_puntos_por_dia INTEGER NOT NULL DEFAULT 500',
  'ALTER TABLE piku_reglas_puntos ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()',
  `CREATE TABLE IF NOT EXISTS piku_recompensa_imagenes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recompensa_id UUID NOT NULL REFERENCES piku_recompensas(id) ON DELETE CASCADE,
    imagen_url TEXT NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
  )`,
  'CREATE INDEX IF NOT EXISTS idx_piku_recompensa_imagenes_rec ON piku_recompensa_imagenes(recompensa_id, orden)',
  'CREATE INDEX IF NOT EXISTS idx_piku_comercios_envios ON piku_comercios(realiza_envios)',
  'ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS fcm_token TEXT',
];

/**
 * Divide SQL en sentencias por ';' sin cortar bloques DO $$ ... $$ ni comentarios --.
 */
function splitSqlStatements(sql) {
  const statements = [];
  let current = '';
  let i = 0;
  let dollarDelimiter = null;

  while (i < sql.length) {
    if (dollarDelimiter !== null) {
      if (sql.startsWith(dollarDelimiter, i)) {
        current += dollarDelimiter;
        i += dollarDelimiter.length;
        dollarDelimiter = null;
        continue;
      }
      current += sql[i];
      i += 1;
      continue;
    }

    if (sql[i] === '$') {
      const match = sql.slice(i).match(/^\$([A-Za-z0-9_]*)\$/);
      if (match) {
        dollarDelimiter = match[0];
        current += dollarDelimiter;
        i += dollarDelimiter.length;
        continue;
      }
    }

    if (sql[i] === '-' && sql[i + 1] === '-') {
      while (i < sql.length && sql[i] !== '\n') i += 1;
      continue;
    }

    if (sql[i] === ';') {
      const trimmed = current.trim();
      if (trimmed.length > 0) statements.push(trimmed);
      current = '';
      i += 1;
      continue;
    }

    current += sql[i];
    i += 1;
  }

  const trimmed = current.trim();
  if (trimmed.length > 0) statements.push(trimmed);
  return statements;
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
