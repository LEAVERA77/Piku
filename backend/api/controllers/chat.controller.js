const { query } = require('../services/neon.service');
const { completarChat } = require('../services/gestorNova.service');
const { registrarEvento, resumenParaIA } = require('../services/eventos.service');
const { normalizarCategoria } = require('../constants/rubros');
const { responderError } = require('../utils/helpers');

function distanciaMetros(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const toRad = (d) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return Math.round(R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
}

async function comerciosCercanos(lat, lon, limite = 12) {
  const result = await query(
    `SELECT c.id, c.nombre, c.categoria, c.lat, c.lon, c.direccion,
            (SELECT MIN(r.puntos_requeridos)
             FROM piku_recompensas r
             WHERE r.comercio_id = c.id AND r.activo = TRUE
               AND (r.stock IS NULL OR r.stock > 0)) AS canje_puntos
     FROM piku_comercios c
     WHERE COALESCE(c.suscripcion_activa, TRUE) = TRUE
       AND c.lat IS NOT NULL AND c.lon IS NOT NULL
     ORDER BY c.nombre ASC
     LIMIT 80`
  );

  const lista = result.rows
    .map((c) => ({
      id: c.id,
      nombre: c.nombre,
      rubro: normalizarCategoria(c.categoria),
      distancia:
        lat != null && lon != null ? distanciaMetros(lat, lon, c.lat, c.lon) : null,
      canje_puntos: c.canje_puntos,
      direccion: c.direccion,
    }))
    .sort((a, b) => (a.distancia ?? 999999) - (b.distancia ?? 999999))
    .slice(0, limite);

  return lista;
}

function extraerComercioSugerido(texto, comercios) {
  if (!texto || !comercios?.length) return null;
  const lower = texto.toLowerCase();
  const match = comercios.find((c) => lower.includes(c.nombre.toLowerCase()));
  return match?.id || null;
}

/**
 * POST /api/chat-piku — asistente con contexto del usuario (Groq vía GestorNova o fallback).
 */
async function chatPiku(req, res) {
  try {
    const pregunta = String(req.body.pregunta || '').trim();
    if (!pregunta) return responderError(res, 400, 'pregunta requerida');

    const lat = parseFloat(req.body.lat ?? req.body.ubicacion?.lat);
    const lon = parseFloat(req.body.lon ?? req.body.ubicacion?.lon);
    const tieneUbicacion = Number.isFinite(lat) && Number.isFinite(lon);

    const puntos = req.user.puntos_saldo ?? 0;
    const resumen = await resumenParaIA(req.user.id);
    const cercanos = await comerciosCercanos(tieneUbicacion ? lat : null, tieneUbicacion ? lon : null);

    const contexto = {
      puntos,
      ubicacion: tieneUbicacion ? { lat, lon } : null,
      historial: resumen.ultimosCanjes,
      rubros_frecuentes: resumen.rubrosFrecuentes,
      comercios_visitados: resumen.comerciosVisitados,
      comercios_cercanos: cercanos.map((c) => ({
        id: c.id,
        nombre: c.nombre,
        rubro: c.rubro,
        distancia: c.distancia,
        canje_puntos: c.canje_puntos,
      })),
      pregunta,
    };

    const system = [
      'Sos Piku, asistente amigable de una app de puntos y descuentos en Argentina.',
      'Respondé en español rioplatense, breve (máximo 6 oraciones).',
      'Usá los datos del JSON de contexto para personalizar (puntos, canjes previos, comercios cercanos).',
      'Si recomendás un comercio, mencioná su nombre exacto como figura en comercios_cercanos.',
      'Al final, en una línea aparte, escribí exactamente: SUGERENCIA_ID:<uuid del comercio> o SUGERENCIA_ID:ninguno',
    ].join('\n');

    const ia = await completarChat({
      system,
      user: JSON.stringify(contexto, null, 2),
    });

    if (!ia.ok) return responderError(res, 503, ia.error);

    let respuesta = ia.texto.trim();
    let comercioSugeridoId = null;
    const matchId = respuesta.match(/SUGERENCIA_ID:\s*([a-f0-9-]{36}|ninguno)/i);
    if (matchId) {
      const id = matchId[1];
      if (id !== 'ninguno') comercioSugeridoId = id;
      respuesta = respuesta.replace(/SUGERENCIA_ID:\s*[^\n]+/gi, '').trim();
    }
    if (!comercioSugeridoId) {
      comercioSugeridoId = extraerComercioSugerido(respuesta, cercanos);
    }

    try {
      await registrarEvento(req.user.id, 'chat_piku', comercioSugeridoId, {
        pregunta: pregunta.slice(0, 200),
      });
    } catch (e) {
      console.warn('evento chat_piku:', e.message);
    }

    return res.json({
      respuesta,
      comercio_sugerido_id: comercioSugeridoId,
      via: ia.via,
    });
  } catch (error) {
    console.error('chatPiku:', error);
    return responderError(res, 500, 'Error en asistente Piku', { detail: error.message });
  }
}

module.exports = { chatPiku, comerciosCercanos };
