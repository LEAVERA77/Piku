-- Desafíos semanales para clientes Piku
CREATE TABLE IF NOT EXISTS piku_desafios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo VARCHAR(100) NOT NULL,
    descripcion TEXT,
    tipo VARCHAR(30) NOT NULL,
    objetivo INT NOT NULL,
    recompensa INT NOT NULL,
    activo BOOLEAN DEFAULT true,
    vigencia_desde DATE,
    vigencia_hasta DATE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS piku_desafios_completados (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES piku_usuarios(id) ON DELETE CASCADE,
    desafio_id UUID NOT NULL REFERENCES piku_desafios(id) ON DELETE CASCADE,
    fecha_completado TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_piku_desafios_activo ON piku_desafios(activo, vigencia_desde, vigencia_hasta);
CREATE INDEX IF NOT EXISTS idx_piku_desafios_completados_user ON piku_desafios_completados(usuario_id, desafio_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_piku_desafios_titulo ON piku_desafios(titulo);

-- Desafíos iniciales (semana actual al migrar)
INSERT INTO piku_desafios (titulo, descripcion, tipo, objetivo, recompensa, activo, vigencia_desde, vigencia_hasta)
SELECT v.titulo, v.descripcion, v.tipo, v.objetivo, v.recompensa, true,
       date_trunc('week', CURRENT_DATE)::date,
       (date_trunc('week', CURRENT_DATE) + interval '6 days')::date
FROM (VALUES
    ('Principiante', 'Escaneá QR en 3 comercios distintos esta semana', 'escaneos_totales', 3, 10),
    ('Explorador', 'Visitá comercios de 2 rubros diferentes', 'escaneos_rubros', 2, 15),
    ('Fidelidad', 'Escaneá 2 veces en el mismo comercio', 'escaneos_mismo_comercio', 2, 20),
    ('Racha', 'Sumá puntos 5 días seguidos', 'dias_consecutivos', 5, 25)
) AS v(titulo, descripcion, tipo, objetivo, recompensa)
WHERE NOT EXISTS (SELECT 1 FROM piku_desafios LIMIT 1);
