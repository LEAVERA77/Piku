const { query } = require('./neon.service');
const { columnasTabla, tiene } = require('../utils/schema.util');

const TIPOS_VALIDOS = new Set([
  'vista_comercio',
  'canje',
  'busqueda',
  'mapa_abierto',
  'chat_piku',
]);

async function tablaEventosDisponible() {
  try {
    const cols = await columnasTabla('piku_eventos_usuario');
    return cols.size > 0;
  } catch {
    return false;
  }
}

async function registrarEvento(usuarioId, tipoEvento, comercioId = null, metadata = null) {
  if (!TIPOS_VALIDOS.has(tipoEvento)) {
    throw new Error('tipo_evento inválido');
  }
  if (!(await tablaEventosDisponible())) {
    return;
  }
  await query(
    `INSERT INTO piku_eventos_usuario (usuario_id, tipo_evento, comercio_id, metadata)
     VALUES ($1, $2, $3, $4)`,
    [usuarioId, tipoEvento, comercioId || null, metadata ? JSON.stringify(metadata) : null]
  );
}

async function resumenParaIA(usuarioId) {
  const vacio = {
    ultimosCanjes: [],
    rubrosFrecuentes: [],
    comerciosVisitados: [],
  };

  try {
    const colsComercio = await columnasTabla('piku_comercios');
    const rubroCol = tiene(colsComercio, 'categoria') ? 'c.categoria' : "'otros'";

    const canjes = await query(
      `SELECT cj.created_at, cj.puntos_usados, c.nombre AS comercio_nombre, ${rubroCol} AS rubro
       FROM piku_canjes cj
       INNER JOIN piku_recompensas r ON r.id = cj.recompensa_id
       INNER JOIN piku_comercios c ON c.id = r.comercio_id
       WHERE cj.usuario_id = $1
       ORDER BY cj.created_at DESC
       LIMIT 10`,
      [usuarioId]
    );

    const rubrosFrecuentes = await query(
      `SELECT COALESCE(${rubroCol}, 'otros') AS rubro, COUNT(*)::int AS total
       FROM piku_canjes cj
       INNER JOIN piku_recompensas r ON r.id = cj.recompensa_id
       INNER JOIN piku_comercios c ON c.id = r.comercio_id
       WHERE cj.usuario_id = $1
       GROUP BY ${rubroCol}
       ORDER BY total DESC
       LIMIT 3`,
      [usuarioId]
    );

    let comerciosVisitados = { rows: [] };
    if (await tablaEventosDisponible()) {
      comerciosVisitados = await query(
        `SELECT c.id, c.nombre, COUNT(*)::int AS visitas
         FROM piku_eventos_usuario e
         INNER JOIN piku_comercios c ON c.id = e.comercio_id
         WHERE e.usuario_id = $1 AND e.tipo_evento IN ('vista_comercio', 'canje')
         GROUP BY c.id, c.nombre
         ORDER BY visitas DESC
         LIMIT 3`,
        [usuarioId]
      );
    }

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
  } catch (error) {
    console.warn('resumenParaIA:', error.message);
    return vacio;
  }
}

module.exports = { registrarEvento, resumenParaIA, TIPOS_VALIDOS };
