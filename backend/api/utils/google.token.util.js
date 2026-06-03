const { OAuth2Client } = require('google-auth-library');
const { sanitizarInput } = require('./helpers');

const GOOGLE_AUDIENCES = [
  process.env.GOOGLE_CLIENT_ID,
  process.env.GOOGLE_IOS_CLIENT_ID,
  process.env.GOOGLE_ANDROID_CLIENT_ID,
].filter(Boolean);

const oauthClient = new OAuth2Client();

/**
 * Verifica idToken de Google localmente (más rápido que tokeninfo HTTP).
 */
async function verificarIdTokenGoogle(idToken) {
  if (!idToken) {
    const err = new Error('Token de Google requerido');
    err.status = 401;
    throw err;
  }

  let ticket;
  try {
    ticket = await oauthClient.verifyIdToken({
      idToken,
      audience: GOOGLE_AUDIENCES.length ? GOOGLE_AUDIENCES : undefined,
    });
  } catch (e) {
    const err = new Error('Token de Google no válido o expirado');
    err.status = 401;
    throw err;
  }

  const payload = ticket.getPayload();
  if (!payload) {
    const err = new Error('No se pudo verificar la cuenta de Google');
    err.status = 401;
    throw err;
  }

  if (GOOGLE_AUDIENCES.length && payload.aud && !GOOGLE_AUDIENCES.includes(payload.aud)) {
    const err = new Error('Token de Google no válido para esta app');
    err.status = 401;
    throw err;
  }

  const email = String(payload.email || '').toLowerCase();
  const googleId = String(payload.sub || '');
  const nombre = sanitizarInput(payload.name || email.split('@')[0], 255);

  if (!email || !googleId) {
    const err = new Error('No se pudo verificar la cuenta de Google');
    err.status = 401;
    throw err;
  }

  return { email, googleId, nombre, picture: payload.picture || null };
}

module.exports = { verificarIdTokenGoogle, GOOGLE_AUDIENCES };
