const { query } = require('../services/neon.service');
const { uploadImage, configurado: cloudinaryOk } = require('../services/cloudinary.service');
const { sanitizarInput, responderError } = require('../utils/helpers');
const { columnasTabla, tiene } = require('../utils/schema.util');
const { validarTelefonoComercio } = require('../utils/telefono.util');
const { selectComerciosColumnas, invalidarCacheComerciosSelect } = require('../utils/comercio.sql.util');
const {
  parseFecha,
  parseIntOrNull,
  parseFloatOrNull,
  normalizarTipo,
  TIPOS_VALIDOS,
} = require('../utils/recompensa.helpers');

function getComercioId(req) {
  return req.user.comercio_id;
}

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function esUuid(valor) {
  return typeof valor === 'string' && UUID_RE.test(valor);
}

function escapeIlike(texto) {
  return String(texto).replace(/[%_\\]/g, '\\$&');
}

const ESTADOS_CANJE = new Set(['confirmado', 'pendiente', 'cancelado']);

/**
 * Obtiene reglas de puntos del comercio.
 */
async function getReglasPuntos(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const result = await query('SELECT * FROM piku_reglas_puntos WHERE comercio_id = $1', [comercioId]);
    if (!result.rows.length) {
      return res.json({
        reglas: {
          comercio_id: comercioId,
          puntos_por_peso: 1,
          monto_minimo: 0,
          puntos_fijos: 10,
          max_puntos_por_dia: 500,
          activo: true,
        },
      });
    }
    return res.json({ reglas: result.rows[0] });
  } catch (error) {
    console.error('getReglasPuntos:', error);
    return responderError(res, 500, 'Error al obtener reglas');
  }
}

/**
 * Actualiza reglas de puntos.
 */
async function updateReglasPuntos(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const puntosPorPeso = req.body.puntosPorPeso ?? req.body.puntos_por_peso ?? 1;
    const montoMinimo = req.body.montoMinimo ?? req.body.monto_minimo ?? 0;
    const puntosFijos = req.body.puntosFijos ?? req.body.puntos_fijos ?? 10;
    const maxDia = req.body.maxPuntosPorDia ?? req.body.max_puntos_por_dia ?? 500;
    const activo = req.body.activo !== false;

    const result = await query(
      `INSERT INTO piku_reglas_puntos
       (comercio_id, puntos_por_peso, monto_minimo, puntos_fijos, max_puntos_por_dia, activo, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6, NOW())
       ON CONFLICT (comercio_id) DO UPDATE SET
         puntos_por_peso = EXCLUDED.puntos_por_peso,
         monto_minimo = EXCLUDED.monto_minimo,
         puntos_fijos = EXCLUDED.puntos_fijos,
         max_puntos_por_dia = EXCLUDED.max_puntos_por_dia,
         activo = EXCLUDED.activo,
         updated_at = NOW()
       RETURNING *`,
      [comercioId, puntosPorPeso, montoMinimo, puntosFijos, maxDia, activo]
    );

    return res.json({ mensaje: 'Reglas actualizadas', reglas: result.rows[0] });
  } catch (error) {
    console.error('updateReglasPuntos:', error);
    return responderError(res, 500, 'Error al actualizar reglas');
  }
}

/**
 * Lista recompensas del comercio.
 */
async function getRecompensas(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const result = await query(
      'SELECT * FROM piku_recompensas WHERE comercio_id = $1 ORDER BY created_at DESC',
      [comercioId]
    );
    return res.json({ recompensas: result.rows });
  } catch (error) {
    console.error('getRecompensas:', error);
    return responderError(res, 500, 'Error al listar recompensas');
  }
}

/**
 * Crea una recompensa.
 */
