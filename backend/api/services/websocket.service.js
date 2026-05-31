const WebSocket = require('ws');
const jwt = require('jsonwebtoken');
const { query } = require('./neon.service');
const { JWT_SECRET } = require('../middleware/auth.middleware');

const WS_PATH = '/ws/comercio';
/** @type {Map<string, Set<import('ws').WebSocket>>} */
const socketsPorComercio = new Map();

function agregarSocket(comercioId, ws) {
  if (!socketsPorComercio.has(comercioId)) {
    socketsPorComercio.set(comercioId, new Set());
  }
  socketsPorComercio.get(comercioId).add(ws);
}

function quitarSocket(comercioId, ws) {
  const set = socketsPorComercio.get(comercioId);
  if (!set) return;
  set.delete(ws);
  if (set.size === 0) socketsPorComercio.delete(comercioId);
}

function broadcastToComercio(comercioId, payload) {
  const set = socketsPorComercio.get(comercioId);
  if (!set || !set.size) return 0;

  const data = JSON.stringify(payload);
  let enviados = 0;
  for (const ws of set) {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(data);
      enviados += 1;
    }
  }
  return enviados;
}

async function autenticarToken(token) {
  const decoded = jwt.verify(token, JWT_SECRET);
  const userId = decoded.userId || decoded.sub;
  if (!userId) return null;

  const result = await query(
    `SELECT id, rol, comercio_id, activo FROM piku_usuarios WHERE id = $1 LIMIT 1`,
    [userId]
  );
  const user = result.rows[0];
  if (!user?.activo || user.rol !== 'comercio' || !user.comercio_id) {
    return null;
  }
  return user;
}

function attachWebSocketServer(httpServer) {
  const wss = new WebSocket.Server({ noServer: true });

  httpServer.on('upgrade', (request, socket, head) => {
    const url = new URL(request.url, `http://${request.headers.host}`);
    if (url.pathname !== WS_PATH) {
      socket.destroy();
      return;
    }

    wss.handleUpgrade(request, socket, head, (ws) => {
      wss.emit('connection', ws, request);
    });
  });

  wss.on('connection', async (ws, request) => {
    const url = new URL(request.url, `http://${request.headers.host}`);
    const token = url.searchParams.get('token');
    if (!token) {
      ws.close(4401, 'Token requerido');
      return;
    }

    let comercioId;
    try {
      const user = await autenticarToken(token);
      if (!user) {
        ws.close(4403, 'Solo comercio autenticado');
        return;
      }
      comercioId = user.comercio_id;
    } catch (_) {
      ws.close(4401, 'Token inválido');
      return;
    }

    agregarSocket(comercioId, ws);
    ws.send(JSON.stringify({ type: 'connected', comercio_id: comercioId }));

    ws.on('close', () => quitarSocket(comercioId, ws));
    ws.on('error', () => quitarSocket(comercioId, ws));
  });

  console.log(`🔌 WebSocket comercio: ${WS_PATH}`);
  return wss;
}

module.exports = {
  WS_PATH,
  attachWebSocketServer,
  broadcastToComercio,
};
