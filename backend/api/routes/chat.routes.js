const express = require('express');
const chatController = require('../controllers/chat.controller');
const { authMiddleware } = require('../middleware/auth.middleware');
const { soloCliente } = require('../middleware/roles.middleware');

const router = express.Router();

router.use(authMiddleware, soloCliente);
router.post('/chat-piku', chatController.chatPiku);

module.exports = router;
