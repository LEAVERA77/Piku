const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const { uploadImage, configurado } = require('../services/cloudinary.service');

const UPLOAD_ROOT = path.join(__dirname, '..', 'uploads');

function ensureDir(folder) {
  const dir = path.join(UPLOAD_ROOT, folder);
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

function publicBaseUrl() {
  const explicit = process.env.API_PUBLIC_URL || process.env.RENDER_EXTERNAL_URL;
  if (explicit) return String(explicit).replace(/\/$/, '');
  return '';
}

/**
 * Sube imagen a Cloudinary si está configurado; si falla o no hay credenciales, guarda en disco local.
 */
async function subirImagenBuffer(buffer, mimetype = 'image/jpeg', folder = 'misc') {
  if (configurado) {
    try {
      const dataUri = `data:${mimetype};base64,${buffer.toString('base64')}`;
      const { url } = await uploadImage(dataUri, folder);
      return url;
    } catch (err) {
      console.warn('Cloudinary falló, usando almacenamiento local:', err.message);
    }
  }

  const ext = mimetype.includes('png') ? 'png' : mimetype.includes('webp') ? 'webp' : 'jpg';
  const name = `${Date.now()}_${crypto.randomBytes(8).toString('hex')}.${ext}`;
  ensureDir(folder);
  fs.writeFileSync(path.join(UPLOAD_ROOT, folder, name), buffer);

  const base = publicBaseUrl();
  if (base) return `${base}/uploads/${folder}/${name}`;
  return `/uploads/${folder}/${name}`;
}

module.exports = { subirImagenBuffer, UPLOAD_ROOT };
