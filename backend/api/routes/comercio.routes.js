const express = require('express');
const multer = require('multer');
const comercioController = require('../controllers/comercio.controller');
const { authMiddleware } = require('../middleware/auth.middleware');
const { soloComercio } = require('../middleware/roles.middleware');
const {
  comercioNotificacionesLimiter,
  comercioCanjesLimiter,
} = require('../middleware/rateLimit.middleware');

const router = express.Router();
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 },
});

router.use(authMiddleware, soloComercio);

router.get('/reglas', comercioController.getReglasPuntos);
router.put('/reglas', comercioController.updateReglasPuntos);
router.get('/recompensas', comercioController.getRecompensas);
router.get('/recompensas/:id', comercioController.getRecompensa);
router.get('/recompensas/:id/stats', comercioController.getRecompensaStats);
router.post('/recompensas', comercioController.createRecompensa);
router.post('/recompensas/:id/duplicar', comercioController.duplicateRecompensa);
router.put('/recompensas/:id', comercioController.updateRecompensa);
router.delete('/recompensas/:id', comercioController.deleteRecompensa);
router.post('/recompensas/:id/imagen', upload.single('file'), comercioController.uploadImagenRecompensa);
router.get('/recompensas/:id/imagenes', comercioController.listarImagenesRecompensa);
router.post('/recompensas/:id/imagenes', upload.single('file'), comercioController.uploadImagenGaleria);
router.delete('/recompensas/:id/imagenes/:imagenId', comercioController.eliminarImagenGaleria);
router.put('/recompensas/:id/portada', comercioController.establecerPortadaRecompensa);
router.post('/generar-qr', comercioController.generarQR);
router.get('/estadisticas', comercioController.getEstadisticas);
router.get('/envios', comercioController.getConfigEnvios);
router.put('/envios', comercioController.updateConfigEnvios);
router.get('/notificaciones', comercioNotificacionesLimiter, comercioController.obtenerNotificaciones);
router.get(
  '/notificaciones/no-leidas',
  comercioNotificacionesLimiter,
  comercioController.contarNotificacionesNoLeidas
);
router.put(
  '/notificaciones/:id/leer',
  comercioNotificacionesLimiter,
  comercioController.marcarNotificacionLeida
);
router.get('/canjes', comercioCanjesLimiter, comercioController.obtenerHistorialCanjes);
router.put('/dispositivo/fcm', comercioController.registrarFcmToken);
router.post('/logo', upload.single('file'), comercioController.uploadLogoComercio);

module.exports = router;