async function getRecompensa(req, res) {
  try {
    const comercioId = getComercioId(req);
    const { id } = req.params;
    const result = await query(
      'SELECT * FROM piku_recompensas WHERE id = $1 AND comercio_id = $2',
      [id, comercioId]
    );
    if (!result.rows.length) return responderError(res, 404, 'Recompensa no encontrada');
    return res.json({ recompensa: result.rows[0] });
  } catch (error) {
    console.error('getRecompensa:', error);
    return responderError(res, 500, 'Error al obtener recompensa');
  }
}

async function getRecompensaStats(req, res) {
  try {
    const comercioId = getComercioId(req);
    const { id } = req.params;
    const existe = await query(
      'SELECT id, usos_actuales FROM piku_recompensas WHERE id = $1 AND comercio_id = $2',
      [id, comercioId]
    );
    if (!existe.rows.length) return responderError(res, 404, 'Recompensa no encontrada');

    const stats = await query(
      `SELECT COUNT(*)::int AS canjes, COUNT(DISTINCT usuario_id)::int AS usuarios_unicos
       FROM piku_canjes WHERE recompensa_id = $1`,
      [id]
    );

    return res.json({
      canjes: stats.rows[0].canjes,
      usuariosUnicos: stats.rows[0].usuarios_unicos,
      usosActuales: existe.rows[0].usos_actuales,
    });
  } catch (error) {
    console.error('getRecompensaStats:', error);
    return responderError(res, 500, 'Error al obtener estadísticas');
  }
}

async function createRecompensa(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const nombre = sanitizarInput(req.body.nombre ?? req.body.titulo, 255);
    const descripcion = sanitizarInput(req.body.descripcion, 1000);
    const puntos = parseInt(req.body.puntosRequeridos ?? req.body.puntos_requeridos, 10);
    const icono = sanitizarInput(req.body.icono, 16) || 'oferta';
    const stock = req.body.stock != null ? parseInt(req.body.stock, 10) : null;
    const imagenUrl = sanitizarInput(req.body.imagenUrl ?? req.body.imagen_url, 500);
    const tipo = normalizarTipo(req.body.tipo);
    const fechaInicio = parseFecha(req.body.fechaInicio ?? req.body.fecha_inicio) || new Date().toISOString();
    const fechaFin =
      parseFecha(req.body.fechaFin ?? req.body.fecha_fin) ||
      new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString();

    if (!nombre || !puntos || puntos <= 0) {
      return responderError(res, 400, 'Nombre y puntos requeridos válidos');
    }

    const insert = await query(
      `INSERT INTO piku_recompensas
       (comercio_id, nombre, descripcion, puntos_requeridos, icono, stock, imagen_url,
        tipo, porcentaje_descuento, monto_maximo_descuento, producto_nombre,
        fecha_inicio, fecha_fin, horarios_validos, max_usos_por_usuario, max_usos_totales, usos_actuales, activo)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,0,TRUE)
       RETURNING *`,
      [
        comercioId,
        nombre,
        descripcion,
        puntos,
        icono,
        stock,
        imagenUrl || null,
        tipo,
        parseIntOrNull(req.body.porcentajeDescuento ?? req.body.porcentaje_descuento),
        parseFloatOrNull(req.body.montoMaximoDescuento ?? req.body.monto_maximo_descuento),
        sanitizarInput(req.body.productoNombre ?? req.body.producto_nombre, 100),
        fechaInicio,
        fechaFin,
        req.body.horariosValidos ?? req.body.horarios_validos
          ? JSON.stringify(req.body.horariosValidos ?? req.body.horarios_validos)
          : null,
        parseIntOrNull(req.body.maxUsosPorUsuario ?? req.body.max_usos_por_usuario) ?? 1,
        parseIntOrNull(req.body.maxUsosTotales ?? req.body.max_usos_totales) ?? 0,
      ]
    );

    return res.status(201).json({ mensaje: 'Recompensa creada', recompensa: insert.rows[0] });
  } catch (error) {
    console.error('createRecompensa:', error);
    return responderError(res, 500, 'Error al crear recompensa', { detail: error.message });
  }
}

