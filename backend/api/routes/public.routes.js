const express = require('express');
const publicController = require('../controllers/public.controller');

const router = express.Router();

router.get('/rubros', publicController.listarRubros);
router.get('/comercios', publicController.listarComercios);
router.get('/comercios/cercanos', publicController.comerciosCercanos);
router.get('/comercios/:id/ofertas', publicController.ofertasComercio);
router.get('/comercios/:id', publicController.detalleComercio);
router.get('/recompensas', publicController.listarRecompensasPublicas);
router.get('/recompensas/:id', publicController.detalleRecompensa);

module.exports = router;
