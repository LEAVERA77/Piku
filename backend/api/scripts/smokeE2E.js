/**
 * Smoke test end-to-end de Piku contra una API corriendo (local o Render).
 *
 * Ejercita el flujo completo de ambos roles:
 *  - Comerciante: registro, login, reglas de puntos (multiplicador + fijos),
 *    catálogo, generar QR, estadísticas, canjes, notificaciones, pausa del programa.
 *  - Cliente: registro (bono bienvenida), saldo, escaneo de QR con GPS,
 *    anti-reuso de QR, anti-fraude por distancia, canje y historial.
 *
 * Uso: node scripts/smokeE2E.js  (requiere PIKU_API_URL o API local en :3000)
 */

const BASE = process.env.PIKU_API_URL || 'http://localhost:3000';

const LAT_COMERCIO = -31.5833;
const LON_COMERCIO = -60.0667;

let pasos = 0;
let fallos = 0;

function ok(nombre, condicion, detalle = '') {
  pasos += 1;
  if (condicion) {
    console.log(`  ✅ ${nombre}`);
  } else {
    fallos += 1;
    console.error(`  ❌ ${nombre} ${detalle}`);
  }
}

async function api(metodo, ruta, { token, body } = {}) {
  const res = await fetch(`${BASE}${ruta}`, {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  let json = null;
  try {
    json = await res.json();
  } catch (_e) {
    // sin body JSON
  }
  return { status: res.status, json };
}

async function main() {
  const sufijo = Date.now();
  console.log(`\n🧪 Smoke E2E Piku → ${BASE}\n`);

  // ───────────────────────── Salud y datos públicos
  console.log('— Sistema');
  const health = await api('GET', '/health');
  ok('GET /health responde 200', health.status === 200);

  const cotiz = await api('GET', '/api/public/cotizacion');
  ok('GET /api/public/cotizacion responde 200', cotiz.status === 200, JSON.stringify(cotiz.json));
  const pesosPorDolar = parseFloat(
    cotiz.json?.pesosPorDolar ?? cotiz.json?.cotizacion?.pesosPorDolar
  );
  ok('Cotización USD válida', Number.isFinite(pesosPorDolar) && pesosPorDolar > 0, String(pesosPorDolar));

  // ───────────────────────── Rol comerciante
  console.log('\n— Comerciante: registro y configuración');
  const emailComercio = `comercio${sufijo}@test.piku`;
  const regCom = await api('POST', '/api/auth/registro-comercio', {
    body: {
      email: emailComercio,
      password: 'secreta123',
      nombre: 'Tester Comercio',
      nombreComercio: `Café Smoke ${sufijo}`,
      codigoInvitacion: process.env.PIKU_CODIGO_INVITACION || 'PIKU2025',
      telefono: '+54 9 343 5550000',
      tipoComercio: 'cafeteria',
      calle: 'Belgrano',
      numero: '345',
      ciudad: 'Cerrito',
      provincia: 'Entre Ríos',
      lat: LAT_COMERCIO,
      lon: LON_COMERCIO,
    },
  });
  ok('Registro comercio 201', regCom.status === 201, JSON.stringify(regCom.json));
  const tokenCom = regCom.json?.token;
  ok('Registro comercio devuelve token', Boolean(tokenCom));

  const login = await api('POST', '/api/auth/login', {
    body: { email: emailComercio, password: 'secreta123' },
  });
  ok('Login comercio 200 con rol comercio', login.status === 200 && login.json?.usuario?.rol === 'comercio');

  // Reglas de puntos: multiplicador x2 + 5 puntos fijos + mínimo de compra
  const reglasPut = await api('PUT', '/api/comercio/reglas', {
    token: tokenCom,
    body: { montoMinimo: 1000, puntosFijos: 5, maxPuntosPorDia: 500, activo: true, puntosPorPeso: 2 },
  });
  ok('PUT reglas 200', reglasPut.status === 200, JSON.stringify(reglasPut.json));
  ok(
    'Reglas persisten puntos_por_peso=2 (multiplicador editable)',
    parseFloat(reglasPut.json?.reglas?.puntos_por_peso) === 2,
    JSON.stringify(reglasPut.json?.reglas)
  );

  const reglasGet = await api('GET', '/api/comercio/reglas', { token: tokenCom });
  ok('GET reglas devuelve sistemaPikuPoints siempre', Boolean(reglasGet.json?.sistemaPikuPoints?.regla));
  ok('GET reglas conserva puntos_fijos=5', parseInt(reglasGet.json?.reglas?.puntos_fijos, 10) === 5);

  // Catálogo: crear oferta canjeable
  const oferta = await api('POST', '/api/comercio/recompensas', {
    token: tokenCom,
    body: {
      nombre: 'Café gratis',
      descripcion: 'Un café de cortesía',
      puntosRequeridos: 10,
      tipo: 'producto_gratis',
      productoNombre: 'Café mediano',
    },
  });
  ok('POST recompensa 201', oferta.status === 201, JSON.stringify(oferta.json));
  const ofertaId = oferta.json?.recompensa?.id;
  ok('Recompensa con id', Boolean(ofertaId));

  // Generar QR por una compra de 10 USD equivalentes
  const monto = Math.round(pesosPorDolar * 10);
  const qr = await api('POST', '/api/comercio/generar-qr', {
    token: tokenCom,
    body: { monto },
  });
  ok('POST generar-qr 201', qr.status === 201, JSON.stringify(qr.json));
  const codigoQr = qr.json?.qr?.codigo;
  const puntosEsperados = Math.floor(Math.floor(monto / pesosPorDolar) * 2) + 5;
  ok(
    `QR calcula puntos con multiplicador y fijos (esperado ${puntosEsperados})`,
    parseInt(qr.json?.qr?.puntos_calculados, 10) === puntosEsperados,
    `calculado=${qr.json?.qr?.puntos_calculados}`
  );

  // Compra debajo del mínimo: 0 puntos
  const qrChico = await api('POST', '/api/comercio/generar-qr', {
    token: tokenCom,
    body: { monto: 500 },
  });
  ok(
    'Compra debajo del monto mínimo genera 0 puntos',
    parseInt(qrChico.json?.qr?.puntos_calculados, 10) === 0,
    `calculado=${qrChico.json?.qr?.puntos_calculados}`
  );

  // ───────────────────────── Rol cliente
  console.log('\n— Cliente: registro, escaneo y canje');
  const emailCliente = `cliente${sufijo}@test.piku`;
  const regCli = await api('POST', '/api/auth/registro-cliente', {
    body: {
      email: emailCliente,
      password: 'secreta123',
      nombre: 'Tester Cliente',
      calle: 'San Martín',
      numero: '100',
      ciudad: 'Cerrito',
      provincia: 'Entre Ríos',
      lat: LAT_COMERCIO,
      lon: LON_COMERCIO,
    },
  });
  ok('Registro cliente 201', regCli.status === 201, JSON.stringify(regCli.json));
  const tokenCli = regCli.json?.token;

  const saldo0 = await api('GET', '/api/usuario/saldo', { token: tokenCli });
  ok('GET saldo 200', saldo0.status === 200);
  const saldoInicial = parseInt(saldo0.json?.puntos, 10) || 0;
  ok('Bono de bienvenida acreditado (saldo ≥ 10)', saldoInicial >= 10, `saldo=${saldoInicial}`);

  // Escaneo del QR cerca del comercio
  const validar = await api('POST', '/api/qr/validar', {
    token: tokenCli,
    body: { codigo: codigoQr, lat: LAT_COMERCIO + 0.0002, lon: LON_COMERCIO + 0.0002 },
  });
  ok('POST /api/qr/validar 200', validar.status === 200, JSON.stringify(validar.json));
  ok(
    `Acredita ${puntosEsperados} PP según reglas del comercio`,
    parseInt(validar.json?.puntosGanados, 10) === puntosEsperados,
    `ganados=${validar.json?.puntosGanados}`
  );
  ok(
    'Saldo actualizado tras escaneo',
    parseInt(validar.json?.saldoActual, 10) === saldoInicial + puntosEsperados
  );

  // Anti-reuso del mismo QR
  const reuso = await api('POST', '/api/qr/validar', {
    token: tokenCli,
    body: { codigo: codigoQr, lat: LAT_COMERCIO, lon: LON_COMERCIO },
  });
  ok('QR reutilizado es rechazado (400)', reuso.status === 400, JSON.stringify(reuso.json));

  // Anti-fraude GPS: escaneo a ~5 km
  const qr2 = await api('POST', '/api/comercio/generar-qr', { token: tokenCom, body: { monto } });
  const lejos = await api('POST', '/api/qr/validar', {
    token: tokenCli,
    body: { codigo: qr2.json?.qr?.codigo, lat: LAT_COMERCIO + 0.05, lon: LON_COMERCIO },
  });
  ok('Escaneo lejos del comercio es rechazado (400)', lejos.status === 400, JSON.stringify(lejos.json));

  // Recompensas visibles y canje
  const recompensas = await api('GET', '/api/usuario/recompensas', { token: tokenCli });
  const lista = recompensas.json?.recompensas || [];
  ok('Cliente ve la oferta del comercio', lista.some((r) => r.id === ofertaId), `total=${lista.length}`);

  const canje = await api('POST', '/api/usuario/canjear', {
    token: tokenCli,
    body: { recompensa_id: ofertaId },
  });
  ok('POST canjear 200/201', canje.status === 200 || canje.status === 201, JSON.stringify(canje.json));
  const codigoCanje = canje.json?.codigoCanje || canje.json?.canje?.codigo_canje;
  ok('Canje devuelve código', Boolean(codigoCanje), JSON.stringify(canje.json));

  const saldoFinal = await api('GET', '/api/usuario/saldo', { token: tokenCli });
  ok(
    'Saldo debitado tras canje',
    parseInt(saldoFinal.json?.puntos, 10) === saldoInicial + puntosEsperados - 10,
    `saldo=${saldoFinal.json?.puntos}`
  );

  const historial = await api('GET', '/api/usuario/historial', { token: tokenCli });
  const transacciones = historial.json?.transacciones || historial.json?.historial || [];
  ok(
    'Historial registra ganado y canjeado',
    transacciones.some((t) => t.tipo === 'ganado') && transacciones.some((t) => t.tipo === 'canjeado'),
    `total=${transacciones.length}`
  );

  // ───────────────────────── Comerciante: vista de la operación
  console.log('\n— Comerciante: estadísticas y notificaciones');
  const stats = await api('GET', '/api/comercio/estadisticas', { token: tokenCom });
  ok('GET estadisticas 200', stats.status === 200);
  const est = stats.json?.estadisticas || {};
  ok('Estadísticas: 1 QR usado', parseInt(est.qrUsados, 10) === 1, JSON.stringify(est));
  ok('Estadísticas: puntos otorgados correctos', parseInt(est.puntosOtorgados, 10) === puntosEsperados);
  ok('Estadísticas: 1 canje realizado', parseInt(est.canjesRealizados, 10) === 1);
  ok('Estadísticas: 1 cliente único', parseInt(est.clientesUnicos, 10) === 1);

  const canjes = await api('GET', '/api/comercio/canjes', { token: tokenCom });
  const listaCanjes = canjes.json?.canjes || [];
  ok('Comercio ve el canje en su historial', listaCanjes.length === 1, `total=${listaCanjes.length}`);

  const notis = await api('GET', '/api/comercio/notificaciones', { token: tokenCom });
  const listaNotis = notis.json?.notificaciones || [];
  ok('Comercio recibió notificación del canje', listaNotis.length >= 1, `total=${listaNotis.length}`);

  // ───────────────────────── Programa pausado
  console.log('\n— Programa de puntos pausado');
  const qr3 = await api('POST', '/api/comercio/generar-qr', { token: tokenCom, body: { monto } });
  await api('PUT', '/api/comercio/reglas', {
    token: tokenCom,
    body: { montoMinimo: 1000, puntosFijos: 5, maxPuntosPorDia: 500, activo: false, puntosPorPeso: 2 },
  });
  const qrPausado = await api('POST', '/api/comercio/generar-qr', { token: tokenCom, body: { monto } });
  ok('Generar QR con programa pausado → 400', qrPausado.status === 400, JSON.stringify(qrPausado.json));
  const validarPausado = await api('POST', '/api/qr/validar', {
    token: tokenCli,
    body: { codigo: qr3.json?.qr?.codigo, lat: LAT_COMERCIO, lon: LON_COMERCIO },
  });
  ok('Validar QR con programa pausado → 400', validarPausado.status === 400, JSON.stringify(validarPausado.json));
  await api('PUT', '/api/comercio/reglas', {
    token: tokenCom,
    body: { montoMinimo: 1000, puntosFijos: 5, maxPuntosPorDia: 500, activo: true, puntosPorPeso: 2 },
  });

  // ───────────────────────── Mapa público (bbox)
  console.log('\n— Mapa público');
  const bbox = await api(
    'GET',
    `/api/public/comercios?minLat=${LAT_COMERCIO - 0.05}&maxLat=${LAT_COMERCIO + 0.05}&minLon=${LON_COMERCIO - 0.05}&maxLon=${LON_COMERCIO + 0.05}`
  );
  const comerciosBbox = bbox.json?.comercios || [];
  ok(
    'Bbox del mapa devuelve el comercio registrado',
    comerciosBbox.some((c) => c.nombre === `Café Smoke ${sufijo}`),
    `total=${comerciosBbox.length}`
  );
  const detalle = await api('GET', `/api/public/comercios/${regCom.json?.comercio?.id}/ofertas`);
  ok('Ofertas públicas del comercio incluyen la creada', (detalle.json?.ofertas || []).some((o) => o.id === ofertaId));

  // ───────────────────────── Resultado
  console.log(`\n${fallos === 0 ? '🎉' : '💥'} ${pasos - fallos}/${pasos} pasos OK${fallos ? ` — ${fallos} fallos` : ''}\n`);
  process.exit(fallos === 0 ? 0 : 1);
}

main().catch((e) => {
  console.error('💥 Error fatal del smoke test:', e);
  process.exit(1);
});
