/**
 * Seed: 5 comercios de prueba en Cerrito, Entre Ríos.
 * Uso: cd backend/api && node scripts/seedComerciosCerrito.js
 * Requiere DATABASE_URL en .env o entorno.
 */
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

const bcrypt = require('bcryptjs');
const { pool, query } = require('../services/neon.service');
const { normalizarTipoComercio } = require('../constants/tipos_comercio');

const PASSWORD = 'comercio123';
const TELEFONO = '3434540250';
const CIUDAD = 'Cerrito';
const PROVINCIA = 'Entre Ríos';

const COMERCIOS = [
  {
    email: 'cafe@martinez.com',
    nombreDueno: 'Dueño Café Martínez',
    nombre: 'Café Martínez',
    direccion: 'Belgrano 345, Cerrito, Entre Ríos',
    lat: -31.9189,
    lon: -60.6085,
    tipoComercio: 'cafeteria',
    realiza_envios: true,
    costo_envio: 500,
    envio_minimo_compra: 8000,
    ofertas: [
      { nombre: 'Café con 2 medialunas', puntos: 300, tipo: 'producto_gratis', dias: 30, icono: '☕' },
      { nombre: '15% OFF en desayunos', puntos: 200, tipo: 'descuento', dias: 15, icono: '☕', porcentaje: 15 },
      { nombre: '2x1 en cafés', puntos: 250, tipo: '2x1', dias: 7, icono: '☕' },
    ],
  },
  {
    email: 'donjuan@pizzeria.com',
    nombreDueno: 'Dueño Pizzería Don Juan',
    nombre: 'Pizzería Don Juan',
    direccion: 'San Martín 890, Cerrito, Entre Ríos',
    lat: -31.9175,
    lon: -60.6078,
    tipoComercio: 'restaurante',
    realiza_envios: true,
    costo_envio: 800,
    envio_minimo_compra: 12000,
    ofertas: [
      { nombre: 'Pizza individual gratis', puntos: 500, tipo: 'producto_gratis', dias: 30, icono: '🍕' },
      { nombre: '20% OFF en pizzas grandes', puntos: 400, tipo: 'descuento', dias: 15, icono: '🍕', porcentaje: 20 },
      { nombre: '2x1 en porciones', puntos: 200, tipo: '2x1', dias: 7, icono: '🍕' },
    ],
  },
  {
    email: 'farmacia@cerrito.com',
    nombreDueno: 'Dueño Farmacia Cerrito',
    nombre: 'Farmacia Cerrito',
    direccion: '25 de Mayo 123, Cerrito, Entre Ríos',
    lat: -31.9202,
    lon: -60.6092,
    tipoComercio: 'farmacia',
    realiza_envios: true,
    costo_envio: 300,
    envio_minimo_compra: 5000,
    ofertas: [
      { nombre: '10% OFF en medicamentos', puntos: 300, tipo: 'descuento', dias: 30, icono: '💊', porcentaje: 10 },
      { nombre: 'Producto de regalo', puntos: 600, tipo: 'producto_gratis', dias: 60, icono: '💊' },
    ],
  },
  {
    email: 'moda@urbana.com',
    nombreDueno: 'Dueño Moda Urbana',
    nombre: 'Moda Urbana',
    direccion: 'Rivadavia 567, Cerrito, Entre Ríos',
    lat: -31.9168,
    lon: -60.6065,
    tipoComercio: 'ropa',
    realiza_envios: true,
    costo_envio: 700,
    envio_minimo_compra: 15000,
    ofertas: [
      { nombre: '15% OFF en toda la tienda', puntos: 500, tipo: 'descuento', dias: 15, icono: '👕', porcentaje: 15 },
      { nombre: 'Envío gratis', puntos: 200, tipo: 'envio_gratis', dias: 7, icono: '👕' },
    ],
  },
  {
    email: 'ahorro@super.com',
    nombreDueno: 'Dueño Supermercado El Ahorro',
    nombre: 'Supermercado El Ahorro',
    direccion: 'Urquiza 789, Cerrito, Entre Ríos',
    lat: -31.9195,
    lon: -60.6101,
    tipoComercio: 'supermercado',
    realiza_envios: true,
    costo_envio: 1000,
    envio_minimo_compra: 20000,
    ofertas: [
      { nombre: '10% OFF en compras', puntos: 400, tipo: 'descuento', dias: 30, icono: '🛒', porcentaje: 10 },
      { nombre: 'Producto sorpresa', puntos: 800, tipo: 'producto_gratis', dias: 30, icono: '🛒' },
      { nombre: '2x1 en productos seleccionados', puntos: 300, tipo: '2x1', dias: 15, icono: '🛒' },
    ],
  },
];

function fechaFinDesdeDias(dias) {
  return new Date(Date.now() + dias * 24 * 60 * 60 * 1000).toISOString();
}