/**
 * Actualiza una recompensa.
 */
async function updateRecompensa(req, res) {
  try {
    const comercioId = getComercioId(req);
    const { id } = req.params;

    const campos = [];
    const valores = [];
    let idx = 1;

    const mapa = {
      nombre: sanitizarInput(req.body.nombre ?? req.body.titulo, 255),
      descripcion: sanitizarInput(req.body.descripcion, 1000),
      icono: sanitizarInput(req.body.icono, 16),
      imagen_url: sanitizarInput(req.body.imagenUrl ?? req.body.imagen_url, 500),
      activo: req.body.activo,
      stock: req.body.stock != null ? parseInt(req.body.stock, 10) : undefined,
      tipo: req.body.tipo != null ? normalizarTipo(req.body.tipo) : undefined,
      porcentaje_descuento: parseIntOrNull(
        req.body.porcentajeDescuento ?? req.body.porcentaje_descuento
      ),
      monto_maximo_descuento: parseFloatOrNull(
        req.body.montoMaximoDescuento ?? req.body.monto_maximo_descuento
      ),
      producto_nombre: sanitizarInput(req.body.productoNombre ?? req.body.producto_nombre, 100),
      fecha_inicio: parseFecha(req.body.fechaInicio ?? req.body.fecha_inicio),
      fecha_fin: parseFecha(req.body.fechaFin ?? req.body.fecha_fin),
      max_usos_por_usuario: parseIntOrNull(
        req.body.maxUsosPorUsuario ?? req.body.max_usos_por_usuario
      ),
      max_usos_totales: parseIntOrNull(req.body.maxUsosTotales ?? req.body.max_usos_totales),
      puntos_requeridos:
        req.body.puntosRequeridos != null
          ? parseInt(req.body.puntosRequeridos, 10)
          : req.body.puntos_requeridos != null
            ? parseInt(req.body.puntos_requeridos, 10)
            : undefined,
    };

    if (req.body.horariosValidos != null || req.body.horarios_validos != null) {
      mapa.horarios_validos = JSON.stringify(
        req.body.horariosValidos ?? req.body.horarios_validos
      );
    }

    for (const [col, val] of Object.entries(mapa)) {
      if (val !== undefined && val !== '') {
        campos.push(`${col} = $${idx++}`);
        valores.push(val);
      }
    }

    if (!campos.length) return responderError(res, 400, 'Sin datos para actualizar');

    campos.push('updated_at = NOW()');
    valores.push(id, comercioId);

    const sql = `UPDATE piku_recompensas SET ${campos.join(', ')}
                 WHERE id = $${idx++} AND comercio_id = $${idx} RETURNING *`;
    const updated = await query(sql, valores);

    if (!updated.rows.length) return responderError(res, 404, 'Recompensa no encontrada');
    return res.json({ mensaje: 'Recompensa actualizada', recompensa: updated.rows[0] });
  } catch (error) {
    console.error('updateRecompensa:', error);
    return responderError(res, 500, 'Error al actualizar recompensa');
  }
}

/**
 * Elimina (desactiva) una recompensa.
 */
async function deleteRecompensa(req, res) {
  try {
    const comercioId = getComercioId(req);
    const { id } = req.params;

    const result = await query(
      `UPDATE piku_recompensas SET activo = FALSE, updated_at = NOW()
       WHERE id = $1 AND comercio_id = $2 RETURNING id`,
      [id, comercioId]
    );
    if (!result.rows.length) return responderError(res, 404, 'Recompensa no encontrada');
    return res.json({ mensaje: 'Recompensa eliminada' });
  } catch (error) {
    console.error('deleteRecompensa:', error);
    return responderError(res, 500, 'Error al eliminar recompensa');
  }
}

