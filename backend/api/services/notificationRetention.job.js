const { query } = require('./neon.service');

const INTERVALO_MS = 24 * 60 * 60 * 1000;

let intervalId = null;

async function purgeNotificacionesAntiguas() {
  const dias = Math.max(parseInt(process.env.NOTIFICACIONES_RETENCION_DIAS, 10) || 90, 7);
  try {
    const result = await query(
      `DELETE FROM piku_notificaciones
       WHERE created_at < NOW() - ($1::int * INTERVAL '1 day')
       RETURNING id`,
      [dias]
    );
    if (result.rowCount > 0) {
      console.log(`🧹 Retención: ${result.rowCount} notificaciones > ${dias} días eliminadas`);
    }
  } catch (error) {
    console.error('purgeNotificacionesAntiguas:', error.message);
  }
}

function startNotificationRetentionJob() {
  if (process.env.DISABLE_NOTIFICACIONES_RETENCION === 'true') {
    console.log('ℹ️ Retención de notificaciones deshabilitada');
    return;
  }
  purgeNotificacionesAntiguas().catch(() => {});
  intervalId = setInterval(() => {
    purgeNotificacionesAntiguas().catch(() => {});
  }, INTERVALO_MS);
}

function stopNotificationRetentionJob() {
  if (intervalId) {
    clearInterval(intervalId);
    intervalId = null;
  }
}

module.exports = {
  purgeNotificacionesAntiguas,
  startNotificationRetentionJob,
  stopNotificationRetentionJob,
};
