const express = require('express');
const usuarioController = require('../controllers/usuario.controller');
const eventosController = require('../controllers/eventos.controller');
const { authMiddleware } = require('../middleware/auth.middleware');
const { soloCliente } = require('../middleware/roles.middleware');

const router = express.Router();

router.use(authMiddleware, soloCliente);

router.get('/saldo', usuarioController.getSaldoPuntos);
router.get('/historial', usuarioController.getHistorialPuntos);
router.get('/recompensas', usuarioController.getRecompensasDisponibles);
router.post('/canjear', usuarioController.canjearRecompensa);
router.post('/eventos', eventosController.crearEvento);

module.exports = router;