async function duplicateRecompensa(req, res) {
  try {
    const comercioId = getComercioId(req);
    const { id } = req.params;
    const orig = await query(
      'SELECT * FROM piku_recompensas WHERE id = $1 AND comercio_id = $2',
      [id, comercioId]
    );
    if (!orig.rows.length) return responderError(res, 404, 'Recompensa no encontrada');
    const r = orig.rows[0];

    const insert = await query(
      `INSERT INTO piku_recompensas
       (comercio_id, nombre, descripcion, puntos_requeridos, icono, stock, imagen_url,
        tipo, porcentaje_descuento, monto_maximo_descuento, producto_nombre,
        fecha_inicio, fecha_fin, horarios_validos, max_usos_por_usuario, max_usos_totales, usos_actuales, activo)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,0,TRUE)
       RETURNING *`,
      [
        comercioId,
        `${r.nombre} (copia)`,
        r.descripcion,
        r.puntos_requeridos,
        r.icono,
        r.stock,
        r.imagen_url,
        r.tipo,
        r.porcentaje_descuento,
        r.monto_maximo_descuento,
        r.producto_nombre,
        r.fecha_inicio,
        r.fecha_fin,
        r.horarios_validos,
        r.max_usos_por_usuario,
        r.max_usos_totales,
      ]
    );

    return res.status(201).json({ mensaje: 'Oferta duplicada', recompensa: insert.rows[0] });
  } catch (error) {
    console.error('duplicateRecompensa:', error);
    return responderError(res, 500, 'Error al duplicar recompensa');
  }
}

async function uploadImagenRecompensa(req, res) {
  try {
    const comercioId = getComercioId(req);
    const { id } = req.params;
    if (!req.file) return responderError(res, 400, 'Archivo de imagen requerido');
    if (!cloudinaryOk) return responderError(res, 503, 'Cloudinary no configurado en el servidor');

    const existe = await query(
      'SELECT id FROM piku_recompensas WHERE id = $1 AND comercio_id = $2',
      [id, comercioId]
    );
    if (!existe.rows.length) return responderError(res, 404, 'Recompensa no encontrada');

    const dataUri = `data:${req.file.mimetype};base64,${req.file.buffer.toString('base64')}`;
    const { url } = await uploadImage(dataUri, 'ofertas');

    const updated = await query(
      'UPDATE piku_recompensas SET imagen_url = $1, updated_at = NOW() WHERE id = $2 RETURNING *',
      [url, id]
    );

    return res.json({ mensaje: 'Imagen actualizada', imagen_url: url, recompensa: updated.rows[0] });
  } catch (error) {
    console.error('uploadImagenRecompensa:', error);
    return responderError(res, 500, 'Error al subir imagen', { detail: error.message });
  }
}

/**
 * Genera QR dinámico (alias de qr.controller generarQR).
 */
async function generarQRComercio(req, res) {
  const { generarQR } = require('./qr.controller');
  return generarQR(req, res);
}

/**
 * Estadísticas del dashboard del comercio.
 */
async function getEstadisticas(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const [escaneos, puntos, canjes, clientes] = await Promise.all([
      query(
        `SELECT COUNT(*)::int AS total FROM piku_qr_dinamicos
         WHERE comercio_id = $1 AND usado = TRUE`,
        [comercioId]
      ),
      query(
        `SELECT COALESCE(SUM(puntos), 0)::int AS total FROM piku_transacciones_puntos
         WHERE comercio_id = $1 AND tipo = 'ganado'`,
        [comercioId]
      ),
      query(
        `SELECT COUNT(*)::int AS total FROM piku_canjes c
         INNER JOIN piku_recompensas r ON r.id = c.recompensa_id
         WHERE r.comercio_id = $1`,
        [comercioId]
      ),
      query(
        `SELECT COUNT(DISTINCT usuario_id)::int AS total FROM piku_transacciones_puntos
         WHERE comercio_id = $1`,
        [comercioId]
      ),
    ]);

    const ultimos = await query(
      `SELECT t.puntos, t.tipo, t.descripcion, t.created_at, u.nombre AS cliente
       FROM piku_transacciones_puntos t
       LEFT JOIN piku_usuarios u ON u.id = t.usuario_id
       WHERE t.comercio_id = $1
       ORDER BY t.created_at DESC LIMIT 10`,
      [comercioId]
    );

    return res.json({
      estadisticas: {
        qrUsados: escaneos.rows[0].total,
        puntosOtorgados: puntos.rows[0].total,
        canjesRealizados: canjes.rows[0].total,
        clientesUnicos: clientes.rows[0].total,
      },
      ultimasTransacciones: ultimos.rows,
    });
  } catch (error) {
    console.error('getEstadisticas:', error);
    return responderError(res, 500, 'Error al obtener estadísticas');
  }
}

