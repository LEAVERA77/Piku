/**
 * Piku Points (PP) — reglas de negocio.
 * 1 PP por cada 1 USD gastado; 1 PP = 0.15 USD de descuento (15% reintegro).
 */
module.exports = {
  PESOS_POR_DOLAR_DEFAULT: parseFloat(process.env.PESOS_POR_DOLAR, 10) || 1400,
  GASTO_PARA_1_PUNTO_USD: parseFloat(process.env.GASTO_PARA_1_PUNTO_USD, 10) || 1,
  VALOR_DE_1_PUNTO_USD: parseFloat(process.env.VALOR_DE_1_PUNTO_USD, 10) || 0.15,
  TASA_DE_REINTEGRO: parseFloat(process.env.TASA_DE_REINTEGRO, 10) || 0.15,
  CACHE_DOLAR_MS: 60 * 60 * 1000,
  BONO_BIENVENIDA: 10,
  BONO_CUMPLEANOS: 5,
  BONO_COMPARTIR: 20,
  BONO_REFERIDO_INVITADOR: 20,
  BONO_REFERIDO_INVITADO: 10,
};
