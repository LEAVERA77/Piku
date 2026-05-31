const { query } = require('../services/neon.service');
const { columnasTabla, tiene } = require('./schema.util');

async function tablaImagenesDisponible() {
  const cols = await columnasTabla('piku_recompensa_imagenes');
  return cols.size > 0;
}

async function listarImagenesGaleria(recompensaId) {
  if (!(await tablaImagenesDisponible())) return [];
  const result = await query(
    `SELECT id, recompensa_id, imagen_url, orden, created_at
     FROM piku_recompensa_imagenes
     WHERE recompensa_id = $1
     ORDER BY orden ASC, created_at ASC`,
    [recompensaId]
  );
  return result.rows;
}

async function adjuntarImagenesARecompensa(recompensa) {
  if (!recompensa?.id) return recompensa;
  const imagenes = await listarImagenesGaleria(recompensa.id);
  const urls = [];
  if (recompensa.imagen_url) urls.push(recompensa.imagen_url);
  for (const img of imagenes) {
    if (img.imagen_url && !urls.includes(img.imagen_url)) urls.push(img.imagen_url);
  }
  return {
    ...recompensa,
    imagenes,
    imagenes_urls: urls,
    imagenes_extra: Math.max(0, urls.length - (recompensa.imagen_url ? 1 : 0)),
  };
}

async function siguienteOrdenGaleria(recompensaId) {
  if (!(await tablaImagenesDisponible())) return 0;
  const r = await query(
    'SELECT COALESCE(MAX(orden), -1) + 1 AS next FROM piku_recompensa_imagenes WHERE recompensa_id = $1',
    [recompensaId]
  );
  return parseInt(r.rows[0].next, 10) || 0;
}

module.exports = {
  tablaImagenesDisponible,
  listarImagenesGaleria,
  adjuntarImagenesARecompensa,
  siguienteOrdenGaleria,
  tiene,
};
