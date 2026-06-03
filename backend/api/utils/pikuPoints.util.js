const {
  GASTO_PARA_1_PUNTO_USD,
  VALOR_DE_1_PUNTO_USD,
  TASA_DE_REINTEGRO,
} = require('../constants/puntos.constants');

/**
 * Puntos ganados por compra en ARS según cotización del dólar.
 * Ej: $14.000 ARS / 1400 = 10 USD → 10 PP
 */
function calcularPuntosDesdePesos(montoPesos, pesosPorDolar) {
  const monto = parseFloat(montoPesos) || 0;
  const cotizacion = parseFloat(pesosPorDolar) || 0;
  if (monto <= 0 || cotizacion <= 0) return 0;
  const gastoUsd = monto / cotizacion;
  return Math.floor(gastoUsd / GASTO_PARA_1_PUNTO_USD);
}

/**
 * Descuento en ARS al canjear puntos.
 * Ej: 10 PP × 0.15 USD × 1400 = $2.100 ARS
 */
function calcularDescuentoArs(puntos, pesosPorDolar) {
  const pts = parseInt(puntos, 10) || 0;
  const cotizacion = parseFloat(pesosPorDolar) || 0;
  if (pts <= 0 || cotizacion <= 0) return 0;
  const descuentoUsd = pts * VALOR_DE_1_PUNTO_USD;
  return Math.round(descuentoUsd * cotizacion);
}

function calcularDescuentoUsd(puntos) {
  const pts = parseInt(puntos, 10) || 0;
  if (pts <= 0) return 0;
  return Math.round(pts * VALOR_DE_1_PUNTO_USD * 100) / 100;
}

function resumenPikuPoints(puntos, pesosPorDolar) {
  const pts = parseInt(puntos, 10) || 0;
  return {
    puntos: pts,
    pesosPorDolar,
    valorPuntoUsd: VALOR_DE_1_PUNTO_USD,
    tasaReintegro: TASA_DE_REINTEGRO,
    descuentoUsd: calcularDescuentoUsd(pts),
    equivalenciaDescuentoArs: calcularDescuentoArs(pts, pesosPorDolar),
  };
}

module.exports = {
  calcularPuntosDesdePesos,
  calcularDescuentoArs,
  calcularDescuentoUsd,
  resumenPikuPoints,
  VALOR_DE_1_PUNTO_USD,
  TASA_DE_REINTEGRO,
};
