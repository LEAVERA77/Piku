const express = require('express');
const authController = require('../controllers/auth.controller');
const { authMiddleware } = require('../middleware/auth.middleware');
const { soloAdmin } = require('../middleware/roles.middleware');

const router = express.Router();

router.post('/registro-cliente', authController.registroCliente);
router.post('/registro-comercio', authController.registroComercio);
router.post('/registro-comercio-admin', authMiddleware, soloAdmin, authController.registroComercio);
router.post('/login', authController.login);
router.post('/google', authController.loginGoogle);
router.get('/perfil', authMiddleware, authController.perfil);
router.put('/perfil', authMiddleware, authController.actualizarPerfil);

module.exports = router;
