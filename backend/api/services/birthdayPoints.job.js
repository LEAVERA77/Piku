const { otorgarBonosCumpleanos } = require('./puntos.service');

const INTERVALO_MS = 24 * 60 * 60 * 1000;
let intervalId = null;

function msHastaProximaHora(horaLocal = 8) {
  const ahora = new Date();
  const prox = new Date(ahora);
  prox.setHours(horaLocal, 0, 0, 0);
  if (prox <= ahora) prox.setDate(prox.getDate() + 1);
  return prox.getTime() - ahora.getTime();
}

async function ejecutarBonosCumpleanos() {
  try {
    const n = await otorgarBonosCumpleanos();
    if (n > 0) console.log(`🎂 Bonos de cumpleaños otorgados: ${n}`);
  } catch (error) {
    console.error('birthdayPoints:', error.message);
  }
}

function startBirthdayPointsJob() {
  if (process.env.DISABLE_BIRTHDAY_POINTS === 'true') {
    console.log('ℹ️ Bonos de cumpleaños deshabilitados');
    return;
  }

  const delay = msHastaProximaHora(8);
  setTimeout(() => {
    ejecutarBonosCumpleanos().catch(() => {});
    intervalId = setInterval(() => {
      ejecutarBonosCumpleanos().catch(() => {});
    }, INTERVALO_MS);
  }, delay);

  console.log(`🎂 Job cumpleaños programado (primera corrida en ${Math.round(delay / 60000)} min)`);
}

function stopBirthdayPointsJob() {
  if (intervalId) {
    clearInterval(intervalId);
    intervalId = null;
  }
}

module.exports = {
  ejecutarBonosCumpleanos,
  startBirthdayPointsJob,
  stopBirthdayPointsJob,
};
