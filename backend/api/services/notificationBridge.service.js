const { onPgNotification } = require('./notificationListener');
const { broadcastToComercio } = require('./websocket.service');
const { sendComercioPush } = require('./fcm.service');

function wireNotificationHandlers() {
  onPgNotification((payload) => {
    const comercioId = payload?.comercio_id;
    if (!comercioId) return;

    const evento = {
      type: 'notification',
      comercio_id: comercioId,
      notification_id: payload.id || null,
      notif_tipo: payload.tipo || 'canje',
    };

    const enviados = broadcastToComercio(comercioId, evento);
    if (process.env.NODE_ENV !== 'production' && enviados > 0) {
      console.log(`WebSocket: ${enviados} cliente(s) comercio ${comercioId}`);
    }

    sendComercioPush(comercioId, payload).catch((err) => {
      console.error('FCM bridge:', err.message);
    });
  });
}

module.exports = { wireNotificationHandlers };
