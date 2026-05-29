-- Esquema Piku - tablas con prefijo piku_ (compartible con GestorNova en la misma BD)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS piku_comercios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    direccion TEXT,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    radio_metros INTEGER NOT NULL DEFAULT 100,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    logo_url TEXT,
    usuario_id UUID,
    suscripcion_activa BOOLEAN NOT NULL DEFAULT TRUE,
    categoria VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS piku_usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    telefono VARCHAR(50),
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('cliente', 'comercio', 'admin')),
    avatar_url TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    puntos_saldo INTEGER NOT NULL DEFAULT 0 CHECK (puntos_saldo >= 0),
    comercio_id UUID REFERENCES piku_comercios(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE piku_comercios
    ADD CONSTRAINT fk_piku_comercios_usuario
    FOREIGN KEY (usuario_id) REFERENCES piku_usuarios(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS piku_invitaciones_comercio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(64) NOT NULL UNIQUE,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    comercio_id UUID REFERENCES piku_comercios(id) ON DELETE SET NULL,
    creado_por UUID REFERENCES piku_usuarios(id) ON DELETE SET NULL,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS piku_reglas_puntos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comercio_id UUID NOT NULL UNIQUE REFERENCES piku_comercios(id) ON DELETE CASCADE,
    puntos_por_peso NUMERIC(10, 2) NOT NULL DEFAULT 1,
    monto_minimo NUMERIC(10, 2) NOT NULL DEFAULT 0,
    puntos_fijos INTEGER NOT NULL DEFAULT 0,
    max_puntos_por_dia INTEGER NOT NULL DEFAULT 500,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS piku_recompensas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comercio_id UUID NOT NULL REFERENCES piku_comercios(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    puntos_requeridos INTEGER NOT NULL CHECK (puntos_requeridos > 0),
    icono VARCHAR(16) DEFAULT '🎁',
    imagen_url TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    stock INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS piku_qr_dinamicos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comercio_id UUID NOT NULL REFERENCES piku_comercios(id) ON DELETE CASCADE,
    codigo VARCHAR(128) NOT NULL UNIQUE,
    monto_transaccion NUMERIC(10, 2) NOT NULL DEFAULT 0,
    puntos_calculados INTEGER,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    usado_por UUID REFERENCES piku_usuarios(id) ON DELETE SET NULL,
    usado_at TIMESTAMPTZ,
    expira_at TIMESTAMPTZ NOT NULL,
    lat_generacion DOUBLE PRECISION,
    lon_generacion DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS piku_transacciones_puntos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES piku_usuarios(id) ON DELETE CASCADE,
    comercio_id UUID REFERENCES piku_comercios(id) ON DELETE SET NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('ganado', 'canjeado')),
    puntos INTEGER NOT NULL,
    descripcion TEXT,
    qr_codigo_id UUID REFERENCES piku_qr_dinamicos(id) ON DELETE SET NULL,
    recompensa_id UUID REFERENCES piku_recompensas(id) ON DELETE SET NULL,
    codigo_canje VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS piku_canjes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES piku_usuarios(id) ON DELETE CASCADE,
    recompensa_id UUID NOT NULL REFERENCES piku_recompensas(id) ON DELETE CASCADE,
    puntos_usados INTEGER NOT NULL,
    codigo_canje VARCHAR(64) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'confirmado',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_piku_usuarios_rol ON piku_usuarios(rol);
CREATE INDEX IF NOT EXISTS idx_piku_usuarios_comercio ON piku_usuarios(comercio_id);
CREATE INDEX IF NOT EXISTS idx_piku_transacciones_usuario ON piku_transacciones_puntos(usuario_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_piku_qr_dinamicos_codigo ON piku_qr_dinamicos(codigo);
CREATE INDEX IF NOT EXISTS idx_piku_recompensas_comercio ON piku_recompensas(comercio_id, activo);
CREATE INDEX IF NOT EXISTS idx_piku_comercios_activo ON piku_comercios(activo);
