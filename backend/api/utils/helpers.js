const { v4: uuidv4 } = require('uuid');

/**
 * Genera un código único para canjes o QR.
 */
function generarCodigoUnico(prefijo = 'PIKU') {
  const fragmento = uuidv4().replace(/-/g, '').slice(0, 12).toUpperCase();
  return `${prefijo}-${fragmento}`;
}

/**
 * Valida formato básico de email.
 */
function validarEmail(email) {
  const valor = String(email || '').trim();
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(valor);
}

/**
 * Limpia strings de entrada (trim y longitud máxima).
 */
function sanitizarInput(valor, maxLen = 500) {
  if (valor == null) return '';
  return String(valor).trim().slice(0, maxLen);
}

/**
 * Respuesta de error estándar.
 */
function responderError(res, status, mensaje, extra = {}) {
  return res.status(status).json({ error: mensaje, ...extra });
}

module.exports = {
  generarCodigoUnico,
  validarEmail,
  sanitizarInput,
  responderError,
};
