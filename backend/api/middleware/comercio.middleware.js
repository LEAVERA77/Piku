const { responderError } = require('../utils/helpers');
const { resolveComercioId } = require('../utils/comercio.resolve.util');

async function attachComercioId(req, res, next) {
  try {
    const comercioId = await resolveComercioId(req);
    if (!comercioId) {
      return responderError(res, 403, 'Sin comercio asociado a tu cuenta');
    }
    req.comercioId = comercioId;
    return next();
  } catch (error) {
    console.error('attachComercioId:', error);
    return responderError(res, 500, 'Error al resolver comercio');
  }
}

module.exports = { attachComercioId };
