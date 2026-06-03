const axios = require('axios');
const {
  PESOS_POR_DOLAR_DEFAULT,
  CACHE_DOLAR_MS,
} = require('../constants/puntos.constants');

const DOLAR_API_URL = 'https://dolarapi.com/v1/dolares/blue';

let cache = {
  valor: PESOS_POR_DOLAR_DEFAULT,
  fetchedAt: 0,
  fuente: 'default',
};

/**
 * Cotización USD blue en ARS. Cache 1 hora; fallback al último valor o default.
 */
async function obtenerPesosPorDolar(force = false) {
  const ahora = Date.now();
  if (!force && cache.fetchedAt && ahora - cache.fetchedAt < CACHE_DOLAR_MS) {
    return cache.valor;
  }

  try {
    const res = await axios.get(DOLAR_API_URL, { timeout: 8000 });
    const venta = parseFloat(res.data?.venta);
    if (Number.isFinite(venta) && venta > 0) {
      cache = { valor: venta, fetchedAt: ahora, fuente: 'dolarapi' };
      return venta;
    }
  } catch (error) {
    console.warn('dolar.service: no se pudo actualizar cotización:', error.message);
  }

  if (cache.fetchedAt) return cache.valor;
  return PESOS_POR_DOLAR_DEFAULT;
}

function getCotizacionCacheada() {
  return {
    pesosPorDolar: cache.valor,
    fuente: cache.fuente,
    actualizadoEn: cache.fetchedAt ? new Date(cache.fetchedAt).toISOString() : null,
  };
}

async function refrescarCotizacion() {
  return obtenerPesosPorDolar(true);
}

module.exports = {
  obtenerPesosPorDolar,
  getCotizacionCacheada,
  refrescarCotizacion,
};