/**
 * Obtiene configuración de envíos del comercio autenticado.
 */
async function getConfigEnvios(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const columnas = await selectComerciosColumnas();
    const result = await query(
      `SELECT ${columnas} FROM piku_comercios c WHERE c.id = $1`,
      [comercioId]
    );
    if (!result.rows.length) return responderError(res, 404, 'Comercio no encontrado');

    const c = result.rows[0];
    return res.json({
      envios: {
        realiza_envios: Boolean(c.realiza_envios),
        envio_gratis: Boolean(c.envio_gratis),
        costo_envio: c.costo_envio != null ? parseFloat(c.costo_envio) : 0,
        envio_minimo_compra:
          c.envio_minimo_compra != null ? parseFloat(c.envio_minimo_compra) : null,
        telefono_contacto: c.telefono_contacto || null,
      },
      comercio: c,
    });
  } catch (error) {
    console.error('getConfigEnvios:', error);
    return responderError(res, 500, 'Error al obtener envíos', { detail: error.message });
  }
}

/**
 * Actualiza configuración de envíos del comercio.
 */
async function updateConfigEnvios(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const cols = await columnasTabla('piku_comercios');
    const realizaEnvios = Boolean(
      req.body.realiza_envios ?? req.body.realizaEnvios ?? false
    );

    const sets = [];
    const vals = [];
    let idx = 1;

    const addSet = (col, val) => {
      if (tiene(cols, col)) {
        sets.push(`${col} = $${idx++}`);
        vals.push(val);
      }
    };

    addSet('realiza_envios', realizaEnvios);

    if (!realizaEnvios) {
      addSet('envio_gratis', false);
      addSet('costo_envio', 0);
      addSet('envio_minimo_compra', null);
    } else {
      const envioGratis = Boolean(req.body.envio_gratis ?? req.body.envioGratis ?? false);
      const costoRaw = req.body.costo_envio ?? req.body.costoEnvio;
      const minimoRaw = req.body.envio_minimo_compra ?? req.body.envioMinimoCompra;
      const costo =
        costoRaw != null && costoRaw !== '' ? parseFloat(costoRaw) : 0;
      const minimo =
        minimoRaw != null && minimoRaw !== '' ? parseFloat(minimoRaw) : null;

      addSet('envio_gratis', envioGratis);
      addSet('costo_envio', Number.isFinite(costo) ? costo : 0);
      addSet('envio_minimo_compra', Number.isFinite(minimo) ? minimo : null);

      const telBody = req.body.telefono_contacto ?? req.body.telefonoContacto ?? req.body.telefono;
      if (telBody) {
        const tel = validarTelefonoComercio(telBody);
        if (!tel.ok) return responderError(res, 400, tel.mensaje);
        addSet('telefono_contacto', tel.normalizado);
      }
    }

    if (!sets.length) {
      return responderError(res, 400, 'Sin columnas de envío en la base de datos');
    }

    vals.push(comercioId);
    await query(
      `UPDATE piku_comercios SET ${sets.join(', ')} WHERE id = $${idx}`,
      vals
    );

    invalidarCacheComerciosSelect();

    const columnas = await selectComerciosColumnas();
    const updated = await query(
      `SELECT ${columnas} FROM piku_comercios c WHERE c.id = $1`,
      [comercioId]
    );

    const c = updated.rows[0];
    return res.json({
      mensaje: 'Configuración de envíos guardada',
      envios: {
        realiza_envios: Boolean(c.realiza_envios),
        envio_gratis: Boolean(c.envio_gratis),
        costo_envio: c.costo_envio != null ? parseFloat(c.costo_envio) : 0,
        envio_minimo_compra:
          c.envio_minimo_compra != null ? parseFloat(c.envio_minimo_compra) : null,
        telefono_contacto: c.telefono_contacto || null,
      },
    });
  } catch (error) {
    console.error('updateConfigEnvios:', error);
    return responderError(res, 500, 'Error al guardar envíos', { detail: error.message });
  }
}

