const { sanitizarInput } = require('./helpers');

/**
 * Normaliza teléfono argentino: solo dígitos.
 */
function soloDigitos(telefono) {
  return String(telefono || '').replace(/\D/g, '');
}

/**
 * Valida teléfono para registro de comercio (obligatorio, mín. 8 dígitos).
 */
function validarTelefonoComercio(telefono) {
  const raw = sanitizarInput(telefono, 50);
  if (!raw) return { ok: false, mensaje: 'El teléfono es obligatorio para comercios' };
  const digits = soloDigitos(raw);
  if (digits.length < 8) {
    return { ok: false, mensaje: 'El teléfono debe tener al menos 8 dígitos (ej: 3434567890)' };
  }
  if (digits.length > 15) {
    return { ok: false, mensaje: 'El teléfono es demasiado largo' };
  }
  return { ok: true, normalizado: digits };
}

module.exports = { soloDigitos, validarTelefonoComercio };
