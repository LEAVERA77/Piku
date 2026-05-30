const { registrarEvento, TIPOS_VALIDOS } = require('../services/eventos.service');
const { responderError } = require('../utils/helpers');

async function crearEvento(req, res) {
  try {
    const tipo = String(req.body.tipo_evento || req.body.tipo || '').trim();
    if (!TIPOS_VALIDOS.has(tipo)) {
      return responderError(res, 400, 'tipo_evento inválido');
    }
    const comercioId = req.body.comercio_id || req.body.comercioId || null;
    const metadata = req.body.metadata || null;

    await registrarEvento(req.user.id, tipo, comercioId, metadata);
    return res.status(201).json({ ok: true });
  } catch (error) {
    console.error('crearEvento:', error);
    return responderError(res, 500, 'Error al registrar evento');
  }
}

module.exports = { crearEvento };
