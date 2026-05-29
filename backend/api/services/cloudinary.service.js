const cloudinary = require('cloudinary').v2;

const configurado =
  process.env.CLOUDINARY_CLOUD_NAME &&
  process.env.CLOUDINARY_API_KEY &&
  process.env.CLOUDINARY_API_SECRET;

if (configurado) {
  cloudinary.config({
    cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
    api_key: process.env.CLOUDINARY_API_KEY,
    api_secret: process.env.CLOUDINARY_API_SECRET,
  });
}

/**
 * Sube una imagen (buffer o ruta) a Cloudinary.
 */
async function uploadImage(file, folder = 'piku') {
  if (!configurado) {
    throw new Error('Cloudinary no está configurado en .env');
  }

  const resultado = await cloudinary.uploader.upload(file, {
    folder: `piku/${folder}`,
    resource_type: 'image',
  });

  return {
    url: resultado.secure_url,
    publicId: resultado.public_id,
  };
}

/**
 * Elimina una imagen por public_id.
 */
async function deleteImage(publicId) {
  if (!configurado) {
    throw new Error('Cloudinary no está configurado en .env');
  }
  return cloudinary.uploader.destroy(publicId);
}

module.exports = { uploadImage, deleteImage, configurado };
