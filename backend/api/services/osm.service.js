/**
 * Integración con la API de Notes de OpenStreetMap (v0.6).
 * @see https://wiki.openstreetmap.org/wiki/API_v0.6#Notes
 */

const axios = require('axios');

const OSM_API_BASE =
  (process.env.OSM_API_BASE || 'https://api.openstreetmap.org/api/0.6').replace(/\/$/, '');

const OSM_USER_AGENT =
  process.env.OSM_USER_AGENT ||
  'Piku/1.0 (fidelizacion; +https://github.com/LEAVERA77/Piku)';

const PIKU_PUBLIC_URL = (process.env.PIKU_PUBLIC_URL || 'https://piku-324e.onrender.com').replace(
  /\/$/,
  ''
);

/**
 * Crea un Note abierto en OSM en las coordenadas indicadas.
 * @param {number} lat
 * @param {number} lon
 * @param {string} text
 * @returns {Promise<{ noteId: number|null, raw: string }>}
 */
async function createNote(lat, lon, text) {
  const latNum = Number(lat);
  const lonNum = Number(lon);
  if (!Number.isFinite(latNum) || !Number.isFinite(lonNum)) {
    throw new Error('Coordenadas inválidas para Note de OSM');
  }
  if (!text || !String(text).trim()) {
    throw new Error('Texto del Note vacío');
  }

  const url = `${OSM_API_BASE}/notes`;
  const response = await axios.post(url, null, {
    params: {
      lat: latNum,
      lon: lonNum,
      text: String(text).trim(),
    },
    headers: {
      'User-Agent': OSM_USER_AGENT,
    },
    timeout: 20000,
    validateStatus: (s) => s >= 200 && s < 300,
    responseType: 'text',
  });

  const noteId = extraerNoteId(response.data);
  return { noteId, raw: response.data };
}

/**
 * Texto estándar del Note para un comercio recién registrado en Piku.
 */
function buildComercioNoteText(comercio) {
  const nombre = comercio.nombre || 'Comercio';
  const categoria = comercio.categoria || 'Sin categoría';
  const direccion = comercio.direccion || 'Sin dirección indicada';
  const id = comercio.id || '';
  const enlace = `${PIKU_PUBLIC_URL}/comercio/${id}`;

  return [
    '📍 NUEVO COMERCIO registrado en PIKU',
    '',
    `Nombre: ${nombre}`,
    `Categoría: ${categoria}`,
    `Dirección: ${direccion}`,
    '',
    '💚 App de fidelización por puntos',
    `🔗 Más info: ${enlace}`,
  ].join('\n');
}

/**
 * Crea Note en OSM para un comercio (no lanza si faltan coordenadas).
 */
async function createNoteForComercio(comercio) {
  const lat = comercio.lat;
  const lon = comercio.lon;
  if (lat == null || lon == null) {
    console.warn('[OSM] Comercio sin lat/lon, se omite Note:', comercio.id);
    return null;
  }
  const text = buildComercioNoteText(comercio);
  return createNote(lat, lon, text);
}

function extraerNoteId(xmlOrText) {
  if (!xmlOrText || typeof xmlOrText !== 'string') return null;
  const match = xmlOrText.match(/<note[^>]*\sid="(\d+)"/i);
  return match ? Number(match[1]) : null;
}

module.exports = {
  createNote,
  buildComercioNoteText,
  createNoteForComercio,
};
