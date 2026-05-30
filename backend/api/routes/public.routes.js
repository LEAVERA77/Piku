const express = require('express');
const publicController = require('../controllers/public.controller');

const router = express.Router();

router.get('/rubros', publicController.listarRubros);
router.get('/comercios', publicController.listarComercios);
router.get('/comercios/:id', publicController.detalleComercio);
router.get('/recompensas', publicController.listarRecompensasPublicas);

module.exports = router;
