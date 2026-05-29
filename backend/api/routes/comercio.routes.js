const express = require('express');
const comercioController = require('../controllers/comercio.controller');
const { authMiddleware } = require('../middleware/auth.middleware');
const { soloComercio } = require('../middleware/roles.middleware');

const router = express.Router();

router.use(authMiddleware, soloComercio);

router.get('/reglas', comercioController.getReglasPuntos);
router.put('/reglas', comercioController.updateReglasPuntos);
router.get('/recompensas', comercioController.getRecompensas);
router.post('/recompensas', comercioController.createRecompensa);
router.put('/recompensas/:id', comercioController.updateRecompensa);
router.delete('/recompensas/:id', comercioController.deleteRecompensa);
router.post('/generar-qr', comercioController.generarQR);
router.get('/estadisticas', comercioController.getEstadisticas);

module.exports = router;
