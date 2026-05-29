const axios = require('axios');

const BASE_URL = (process.env.NOMINATIM_BASE_URL || 'https://nominatim.openstreetmap.org').replace(/\/$/, '');

const client = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: { 'User-Agent': 'Piku-Backend/1.0' },
});

/**
 * Convierte una dirección en texto a coordenadas lat/lon.
 */
async function geocodeAddress(direccion) {
  const q = String(direccion || '').trim();
  if (!q) throw new Error('Dirección requerida');

  const { data } = await client.get('/search', {
    params: { q, format: 'json', limit: 1 },
  });

  if (!Array.isArray(data) || !data.length) {
    throw new Error('No se encontró la dirección');
  }

  return {
    lat: parseFloat(data[0].lat),
    lon: parseFloat(data[0].lon),
    displayName: data[0].display_name,
  };
}

/**
 * Convierte coordenadas a dirección legible.
 */
async function reverseGeocode(lat, lon) {
  const { data } = await client.get('/reverse', {
    params: { lat, lon, format: 'json' },
  });

  return {
    lat: parseFloat(lat),
    lon: parseFloat(lon),
    displayName: data?.display_name || '',
  };
}

/**
 * Distancia en metros entre dos puntos (fórmula de Haversine).
 */
function calcularDistancia(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const toRad = (deg) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c);
}

module.exports = { geocodeAddress, reverseGeocode, calcularDistancia };
