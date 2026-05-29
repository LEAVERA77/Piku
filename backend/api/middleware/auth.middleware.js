const jwt = require('jsonwebtoken');
const { query } = require('../services/neon.service');

const JWT_SECRET = process.env.JWT_SECRET || 'dev_secret_cambiar';
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '30d';

/**
 * Genera un JWT con datos del usuario.
 */
function signToken(payload) {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });
}

/**
 * Verifica Bearer token y carga req.user desde la BD.
 */
async function authMiddleware(req, res, next) {
  try {
    const header = req.headers.authorization || '';
    const [, token] = header.split(' ');
    if (!token) {
      return res.status(401).json({ error: 'Token requerido' });
    }

    const decoded = jwt.verify(token, JWT_SECRET);
    const userId = decoded.userId || decoded.sub;
    if (!userId) {
      return res.status(401).json({ error: 'Token inválido' });
    }

    const result = await query(
      `SELECT id, email, nombre, telefono, rol, avatar_url, activo,
              puntos_saldo, comercio_id, created_at, updated_at
       FROM usuarios WHERE id = $1 LIMIT 1`,
      [userId]
    );

    if (!result.rows.length || !result.rows[0].activo) {
      return res.status(401).json({ error: 'Usuario inválido o inactivo' });
    }

    req.user = result.rows[0];
    next();
  } catch (error) {
    return res.status(401).json({ error: 'Token inválido', detail: error.message });
  }
}

module.exports = { authMiddleware, signToken, JWT_SECRET };
