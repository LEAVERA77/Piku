#!/usr/bin/env node
/**
 * Prueba Cloudinary y subida de imágenes (unit + API comercio).
 * Uso:
 *   node scripts/testCloudinary.js
 *   node scripts/testCloudinary.js --api https://piku-324e.onrender.com farmacia@cerrito.com comercio123
 */
require('dotenv').config();

const fs = require('fs');
const path = require('path');
const { configurado, uploadImage, deleteImage } = require('../services/cloudinary.service');
const { subirImagenBuffer } = require('../utils/uploadImagen.util');

/** JPEG 1×1 px válido */
const JPEG_1X1 = Buffer.from(
  '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAn/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCwAA8A/9k=',
  'base64'
);

function esUrlCloudinary(url) {
  return typeof url === 'string' && url.includes('res.cloudinary.com');
}

function esUrlLocal(url) {
  return typeof url === 'string' && (url.includes('/uploads/') || url.startsWith('/uploads'));
}

async function testServicioDirecto() {
  console.log('\n── 1) Servicio Cloudinary directo ──');
  console.log('Configurado:', configurado ? 'SÍ' : 'NO');
  if (!configurado) {
    console.log('⚠️  Faltan CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY o CLOUDINARY_API_SECRET en .env');
    return { ok: false, motivo: 'sin_credenciales' };
  }

  const dataUri = `data:image/jpeg;base64,${JPEG_1X1.toString('base64')}`;
  let publicId;
  try {
    const { url, publicId: pid } = await uploadImage(dataUri, 'test-piku');
    publicId = pid;
    console.log('✅ Upload OK');
    console.log('   URL:', url);
    console.log('   public_id:', publicId);

    const probe = await fetch(url, { method: 'HEAD' });
    console.log('   Acceso HTTP:', probe.status, probe.ok ? '(OK)' : '(falló)');

    if (publicId) {
      await deleteImage(publicId);
      console.log('✅ Imagen de prueba eliminada de Cloudinary');
    }
    return { ok: true, url };
  } catch (err) {
    console.error('❌ Error Cloudinary:', err.message);
    return { ok: false, motivo: err.message };
  }
}

