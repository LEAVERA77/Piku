const { prepararMesNuevoParaComercios, mesActualYyyyMm } = require('../services/suscripcion.service');

let ultimoMesEjecutado = null;
let intervalId = null;

async function ejecutarResetMensual() {
  const mes = mesActualYyyyMm();
  if (ultimoMesEjecutado === mes) return;
  try {
    await prepararMesNuevoParaComercios();
    ultimoMesEjecutado = mes;
  } catch (error) {
    console.error('resetContadoresMensuales:', error.message);
  }
}

function msHastaProximoDia(hora = 0, minuto = 1) {
  const ahora = new Date();
  const prox = new Date(ahora);
  prox.setHours(hora, minuto, 0, 0);
  if (prox <= ahora) prox.setDate(prox.getDate() + 1);
  return prox.getTime() - ahora.getTime();
}

function startResetContadoresMensualesJob() {
  if (process.env.DISABLE_RESET_CONTADORES === 'true') {
    console.log('ℹ️ Reset contadores mensuales deshabilitado');
    return;
  }

  ejecutarResetMensual().catch(() => {});

  const delay = msHastaProximoDia(0, 1);
  setTimeout(() => {
    ejecutarResetMensual().catch(() => {});
    intervalId = setInterval(() => {
      ejecutarResetMensual().catch(() => {});
    }, 24 * 60 * 60 * 1000);
  }, delay);

  console.log(`📅 Job contadores mensuales programado (cada día 00:01)`);
}

function stopResetContadoresMensualesJob() {
  if (intervalId) {
    clearInterval(intervalId);
    intervalId = null;
  }
}

module.exports = {
  ejecutarResetMensual,
  startResetContadoresMensualesJob,
  stopResetContadoresMensualesJob,
};