/**
 * Notificaciones del comercio (persistidas; NOTIFY es solo señal en tiempo real).
 */
async function obtenerNotificaciones(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const limite = Math.min(Math.max(parseInt(req.query.limite, 10) || 20, 1), 50);
    const offset = Math.max(parseInt(req.query.offset, 10) || 0, 0);
    const soloNoLeidas = req.query.solo_no_leidas === 'true';

    let sql = `
      SELECT id, comercio_id, usuario_id, recompensa_id, canje_id, tipo, titulo, cuerpo, leida, created_at
      FROM piku_notificaciones
      WHERE comercio_id = $1`;
    const params = [comercioId];

    if (soloNoLeidas) {
      sql += ' AND leida = false';
    }
    sql += ` ORDER BY created_at DESC LIMIT $2 OFFSET $3`;
    params.push(limite, offset);

    const [rows, countRes] = await Promise.all([
      query(sql, params),
      query(
        `SELECT COUNT(*)::int AS total FROM piku_notificaciones
         WHERE comercio_id = $1${soloNoLeidas ? ' AND leida = false' : ''}`,
        [comercioId]
      ),
    ]);

    return res.json({
      notificaciones: rows.rows,
      total: countRes.rows[0].total,
      limite,
      offset,
    });
  } catch (error) {
    console.error('obtenerNotificaciones:', error);
    return responderError(res, 500, 'Error al obtener notificaciones');
  }
}

/**
 * Cantidad de notificaciones sin leer (badge en app comercio).
 */
async function contarNotificacionesNoLeidas(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const result = await query(
      `SELECT COUNT(*)::int AS total FROM piku_notificaciones
       WHERE comercio_id = $1 AND leida = false`,
      [comercioId]
    );
    return res.json({ noLeidas: result.rows[0].total });
  } catch (error) {
    console.error('contarNotificacionesNoLeidas:', error);
    return responderError(res, 500, 'Error al contar notificaciones');
  }
}

/**
 * Marca una notificación como leída (solo del comercio autenticado).
 */
async function marcarNotificacionLeida(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const { id } = req.params;
    if (!esUuid(id)) return responderError(res, 400, 'ID de notificación inválido');

    const updated = await query(
      `UPDATE piku_notificaciones SET leida = true
       WHERE id = $1 AND comercio_id = $2
       RETURNING id`,
      [id, comercioId]
    );
    if (!updated.rows.length) return responderError(res, 404, 'Notificación no encontrada');

    return res.json({ mensaje: 'Notificación marcada como leída', id: updated.rows[0].id });
  } catch (error) {
    console.error('marcarNotificacionLeida:', error);
    return responderError(res, 500, 'Error al marcar notificación');
  }
}

/**
 * Historial de canjes del comercio con filtros y paginación.
 */
