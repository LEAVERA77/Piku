const { query } = require('./neon.service');

const TIPOS_VALIDOS = new Set([
  'vista_comercio',
  'canje',
  'busqueda',
  'mapa_abierto',
  'chat_piku',
]);

async function registrarEvento(usuarioId, tipoEvento, comercioId = null, metadata = null) {
  if (!TIPOS_VALIDOS.has(tipoEvento)) {
    throw new Error('tipo_evento inválido');
  }
  await query(
    `INSERT INTO piku_eventos_usuario (usuario_id, tipo_evento, comercio_id, metadata)
     VALUES ($1, $2, $3, $4)`,
    [usuarioId, tipoEvento, comercioId || null, metadata ? JSON.stringify(metadata) : null]
  );
}

async function resumenParaIA(usuarioId) {
  const canjes = await query(
    `SELECT cj.created_at, cj.puntos_usados, c.nombre AS comercio_nombre, c.categoria AS rubro
     FROM piku_canjes cj
     INNER JOIN piku_recompensas r ON r.id = cj.recompensa_id
     INNER JOIN piku_comercios c ON c.id = r.comercio_id
     WHERE cj.usuario_id = $1
     ORDER BY cj.created_at DESC
     LIMIT 10`,
    [usuarioId]
  );

  const rubrosFrecuentes = await query(
    `SELECT COALESCE(c.categoria, 'otros') AS rubro, COUNT(*)::int AS total
     FROM piku_canjes cj
     INNER JOIN piku_recompensas r ON r.id = cj.recompensa_id
     INNER JOIN piku_comercios c ON c.id = r.comercio_id
     WHERE cj.usuario_id = $1
     GROUP BY c.categoria
     ORDER BY total DESC
     LIMIT 3`,
    [usuarioId]
  );

  const comerciosVisitados = await query(
    `SELECT c.id, c.nombre, COUNT(*)::int AS visitas
     FROM piku_eventos_usuario e
     INNER JOIN piku_comercios c ON c.id = e.comercio_id
     WHERE e.usuario_id = $1 AND e.tipo_evento IN ('vista_comercio', 'canje')
     GROUP BY c.id, c.nombre
     ORDER BY visitas DESC
     LIMIT 3`,
    [usuarioId]
  );

  return {
    ultimosCanjes: canjes.rows.map((r) => ({
      rubro: r.rubro,
      fecha: r.created_at,
      puntos_usados: r.puntos_usados,
      comercio: r.comercio_nombre,
    })),
    rubrosFrecuentes: rubrosFrecuentes.rows,
    comerciosVisitados: comerciosVisitados.rows,
  };
}

module.exports = { registrarEvento, resumenParaIA, TIPOS_VALIDOS };
