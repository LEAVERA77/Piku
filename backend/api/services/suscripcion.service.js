const { query } = require('./neon.service');
const { columnasTabla, tiene } = require('../utils/schema.util');
const { invalidarCacheComerciosSelect } = require('../utils/comercio.sql.util');
const {
  PLANES,
  normalizarPlan,
  obtenerLimitesPlan,
  esIlimitado,
} = require('../constants/planes.constants');

function mesActualYyyyMm() {
  const ahora = new Date();
  return ahora.getFullYear() * 100 + (ahora.getMonth() + 1);
}

async function obtenerPlanComercio(comercioId, client = null) {
  const q = client?.query?.bind(client) || query;
  const cols = await columnasTabla('piku_comercios');
  if (tiene(cols, 'plan')) {
    const res = await q('SELECT plan FROM piku_comercios WHERE id = $1', [comercioId]);
    if (res.rows[0]?.plan) return normalizarPlan(res.rows[0].plan);
  }
  try {
    const sub = await q(
      `SELECT plan FROM piku_suscripciones
       WHERE comercio_id = $1 AND activa = TRUE
       ORDER BY fecha_inicio DESC LIMIT 1`,
      [comercioId]
    );
    if (sub.rows[0]?.plan) return normalizarPlan(sub.rows[0].plan);
  } catch (_err) {
    // tabla puede no existir aún
  }
  return 'gratuito';
}

async function asegurarUsoMensual(comercioId, client = null) {
  const q = client?.query?.bind(client) || query;
  const mes = mesActualYyyyMm();
  try {
    const res = await q(
      `INSERT INTO piku_uso_mensual (comercio_id, mes, puntos_otorgados)
       VALUES ($1, $2, 0)
       ON CONFLICT (comercio_id, mes) DO UPDATE SET updated_at = NOW()
       RETURNING puntos_otorgados`,
      [comercioId, mes]
    );
    return { mes, puntosOtorgados: res.rows[0]?.puntos_otorgados ?? 0 };
  } catch (err) {
    if (/piku_uso_mensual|does not exist|no existe/i.test(err.message)) {
      return { mes, puntosOtorgados: 0, tablaAusente: true };
    }
    throw err;
  }
}

async function obtenerUsoMensual(comercioId, client = null) {
  const q = client?.query?.bind(client) || query;
  const mes = mesActualYyyyMm();
  try {
    const res = await q(
      `SELECT puntos_otorgados FROM piku_uso_mensual
       WHERE comercio_id = $1 AND mes = $2`,
      [comercioId, mes]
    );
    return res.rows[0]?.puntos_otorgados ?? 0;
  } catch (err) {
    if (/piku_uso_mensual|does not exist|no existe/i.test(err.message)) return 0;
    throw err;
  }
}

async function incrementarUsoMensual(comercioId, puntos, client = null) {
  const q = client?.query?.bind(client) || query;
  const pts = parseInt(puntos, 10) || 0;
  if (pts <= 0) return 0;
  const mes = mesActualYyyyMm();
  try {
    const res = await q(
      `INSERT INTO piku_uso_mensual (comercio_id, mes, puntos_otorgados)
       VALUES ($1, $2, $3)
       ON CONFLICT (comercio_id, mes) DO UPDATE SET
         puntos_otorgados = piku_uso_mensual.puntos_otorgados + EXCLUDED.puntos_otorgados,
         updated_at = NOW()
       RETURNING puntos_otorgados`,
      [comercioId, mes, pts]
    );
    return res.rows[0]?.puntos_otorgados ?? pts;
  } catch (err) {
    if (/piku_uso_mensual|does not exist|no existe/i.test(err.message)) return pts;
    throw err;
  }
}

async function verificarLimitePuntos(comercioId, puntosAAgregar = 0, client = null) {
  const plan = await obtenerPlanComercio(comercioId, client);
  const limites = obtenerLimitesPlan(plan);
  if (esIlimitado(limites.puntosMes)) {
    return { permitido: true, plan, puntosUsados: 0, puntosLimite: null, restantes: null };
  }

  const usados = await obtenerUsoMensual(comercioId, client);
  const agregar = parseInt(puntosAAgregar, 10) || 0;
  const limite = limites.puntosMes;
  const permitido = usados + agregar <= limite;

  return {
    permitido,
    plan,
    puntosUsados: usados,
    puntosLimite: limite,
    restantes: Math.max(0, limite - usados),
    porcentajeUso: limite > 0 ? Math.min(100, Math.round((usados / limite) * 100)) : 0,
  };
}