async function obtenerHistorialCanjes(req, res) {
  try {
    const comercioId = getComercioId(req);
    if (!comercioId) return responderError(res, 403, 'Sin comercio asociado');

    const pagina = Math.max(parseInt(req.query.pagina, 10) || 1, 1);
    const limite = Math.min(Math.max(parseInt(req.query.limite, 10) || 20, 1), 50);
    const offset = (pagina - 1) * limite;
    const { estado, fecha_desde, fecha_hasta, buscar } = req.query;

    if (estado && !ESTADOS_CANJE.has(estado)) {
      return responderError(res, 400, 'Estado de canje inválido');
    }

    let sql = `
      SELECT
        c.id,
        c.usuario_id,
        c.recompensa_id,
        c.puntos_usados,
        c.codigo_canje,
        c.estado,
        c.created_at,
        u.nombre AS cliente_nombre,
        r.nombre AS oferta_nombre,
        r.puntos_requeridos,
        r.tipo AS oferta_tipo,
        r.imagen_url
      FROM piku_canjes c
      INNER JOIN piku_recompensas r ON r.id = c.recompensa_id
      LEFT JOIN piku_usuarios u ON u.id = c.usuario_id
      WHERE r.comercio_id = $1`;
    const params = [comercioId];
    let idx = 2;

    if (estado) {
      sql += ` AND c.estado = $${idx++}`;
      params.push(estado);
    }
    if (fecha_desde) {
      sql += ` AND c.created_at >= $${idx++}`;
      params.push(fecha_desde);
    }
    if (fecha_hasta) {
      sql += ` AND c.created_at <= $${idx++}`;
      params.push(fecha_hasta);
    }
    if (buscar && String(buscar).trim()) {
      sql += ` AND (u.nombre ILIKE $${idx} ESCAPE '\\' OR r.nombre ILIKE $${idx} ESCAPE '\\' OR c.codigo_canje ILIKE $${idx} ESCAPE '\\')`;
      params.push(`%${escapeIlike(String(buscar).trim())}%`);
      idx += 1;
    }

    const countSql = sql.replace(
      /SELECT[\s\S]*FROM piku_canjes c/,
      'SELECT COUNT(*)::int AS total FROM piku_canjes c'
    );

    sql += ` ORDER BY c.created_at DESC LIMIT $${idx++} OFFSET $${idx++}`;
    params.push(limite, offset);

    const [canjes, totalRes] = await Promise.all([
      query(sql, params),
      query(countSql, params.slice(0, params.length - 2)),
    ]);

    return res.json({
      canjes: canjes.rows,
      total: totalRes.rows[0].total,
      pagina,
      limite,
    });
  } catch (error) {
    console.error('obtenerHistorialCanjes:', error);
    return responderError(res, 500, 'Error al obtener historial de canjes');
  }
}

/**
 * Registra token FCM del usuario comercio (push en segundo plano).
 */
async function registrarFcmToken(req, res) {
  try {
    const token = String(req.body.token || req.body.fcm_token || '').trim();
    if (!token || token.length < 20 || token.length > 512) {
      return responderError(res, 400, 'Token FCM inválido');
    }

    await query(
      'UPDATE piku_usuarios SET fcm_token = $1, updated_at = NOW() WHERE id = $2',
      [token, req.user.id]
    );

    return res.json({ mensaje: 'Token FCM registrado' });
  } catch (error) {
    console.error('registrarFcmToken:', error);
    return responderError(res, 500, 'Error al registrar dispositivo');
  }
}

module.exports = {
  getReglasPuntos,
  updateReglasPuntos,
  getRecompensas,
  getRecompensa,
  getRecompensaStats,
  createRecompensa,
  duplicateRecompensa,
  updateRecompensa,
  deleteRecompensa,
  uploadImagenRecompensa,
  generarQR: generarQRComercio,
  getEstadisticas,
  getConfigEnvios,
  updateConfigEnvios,
  obtenerNotificaciones,
  contarNotificacionesNoLeidas,
  marcarNotificacionLeida,
  obtenerHistorialCanjes,
  registrarFcmToken,
};
