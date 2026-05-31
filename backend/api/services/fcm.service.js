const { query } = require('./neon.service');

let admin = null;
let messaging = null;

function initFcm() {
  if (messaging) return true;

  const rawB64 = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  const rawPlain = process.env.FIREBASE_SERVICE_ACCOUNT;
  let serviceAccount = null;

  try {
    if (rawB64) {
      serviceAccount = JSON.parse(Buffer.from(rawB64, 'base64').toString('utf8'));
    } else if (rawPlain) {
      serviceAccount = JSON.parse(rawPlain);
    }
  } catch (error) {
    console.error('FCM: JSON de cuenta de servicio inválido:', error.message);
    return false;
  }

  if (!serviceAccount) {
    return false;
  }

  try {
    // eslint-disable-next-line global-require
    admin = require('firebase-admin');
    if (!admin.apps.length) {
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
      });
    }
    messaging = admin.messaging();
    console.log('✅ FCM inicializado');
    return true;
  } catch (error) {
    console.warn('⚠️ FCM no disponible (firebase-admin):', error.message);
    return false;
  }
}

/**
 * Envía push al usuario comercio con token FCM registrado.
 */
async function sendComercioPush(comercioId, notifyPayload) {
  if (!comercioId || !initFcm()) return;

  try {
    const users = await query(
      `SELECT id, fcm_token FROM piku_usuarios
       WHERE comercio_id = $1 AND activo = TRUE AND fcm_token IS NOT NULL`,
      [comercioId]
    );
    if (!users.rows.length) return;

    let titulo = 'Piku';
    let cuerpo = 'Tenés una nueva notificación';
    if (notifyPayload?.id) {
      const det = await query(
        `SELECT titulo, cuerpo FROM piku_notificaciones WHERE id = $1 AND comercio_id = $2`,
        [notifyPayload.id, comercioId]
      );
      if (det.rows.length) {
        titulo = det.rows[0].titulo;
        cuerpo = det.rows[0].cuerpo;
      }
    }

    const tokens = users.rows.map((r) => r.fcm_token).filter(Boolean);
    if (!tokens.length) return;

    const response = await messaging.sendEachForMulticast({
      tokens,
      notification: { title: titulo, body: cuerpo },
      data: {
        tipo: String(notifyPayload?.tipo || 'canje'),
        comercio_id: String(comercioId),
        notification_id: String(notifyPayload?.id || ''),
      },
      android: { priority: 'high' },
    });

    if (process.env.NODE_ENV !== 'production' && response.failureCount > 0) {
      console.warn(`FCM: ${response.failureCount} envíos fallidos`);
    }
  } catch (error) {
    console.error('sendComercioPush:', error.message);
  }
}

module.exports = {
  initFcm,
  sendComercioPush,
};