async function upsertComercio(hash, data) {
  const tipoInfo = normalizarTipoComercio(data.tipoComercio);
  const email = data.email.toLowerCase();

  let usuarioId;
  let comercioId;

  const existente = await query(
    `SELECT u.id AS usuario_id, u.comercio_id, c.id AS comercio_pk
     FROM piku_usuarios u
     LEFT JOIN piku_comercios c ON c.id = u.comercio_id OR c.usuario_id = u.id
     WHERE LOWER(u.email) = $1`,
    [email]
  );

  if (existente.rows.length) {
    usuarioId = existente.rows[0].usuario_id;
    comercioId = existente.rows[0].comercio_id || existente.rows[0].comercio_pk;
    console.log(`↪ Ya existe: ${data.nombre} (${email})`);
  } else {
    const comercioInsert = await query(
      `INSERT INTO piku_comercios (
         nombre, direccion, lat, lon, radio_metros, activo, suscripcion_activa,
         categoria, tipo_comercio, icono_emoji,
         realiza_envios, costo_envio, envio_minimo_compra, telefono_contacto
       ) VALUES ($1,$2,$3,$4,100,TRUE,TRUE,$5,$6,$7,$8,$9,$10,$11)
       RETURNING id`,
      [
        data.nombre,
        data.direccion,
        data.lat,
        data.lon,
        tipoInfo.categoria,
        tipoInfo.id,
        tipoInfo.emoji,
        data.realiza_envios,
        data.costo_envio,
        data.envio_minimo_compra,
        TELEFONO,
      ]
    );
    comercioId = comercioInsert.rows[0].id;

    const usuarioInsert = await query(
      `INSERT INTO piku_usuarios (
         email, password_hash, nombre, telefono, rol, comercio_id, ciudad, provincia, direccion, activo
       ) VALUES ($1,$2,$3,$4,'comercio',$5,$6,$7,$8,TRUE)
       RETURNING id`,
      [
        email,
        hash,
        data.nombreDueno,
        TELEFONO,
        comercioId,
        CIUDAD,
        PROVINCIA,
        data.direccion,
      ]
    );
    usuarioId = usuarioInsert.rows[0].id;

    await query('UPDATE piku_comercios SET usuario_id = $1, updated_at = NOW() WHERE id = $2', [
      usuarioId,
      comercioId,
    ]);

    console.log(`✅ Creado: ${data.nombre}`);
  }

  if (!comercioId) {
    throw new Error(`Sin comercio_id para ${email}`);
  }

  await query(
    `INSERT INTO piku_reglas_puntos (comercio_id, puntos_por_peso, monto_minimo, puntos_fijos, max_puntos_por_dia, activo)
     VALUES ($1, 1, 0, 10, 500, TRUE)
     ON CONFLICT (comercio_id) DO NOTHING`,
    [comercioId]
  );

  for (const oferta of data.ofertas) {
    const dup = await query(
      'SELECT id FROM piku_recompensas WHERE comercio_id = $1 AND nombre = $2',
      [comercioId, oferta.nombre]
    );
    if (dup.rows.length) continue;

    const fechaFin = fechaFinDesdeDias(oferta.dias);
    await query(
      `INSERT INTO piku_recompensas (
         comercio_id, nombre, descripcion, puntos_requeridos, icono, tipo,
         porcentaje_descuento, fecha_inicio, fecha_fin, max_usos_por_usuario, max_usos_totales, activo
       ) VALUES ($1,$2,$3,$4,$5,$6,$7,NOW(),$8,1,0,TRUE)`,
      [
        comercioId,
        oferta.nombre,
        `Oferta de prueba — ${data.nombre}, Cerrito`,
        oferta.puntos,
        oferta.icono,
        oferta.tipo,
        oferta.porcentaje || null,
        fechaFin,
      ]
    );
  }

  return { comercioId, email };
}

async function main() {
  if (!pool) {
    console.error('❌ DATABASE_URL no configurada');
    process.exit(1);
  }

  const hash = await bcrypt.hash(PASSWORD, 10);
  console.log('🌱 Seed comercios Cerrito, Entre Ríos...\n');

  const creados = [];
  for (const c of COMERCIOS) {
    try {
      const r = await upsertComercio(hash, c);
      creados.push(r);
    } catch (err) {
      console.error(`❌ ${c.nombre}:`, err.message);
    }
  }

  const listado = await query(
    `SELECT u.email, c.nombre, c.direccion, c.lat, c.lon, c.icono_emoji,
            (SELECT COUNT(*)::int FROM piku_recompensas r WHERE r.comercio_id = c.id AND r.activo) AS ofertas
     FROM piku_usuarios u
     JOIN piku_comercios c ON c.id = u.comercio_id
     WHERE u.email = ANY($1::text[])`,
    [COMERCIOS.map((x) => x.email.toLowerCase())]
  );

  console.log('\n📋 Resumen:');
  for (const row of listado.rows) {
    console.log(
      `  ${row.icono_emoji} ${row.nombre} — ${row.email} — ${row.ofertas} ofertas — (${row.lat}, ${row.lon})`
    );
  }

  console.log(`\n🔑 Contraseña para todos: ${PASSWORD}`);
  await pool.end();
  process.exit(0);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