async function testSubirImagenUtil() {
  console.log('\n── 2) Util subirImagenBuffer (flujo del backend) ──');
  try {
    const url = await subirImagenBuffer(JPEG_1X1, 'image/jpeg', 'test-piku');
    const viaCloudinary = esUrlCloudinary(url);
    const viaLocal = esUrlLocal(url);
    console.log('URL resultante:', url);
    console.log('Destino:', viaCloudinary ? 'Cloudinary ✅' : viaLocal ? 'Disco local (fallback)' : 'otro');

    if (viaLocal) {
      const localPath = url.replace(/^https?:\/\/[^/]+/, '').replace(/^\//, '');
      const full = path.join(__dirname, '..', localPath.replace(/^uploads\//, 'uploads/'));
      if (fs.existsSync(full)) {
        fs.unlinkSync(full);
        console.log('✅ Archivo local de prueba eliminado');
      }
    }
    return { ok: true, viaCloudinary, viaLocal, url };
  } catch (err) {
    console.error('❌ Error subirImagenBuffer:', err.message);
    return { ok: false, motivo: err.message };
  }
}

async function testApiComercio(baseUrl, email, password) {
  console.log('\n── 3) API comercio (logo + foto oferta) ──');
  console.log('API:', baseUrl);
  console.log('Usuario:', email);

  const loginRes = await fetch(`${baseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  const loginBody = await loginRes.json().catch(() => ({}));
  if (!loginRes.ok) {
    console.error('❌ Login falló:', loginRes.status, loginBody.error || loginBody);
    return { ok: false, motivo: 'login' };
  }
  const token = loginBody.token;
  console.log('✅ Login OK — rol:', loginBody.usuario?.rol);

  const authHeaders = { Authorization: `Bearer ${token}` };

  const blob = new Blob([JPEG_1X1], { type: 'image/jpeg' });

  const logoForm = new FormData();
  logoForm.append('file', blob, 'test-logo.jpg');

  const logoRes = await fetch(`${baseUrl}/api/comercio/upload-logo`, {
    method: 'POST',
    headers: authHeaders,
    body: logoForm,
  });
  const logoBody = await logoRes.json().catch(() => ({}));
  console.log('\nLogo POST:', logoRes.status);
  if (logoRes.ok) {
    const logoUrl = logoBody.logo_url || logoBody.comercio?.logo_url;
    console.log('   logo_url:', logoUrl);
    console.log('   Cloudinary:', esUrlCloudinary(logoUrl) ? 'SÍ ✅' : esUrlLocal(logoUrl) ? 'local/fallback' : 'otro');
  } else {
    console.log('   error:', logoBody.error, logoBody.detail || '');
  }

  // Recompensa — imagen portada
  const recRes = await fetch(`${baseUrl}/api/comercio/recompensas`, { headers: authHeaders });
  const recBody = await recRes.json().catch(() => ({}));
  const recompensas = recBody.recompensas || [];
  if (!recompensas.length) {
    console.log('\n⚠️  Sin recompensas para probar foto de producto');
    return { ok: logoRes.ok, logo: logoBody, viaCloudinary: esUrlCloudinary(logoBody.logo_url) };
  }

  const recId = recompensas[0].id;
  console.log('\nOferta de prueba:', recompensas[0].nombre, `(${recId})`);

  const imgForm = new FormData();
  imgForm.append('file', blob, 'test-oferta.jpg');

  const imgRes = await fetch(`${baseUrl}/api/comercio/recompensas/${recId}/imagen`, {
    method: 'POST',
    headers: authHeaders,
    body: imgForm,
  });
  const imgBody = await imgRes.json().catch(() => ({}));
  console.log('Imagen oferta POST:', imgRes.status);
  if (imgRes.ok) {
    const imgUrl = imgBody.imagen_url || imgBody.recompensa?.imagen_url;
    console.log('   imagen_url:', imgUrl);
    console.log('   Cloudinary:', esUrlCloudinary(imgUrl) ? 'SÍ ✅' : esUrlLocal(imgUrl) ? 'local/fallback' : 'otro');
  } else {
    console.log('   error:', imgBody.error, imgBody.detail || '');
  }

  const logoUrl = logoBody.logo_url || logoBody.comercio?.logo_url;
  const imgUrl = imgBody.imagen_url || imgBody.recompensa?.imagen_url;
  return {
    ok: logoRes.ok && imgRes.ok,
    logoCloudinary: esUrlCloudinary(logoUrl),
    ofertaCloudinary: esUrlCloudinary(imgUrl),
    logoUrl,
    imgUrl,
  };
}

async function main() {
  console.log('🧪 Test Cloudinary — Piku');
  console.log('CLOUDINARY_CLOUD_NAME:', process.env.CLOUDINARY_CLOUD_NAME ? '(definido)' : '(ausente)');

  const r1 = await testServicioDirecto();
  const r2 = await testSubirImagenUtil();

  const args = process.argv.slice(2);
  const apiIdx = args.indexOf('--api');
  let r3 = null;
  if (apiIdx >= 0 && args[apiIdx + 1]) {
    const base = args[apiIdx + 1].replace(/\/$/, '');
    const email = args[apiIdx + 2] || 'farmacia@cerrito.com';
    const pass = args[apiIdx + 3] || 'comercio123';
    r3 = await testApiComercio(base, email, pass);
  }

  console.log('\n── Resumen ──');
  console.log('Cloudinary directo:', r1.ok ? '✅' : '❌', r1.motivo || '');
  console.log('subirImagenBuffer:', r2.ok ? '✅' : '❌', r2.viaCloudinary ? '(Cloudinary)' : r2.viaLocal ? '(local)' : '');
  if (r3) {
    console.log('API producción:', r3.ok ? '✅' : '❌');
    console.log('  Logo en Cloudinary:', r3.logoCloudinary ? 'SÍ' : 'NO');
    console.log('  Oferta en Cloudinary:', r3.ofertaCloudinary ? 'SÍ' : 'NO');
  } else {
    console.log('API producción: omitida (usá --api URL email password)');
  }

  const exitOk = r2.ok && (r3 ? r3.ok : true);
  process.exit(exitOk ? 0 : 1);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
