const { validarTelefonoComercio } = require('../utils/telefono.util');
const { responderError } = require('../utils/helpers');

/**
 * Exige teléfono válido en body (registro / actualización de comercio).
 */
function requiereTelefonoComercio(req, res, next) {
  const telefono = req.body.telefono ?? req.body.telefono_contacto;
  const resultado = validarTelefonoComercio(telefono);
  if (!resultado.ok) {
    return responderError(res, 400, resultado.mensaje);
  }
  req.telefonoNormalizado = resultado.normalizado;
  return next();
}

module.exports = { requiereTelefonoComercio, validarTelefonoComercio };
