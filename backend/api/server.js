require('dotenv').config();
const http = require('http');
const express = require('express');
const cors = require('cors');

const authRoutes = require('./routes/auth.routes');
const usuarioRoutes = require('./routes/usuario.routes');
const qrRoutes = require('./routes/qr.routes');
const comercioRoutes = require('./routes/comercio.routes');
const publicRoutes = require('./routes/public.routes');
const publicController = require('./controllers/public.controller');
const chatRoutes = require('./routes/chat.routes');
const { runStartupMigrations } = require('./services/migrate.service');
const { startNotificationListener } = require('./services/notificationListener');
const { startNotificationRetentionJob } = require('./services/notificationRetention.job');
const { attachWebSocketServer } = require('./services/websocket.service');
const { wireNotificationHandlers } = require('./services/notificationBridge.service');
const { initFcm } = require('./services/fcm.service');
const { startBirthdayPointsJob } = require('./services/birthdayPoints.job');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json({ limit: '2mb' }));
app.use(express.urlencoded({ extended: true }));

app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'Piku API',
    timestamp: new Date().toISOString(),
  });
});

app.get('/', (req, res) => {
  res.json({
    message: 'Bienvenido a Piku API',
    tagline: 'Tus puntos, tus descuentos',
    version: '1.0.0',
  });
});

app.use('/api/auth', authRoutes);
app.use('/api/usuario', usuarioRoutes);
app.use('/api/qr', qrRoutes);
app.use('/api/comercio', comercioRoutes);
app.get('/api/rubros', publicController.listarRubros);
app.use('/api/public', publicRoutes);
app.use('/api', chatRoutes);

app.use((req, res) => {
  res.status(404).json({ error: 'Ruta no encontrada', path: req.path });
});

app.use((err, req, res, _next) => {
  console.error('Error no controlado:', err);
  res.status(500).json({ error: 'Error interno del servidor' });
});

async function start() {
  try {
    await runStartupMigrations();
  } catch (error) {
    console.error('⚠️ Migraciones al inicio:', error.message);
  }

  initFcm();

  const httpServer = http.createServer(app);
  attachWebSocketServer(httpServer);
  wireNotificationHandlers();

  httpServer.listen(PORT, () => {
    console.log(`✅ Servidor Piku corriendo en puerto ${PORT}`);
    console.log(`📡 Health: http://localhost:${PORT}/health`);
    console.log(`🔐 Auth:   http://localhost:${PORT}/api/auth/login`);
    console.log(`🔌 WS:     ws://localhost:${PORT}/ws/comercio?token=JWT`);
  });

  startNotificationListener().catch((err) => {
    console.error('⚠️ Listener NOTIFY:', err.message);
  });

  startNotificationRetentionJob();
  startBirthdayPointsJob();
}

start();
