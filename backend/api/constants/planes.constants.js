/**
 * Planes de suscripción Piku para comercios.
 */
const PLANES = {
  gratuito: {
    id: 'gratuito',
    nombre: 'Gratuito',
    precioUsd: 0,
    puntosMes: 500,
    ofertasActivas: 2,
    destacado: false,
  },
  basico: {
    id: 'basico',
    nombre: 'Básico',
    precioUsd: 5,
    puntosMes: 5000,
    ofertasActivas: 10,
    destacado: false,
  },
  pro: {
    id: 'pro',
    nombre: 'Pro',
    precioUsd: 15,
    puntosMes: null,
    ofertasActivas: null,
    destacado: true,
  },
};

const PLAN_IDS = new Set(Object.keys(PLANES));

function normalizarPlan(plan) {
  const p = String(plan || 'gratuito').toLowerCase().trim();
  return PLAN_IDS.has(p) ? p : 'gratuito';
}

function obtenerLimitesPlan(plan) {
  const p = normalizarPlan(plan);
  return PLANES[p];
}

function esIlimitado(valor) {
  return valor == null;
}

module.exports = {
  PLANES,
  PLAN_IDS,
  normalizarPlan,
  obtenerLimitesPlan,
  esIlimitado,
};
