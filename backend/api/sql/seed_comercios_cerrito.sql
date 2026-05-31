-- Seed Cerrito (Entre Ríos) — SOLO REFERENCIA
-- Los inserts con password_hash deben generarse con bcrypt.
-- Ejecutá el script Node (recomendado):
--   cd backend/api && node scripts/seedComerciosCerrito.js
--
-- Credenciales tras el seed:
--   cafe@martinez.com / donjuan@pizzeria.com / farmacia@cerrito.com /
--   moda@urbana.com / ahorro@super.com  →  contraseña: comercio123

-- Verificación manual:
SELECT u.email, c.nombre, c.direccion, c.lat, c.lon, c.icono_emoji, c.realiza_envios
FROM piku_usuarios u
JOIN piku_comercios c ON c.id = u.comercio_id
WHERE u.email IN (
  'cafe@martinez.com',
  'donjuan@pizzeria.com',
  'farmacia@cerrito.com',
  'moda@urbana.com',
  'ahorro@super.com'
);
