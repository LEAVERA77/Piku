const { Client } = require('pg');
const { EventEmitter } = require('events');

const CANAL = 'piku_notificaciones';
const emitter = new EventEmitter();

let listenClient = null;
let reconnectTimer = null;

/**
 * URL directa a Neon (sin pooler). LISTEN/NOTIFY no funciona con PgBouncer en modo transaction.
 */
function resolveDirectConnectionString() {
  const direct = process.env.DATABASE_URL_DIRECT;
  if (direct) {
    return direct.includes('-pooler') ? direct.replace(/-pooler\./g, '.') : direct;
  }

  const pooled = process.env.DATABASE_URL;
  if (!pooled) return null;

  if (pooled.includes('-pooler')) {
    console.warn(
      '⚠️ LISTEN/NOTIFY: DATABASE_URL usa pooler de Neon. Definí DATABASE_URL_DIRECT (host sin -pooler) para el listener.'
    );
    return pooled.replace(/-pooler\./g, '.');
  }
  return pooled;
}

async function connectAndListen() {
  const connectionString = resolveDirectConnectionString();
  if (!connectionString) {
    console.warn('⚠️ LISTEN/NOTIFY: sin DATABASE_URL; listener desactivado.');
    return;
  }

  if (listenClient) {
    try {
      await listenClient.end();
    } catch (_) {
      /* ignore */
    }
    listenClient = null;
  }

  const client = new Client({
    connectionString,
    ssl: connectionString.includes('neon.tech') ? { rejectUnauthorized: false } : false,
  });

  client.on('error', (err) => {
    console.error('LISTEN client error:', err.message);
    scheduleReconnect();
  });

  client.on('notification', (msg) => {
    if (msg.channel !== CANAL) return;
    let payload = null;
    try {
      payload = msg.payload ? JSON.parse(msg.payload) : null;
    } catch (_) {
      if (process.env.NODE_ENV !== 'production') {
        console.warn('NOTIFY payload no JSON');
      }
      return;
    }
    emitter.emit('notification', payload);
    if (process.env.NODE_ENV !== 'production') {
      console.log(`📨 NOTIFY ${CANAL}:`, payload?.tipo || 'evento', payload?.comercio_id || '');
    }
  });

  await client.connect();
  await client.query(`LISTEN ${CANAL}`);
  listenClient = client;
  console.log(`👂 Escuchando canal PostgreSQL "${CANAL}"`);
}

function scheduleReconnect() {
  if (reconnectTimer) return;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    startNotificationListener().catch((err) => {
      console.error('Reintento LISTEN falló:', err.message);
      scheduleReconnect();
    });
  }, 5000);
}

async function startNotificationListener() {
  if (process.env.DISABLE_PG_LISTEN === 'true') {
    console.log('ℹ️ LISTEN/NOTIFY deshabilitado (DISABLE_PG_LISTEN=true)');
    return;
  }
  try {
    await connectAndListen();
  } catch (error) {
    console.error('No se pudo iniciar LISTEN/NOTIFY:', error.message);
    scheduleReconnect();
  }
}

async function stopNotificationListener() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  if (listenClient) {
    await listenClient.end();
    listenClient = null;
  }
}

module.exports = {
  startNotificationListener,
  stopNotificationListener,
  onPgNotification: (handler) => emitter.on('notification', handler),
  notificationEmitter: emitter,
};
