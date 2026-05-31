const express = require('express');
const multer = require('multer');
const comercioController = require('../controllers/comercio.controller');
const { authMiddleware } = require('../middleware/auth.middleware');
const { soloComercio } = require('../middleware/roles.middleware');

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
router.post('/generar-qr', comercioController.generarQR);
router.get('/estadisticas', comercioController.getEstadisticas);
router.get('/envios', comercioController.getConfigEnvios);
router.put('/envios', comercioController.updateConfigEnvios);

module.exports = router;
