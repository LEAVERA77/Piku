-- Comercios de prueba en Cerrito, Entre Ríos (referencia).
-- Preferir: cd backend/api && npm run seed:cerrito
-- (crea usuarios, ofertas y reglas de puntos).

SELECT id, nombre, lat, lon, tipo_comercio, icono_emoji
FROM piku_comercios
WHERE lat BETWEEN -31.921 AND -31.916
   OR direccion ILIKE '%Cerrito%';

-- Si la consulta anterior está vacía, ejecutar el seed Node o insertar manualmente
-- con los mismos datos que scripts/seedComerciosCerrito.js
