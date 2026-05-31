const { geocodeAddress } = require('../services/nominatim.service');
const { sanitizarInput } = require('./helpers');
const { tiene } = require('./schema.util');

/**
 * Valida campos de dirección y obtiene coordenadas (Nominatim o pin del mapa).
 */
async function resolverDireccionRegistro(body, opts = {}) {
  const calle = sanitizarInput(body.calle, 200);
  const numero = sanitizarInput(body.numero, 20);
  const ciudad = sanitizarInput(body.ciudad, 100);
  const provincia = sanitizarInput(body.provincia, 100);
  const codigoPostal = sanitizarInput(body.codigoPostal || body.codigo_postal, 20);

  if (!calle || !numero || !ciudad || !provincia) {
    const err = new Error('Calle, número, ciudad y provincia son obligatorios');
    err.status = 400;
    throw err;
  }

  const direccion = `${calle} ${numero}, ${ciudad}, ${provincia}, Argentina`;
  let lat = opts.lat != null ? parseFloat(opts.lat) : parseFloat(body.lat);
  let lon = opts.lon != null ? parseFloat(opts.lon) : parseFloat(body.lon);

  const pinValido = Number.isFinite(lat) && Number.isFinite(lon);
  if (!pinValido) {
    const geo = await geocodeAddress(direccion);
    lat = geo.lat;
    lon = geo.lon;
  }

  return {
    direccion,
    ciudad,
    provincia,
    codigo_postal: codigoPostal || null,
    lat,
    lon,
  };
}

function agregarCamposDireccionUsuario(colsUsuario, campos, vals, data) {
  const add = (col, val) => {
    if (tiene(colsUsuario, col)) {
      campos.push(col);
      vals.push(val);
    }
  };
  add('direccion', data.direccion);
  add('direccion_entrega', data.direccion);
  add('ciudad', data.ciudad);
  add('provincia', data.provincia);
  add('codigo_postal', data.codigo_postal);
  add('lat', data.lat);
  add('lon', data.lon);
}

module.exports = { resolverDireccionRegistro, agregarCamposDireccionUsuario };
