const rateLimit = require('express-rate-limit');

const mensajeLimite = { error: 'Demasiadas solicitudes. Intentá de nuevo en un minuto.' };

/**
 * Lecturas de notificaciones del comercio (60 req/min por IP).
 */
const comercioNotificacionesLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: parseInt(process.env.RATE_LIMIT_NOTIFICACIONES_MAX, 10) || 60,
  standardHeaders: true,
  legacyHeaders: false,
  message: mensajeLimite,
  keyGenerator: (req) => req.user?.id || req.ip,
});

/**
 * Historial de canjes del comercio (40 req/min por usuario).
 */
const comercioCanjesLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: parseInt(process.env.RATE_LIMIT_CANJES_MAX, 10) || 40,
  standardHeaders: true,
  legacyHeaders: false,
  message: mensajeLimite,
  keyGenerator: (req) => req.user?.id || req.ip,
});

module.exports = {
  comercioNotificacionesLimiter,
  comercioCanjesLimiter,
};
