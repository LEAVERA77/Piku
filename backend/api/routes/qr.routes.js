const express = require('express');
const qrController = require('../controllers/qr.controller');
const { authMiddleware } = require('../middleware/auth.middleware');
const { soloCliente, soloComercio } = require('../middleware/roles.middleware');

const router = express.Router();

router.post('/validar', authMiddleware, soloCliente, qrController.validarEscaneo);
router.post('/generar', authMiddleware, soloComercio, qrController.generarQR);

module.exports = router;