async function contarOfertasActivas(comercioId, client = null) {
  const q = client?.query?.bind(client) || query;
  const res = await q(
    `SELECT COUNT(*)::int AS n FROM piku_recompensas
     WHERE comercio_id = $1 AND activo = TRUE`,
    [comercioId]
  );
  return res.rows[0]?.n ?? 0;
}

async function verificarLimiteOfertas(comercioId, client = null) {
  const plan = await obtenerPlanComercio(comercioId, client);
  const limites = obtenerLimitesPlan(plan);
  const activas = await contarOfertasActivas(comercioId, client);

  if (esIlimitado(limites.ofertasActivas)) {
    return { permitido: true, plan, ofertasActivas: activas, ofertasLimite: null };
  }

  return {
    permitido: activas < limites.ofertasActivas,
    plan,
    ofertasActivas: activas,
    ofertasLimite: limites.ofertasActivas,
  };
}

async function obtenerEstadoSuscripcion(comercioId) {
  const plan = await obtenerPlanComercio(comercioId);
  const limites = obtenerLimitesPlan(plan);
  const puntosUsados = await obtenerUsoMensual(comercioId);
  const ofertasActivas = await contarOfertasActivas(comercioId);

  const puntosLimite = limites.puntosMes;
  const ofertasLimite = limites.ofertasActivas;
  const porcentajeUso = puntosLimite
    ? Math.min(100, Math.round((puntosUsados / puntosLimite) * 100))
    : 0;

  return {
    plan,
    planNombre: limites.nombre,
    precioUsd: limites.precioUsd,
    puntos_usados_mes: puntosUsados,
    puntos_limite: puntosLimite,
    puntos_restantes: puntosLimite != null ? Math.max(0, puntosLimite - puntosUsados) : null,
    porcentaje_uso: porcentajeUso,
    ofertas_activas: ofertasActivas,
    ofertas_limite: ofertasLimite,
    destacado: limites.destacado,
    limite_alcanzado: puntosLimite != null && puntosUsados >= puntosLimite,
    planes: Object.values(PLANES).map((p) => ({
      id: p.id,
      nombre: p.nombre,
      precioUsd: p.precioUsd,
      puntosMes: p.puntosMes,
      ofertasActivas: p.ofertasActivas,
      destacado: p.destacado,
    })),
  };
}

async function cambiarPlanComercio(comercioId, nuevoPlan) {
  const plan = normalizarPlan(nuevoPlan);
  const limites = obtenerLimitesPlan(plan);
  const cols = await columnasTabla('piku_comercios');

  if (tiene(cols, 'plan')) {
    await query(
      'UPDATE piku_comercios SET plan = $1, updated_at = NOW() WHERE id = $2',
      [plan, comercioId]
    );
  }

  try {
    await query(
      `UPDATE piku_suscripciones SET activa = FALSE, updated_at = NOW()
       WHERE comercio_id = $1 AND activa = TRUE`,
      [comercioId]
    );
    await query(
      `INSERT INTO piku_suscripciones (comercio_id, plan, activa, fecha_inicio)
       VALUES ($1, $2, TRUE, NOW())`,
      [comercioId, plan]
    );
  } catch (err) {
    if (!/piku_suscripciones|does not exist|no existe/i.test(err.message)) throw err;
  }

  if (tiene(cols, 'suscripcion_activa')) {
    await query(
      'UPDATE piku_comercios SET suscripcion_activa = TRUE, updated_at = NOW() WHERE id = $1',
      [comercioId]
    );
  }

  invalidarCacheComerciosSelect();
  return { plan, planNombre: limites.nombre, destacado: limites.destacado };
}

async function prepararMesNuevoParaComercios() {
  const mes = mesActualYyyyMm();
  try {
    const comercios = await query('SELECT id FROM piku_comercios');
    for (const row of comercios.rows) {
      await asegurarUsoMensual(row.id);
    }
    console.log(`📅 Uso mensual preparado para mes ${mes} (${comercios.rows.length} comercios)`);
    return comercios.rows.length;
  } catch (err) {
    if (/piku_uso_mensual|does not exist|no existe/i.test(err.message)) {
      console.warn('prepararMesNuevo: tabla ausente');
      return 0;
    }
    throw err;
  }
}

module.exports = {
  mesActualYyyyMm,
  obtenerPlanComercio,
  asegurarUsoMensual,
  obtenerUsoMensual,
  incrementarUsoMensual,
  verificarLimitePuntos,
  contarOfertasActivas,
  verificarLimiteOfertas,
  obtenerEstadoSuscripcion,
  cambiarPlanComercio,
  prepararMesNuevoParaComercios,
};
