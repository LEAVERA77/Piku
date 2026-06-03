require('dotenv').config();
const http = require('http');
const path = require('path');
const fs = require('fs');
const express = require('express');
const cors = require('cors');
const { UPLOAD_ROOT } = require('./utils/uploadImagen.util');

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
const { refrescarCotizacion } = require('./services/dolar.service');
const { startResetContadoresMensualesJob } = require('./jobs/resetContadoresMensuales');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json({ limit: '2mb' }));
app.use(express.urlencoded({ extended: true }));

fs.mkdirSync(UPLOAD_ROOT, { recursive: true });
app.use('/uploads', express.static(UPLOAD_ROOT));

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
  if (err?.code === 'LIMIT_FILE_SIZE') {
    return res.status(400).json({ error: 'Imagen demasiado grande (máx. 5 MB)' });
  }
  if (err?.name === 'MulterError') {
    return res.status(400).json({ error: err.message || 'Error al subir archivo' });
  }
  console.error('Error no controlado:', err);
  res.status(500).json({ error: 'Error interno del servidor', detail: err?.message });
});

async function start() {
  try {
    await runStartupMigrations();
    const { columnasTabla } = require('./utils/schema.util');
    await columnasTabla('piku_usuarios');
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
  startResetContadoresMensualesJob();

  refrescarCotizacion()
    .then((valor) => console.log(`💵 Cotización USD blue: $${valor} ARS`))
    .catch((err) => console.warn('⚠️ Cotización inicial:', err.message));
}

start();
