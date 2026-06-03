const bcrypt = require('bcryptjs');
const axios = require('axios');
const { query, withTransaction } = require('../services/neon.service');
const { signToken } = require('../middleware/auth.middleware');
const { validarEmail, sanitizarInput, responderError } = require('../utils/helpers');
const { columnasTabla, tiene } = require('../utils/schema.util');
const osmService = require('../services/osm.service');
const { normalizarTipoComercio } = require('../constants/tipos_comercio');
const {
  resolverDireccionRegistro,
  agregarCamposDireccionUsuario,
} = require('../utils/direccion.registro.util');
const { validarTelefonoComercio } = require('../utils/telefono.util');
const { otorgarBonoBienvenida } = require('../services/puntos.service');
const { verificarIdTokenGoogle } = require('../utils/google.token.util');

/**
 * Publica el comercio como Note en OSM (no bloquea el registro si falla).
 */
async function publicarComercioEnOsm(comercio) {
  try {
    const resultado = await osmService.createNoteForComercio(comercio);
    if (!resultado) return;

    const cols = await columnasTabla('piku_comercios');
    if (resultado.noteId && tiene(cols, 'osm_note_id')) {
      const sets = ['osm_note_id = $1'];
      const vals = [resultado.noteId];
      if (tiene(cols, 'osm_note_created_at')) {
        sets.push('osm_note_created_at = NOW()');
      }
      vals.push(comercio.id);
      await query(
        `UPDATE piku_comercios SET ${sets.join(', ')} WHERE id = $${vals.length}`,
        vals
      );
    }
    console.log(`[OSM] Note creado para comercio ${comercio.id}`, resultado.noteId || '(sin id)');
  } catch (error) {
    console.error('[OSM] No se pudo crear Note:', error.message);
  }
}

async function camposUsuario(permitePassword = false) {
  const cols = await columnasTabla('piku_usuarios');
  const lista = ['id', 'email', 'nombre', 'telefono', 'rol'];
  if (permitePassword && tiene(cols, 'password_hash')) lista.push('password_hash');
  for (const c of ['activo', 'puntos_saldo', 'comercio_id', 'avatar_url', 'google_id', 'created_at', 'updated_at']) {
    if (tiene(cols, c)) lista.push(c);
  }
  return lista;
}

let cacheUsuarioSelectCols = null;
let cacheUsuarioTieneGoogleId = null;

async function prepararConsultaUsuario() {
  if (cacheUsuarioSelectCols != null) {
    return { selectCols: cacheUsuarioSelectCols, tieneGoogleId: cacheUsuarioTieneGoogleId };
  }
  const cols = await columnasTabla('piku_usuarios');
  cacheUsuarioTieneGoogleId = tiene(cols, 'google_id');
  cacheUsuarioSelectCols = (await camposUsuario()).join(', ');
  return { selectCols: cacheUsuarioSelectCols, tieneGoogleId: cacheUsuarioTieneGoogleId };
}

function normalizarUsuario(row) {
  if (!row) return row;
  if (row.puntos_saldo == null) row.puntos_saldo = 0;
  if (row.activo == null) row.activo = true;
  return row;
}

/**
 * Registro de cliente (rol cliente).
 */
async function registroCliente(req, res) {
  try {
    const email = sanitizarInput(req.body.email, 255).toLowerCase();
    const password = String(req.body.password || '');
    const nombre = sanitizarInput(req.body.nombre, 255);
    const telefono = sanitizarInput(req.body.telefono, 50);

    if (!validarEmail(email)) return responderError(res, 400, 'Email inválido');
    if (password.length < 6) return responderError(res, 400, 'La contraseña debe tener al menos 6 caracteres');
    if (!nombre) return responderError(res, 400, 'Nombre requerido');

    let direccionData;
    try {
      direccionData = await resolverDireccionRegistro(req.body);
    } catch (e) {
      return responderError(res, e.status || 400, e.message);
    }

    const existe = await query('SELECT id FROM piku_usuarios WHERE LOWER(email) = $1', [email]);
    if (existe.rows.length) {
      return responderError(
        res,
        409,
        'Este email ya está registrado. Probá «Ingresar» o usá otro correo.'
      );
    }

    const hash = await bcrypt.hash(password, 10);
    const colsUsuario = await columnasTabla('piku_usuarios');
    const campos = ['email', 'password_hash', 'nombre', 'telefono', 'rol'];
    const vals = [email, hash, nombre, telefono || null, 'cliente'];
    agregarCamposDireccionUsuario(colsUsuario, campos, vals, direccionData);
    const returning = (await camposUsuario()).join(', ');
    const placeholders = vals.map((_, i) => `$${i + 1}`).join(', ');
    const insert = await query(
      `INSERT INTO piku_usuarios (${campos.join(', ')})
       VALUES (${placeholders})
       RETURNING ${returning}`,
      vals
    );

    const usuario = normalizarUsuario(insert.rows[0]);
    let puntosBienvenida = 0;
    try {
      const bono = await withTransaction(async (client) =>
        otorgarBonoBienvenida(client, usuario.id)
      );
      if (bono.otorgado) {
        puntosBienvenida = bono.puntos;
        usuario.puntos_saldo = (usuario.puntos_saldo || 0) + bono.puntos;
      }
    } catch (e) {
      console.warn('bono bienvenida registroCliente:', e.message);
    }

    const token = signToken({ userId: usuario.id, rol: usuario.rol });

    return res.status(201).json({
      mensaje: 'Cliente registrado correctamente',
      token,
      usuario,
      puntosBienvenida,
    });
  } catch (error) {
    console.error('registroCliente:', error);
    return responderError(res, 500, 'Error al registrar cliente', { detail: error.message });
  }
}

/**
 * Registro de comercio (requiere código de invitación o usuario admin).
 */
async function registroComercio(req, res) {
  try {
    const email = sanitizarInput(req.body.email, 255).toLowerCase();
    const password = String(req.body.password || '');
    const nombre = sanitizarInput(req.body.nombre, 255);
    const nombreComercio = sanitizarInput(req.body.nombreComercio || req.body.nombre_comercio, 255);
    const codigoInvitacion = sanitizarInput(req.body.codigoInvitacion || req.body.codigo_invitacion, 64);
    const tipoRaw = req.body.tipoComercio || req.body.tipo_comercio || req.body.categoria;
    const tipoInfo = normalizarTipoComercio(tipoRaw);

    if (!validarEmail(email)) return responderError(res, 400, 'Email inválido');
    if (password.length < 6) return responderError(res, 400, 'La contraseña debe tener al menos 6 caracteres');
    if (!nombre || !nombreComercio) {
      return responderError(res, 400, 'Nombre del responsable y del comercio son requeridos');
    }

    let direccionData;
    try {
      direccionData = await resolverDireccionRegistro(req.body, {
        lat: req.body.lat,
        lon: req.body.lon,
      });
    } catch (e) {
      return responderError(res, e.status || 400, e.message);
    }

    const telCheck = validarTelefonoComercio(req.body.telefono);
    if (!telCheck.ok) return responderError(res, 400, telCheck.mensaje);
    const telefonoComercio = telCheck.normalizado;

    const existe = await query('SELECT id FROM piku_usuarios WHERE LOWER(email) = $1', [email]);
    if (existe.rows.length) {
      return responderError(
        res,
        409,
        'Este email ya está registrado. Probá «Ingresar» o usá otro correo.'
      );
    }

    const colsComercio = await columnasTabla('piku_comercios');
    const colsInv = await columnasTabla('piku_invitaciones_comercio');
    const colsUsuario = await columnasTabla('piku_usuarios');

    const resultado = await withTransaction(async (client) => {
      const esAdmin = req.user?.rol === 'admin';
      if (!esAdmin) {
        const codigoEnv = process.env.PIKU_CODIGO_INVITACION || '';
        const codigoValido =
          codigoInvitacion &&
          (codigoEnv && codigoInvitacion.toUpperCase() === codigoEnv.toUpperCase());

        if (!codigoValido) {
          if (!codigoInvitacion) throw new Error('Código de invitación requerido');
          const invCols = ['id'];
          if (tiene(colsInv, 'usado')) invCols.push('usado');
          if (tiene(colsInv, 'expires_at')) invCols.push('expires_at');
          const inv = await client.query(
            `SELECT ${invCols.join(', ')} FROM piku_invitaciones_comercio
             WHERE UPPER(codigo) = UPPER($1) LIMIT 1`,
            [codigoInvitacion]
          );
          if (!inv.rows.length) throw new Error('Invitación inválida');
          if (inv.rows[0].usado) throw new Error('Invitación ya utilizada');
          if (inv.rows[0].expires_at && new Date(inv.rows[0].expires_at) < new Date()) {
            throw new Error('Invitación expirada');
          }
        }
      }

      const hash = await bcrypt.hash(password, 10);

      const comercioCampos = ['nombre'];
      const comercioVals = [nombreComercio];
      const addComercio = (col, val) => {
        if (tiene(colsComercio, col)) {
          comercioCampos.push(col);
          comercioVals.push(val);
        }
      };
      addComercio('direccion', direccionData.direccion);
      addComercio('lat', direccionData.lat);
      addComercio('lon', direccionData.lon);
      addComercio('suscripcion_activa', true);
      addComercio('categoria', tipoInfo.categoria);
      addComercio('tipo_comercio', tipoInfo.id);
      addComercio('icono_emoji', tipoInfo.emoji);
      addComercio('telefono_contacto', telefonoComercio);

      const comercioPlaceholders = comercioVals.map((_, i) => `$${i + 1}`).join(', ');
      const comercioReturning = ['id', 'nombre'];
      for (const c of ['direccion', 'lat', 'lon', 'categoria', 'tipo_comercio', 'icono_emoji', 'telefono_contacto']) {
        if (tiene(colsComercio, c)) comercioReturning.push(c);
      }

      const comercioInsert = await client.query(
        `INSERT INTO piku_comercios (${comercioCampos.join(', ')})
         VALUES (${comercioPlaceholders})
         RETURNING ${comercioReturning.join(', ')}`,
        comercioVals
      );
      const comercio = comercioInsert.rows[0];

      const usuarioCampos = ['email', 'password_hash', 'nombre', 'rol'];
      const usuarioVals = [email, hash, nombre, 'comercio'];
      if (tiene(colsUsuario, 'telefono')) {
        usuarioCampos.push('telefono');
        usuarioVals.push(telefonoComercio);
      }
      if (tiene(colsUsuario, 'comercio_id')) {
        usuarioCampos.push('comercio_id');
        usuarioVals.push(comercio.id);
      }
      agregarCamposDireccionUsuario(colsUsuario, usuarioCampos, usuarioVals, direccionData);

      const usuarioReturning = ['id', 'email', 'nombre', 'rol'];
      if (tiene(colsUsuario, 'comercio_id')) usuarioReturning.push('comercio_id');

      const usuarioInsert = await client.query(
        `INSERT INTO piku_usuarios (${usuarioCampos.join(', ')})
         VALUES (${usuarioVals.map((_, i) => `$${i + 1}`).join(', ')})
         RETURNING ${usuarioReturning.join(', ')}`,
        usuarioVals
      );
      const usuario = normalizarUsuario(usuarioInsert.rows[0]);

      const linkCol = tiene(colsComercio, 'usuario_id')
        ? 'usuario_id'
        : tiene(colsComercio, 'owner_usuario_id')
          ? 'owner_usuario_id'
          : null;
      if (linkCol) {
        await client.query(`UPDATE piku_comercios SET ${linkCol} = $1 WHERE id = $2`, [
          usuario.id,
          comercio.id,
        ]);
      }

      try {
        await client.query(
          `INSERT INTO piku_reglas_puntos (comercio_id, puntos_por_peso, monto_minimo, puntos_fijos, max_puntos_por_dia)
           VALUES ($1, 1, 0, 10, 500)
           ON CONFLICT (comercio_id) DO NOTHING`,
          [comercio.id]
        );
      } catch (_e) {
        /* tabla opcional en instalaciones viejas */
      }

      if (!esAdmin && codigoInvitacion && tiene(colsInv, 'usado')) {
        const sets = ['usado = TRUE'];
        if (tiene(colsInv, 'comercio_id')) sets.push('comercio_id = $1');
        await client.query(
          `UPDATE piku_invitaciones_comercio SET ${sets.join(', ')} WHERE UPPER(codigo) = UPPER($${tiene(colsInv, 'comercio_id') ? '2' : '1'})`,
          tiene(colsInv, 'comercio_id') ? [comercio.id, codigoInvitacion] : [codigoInvitacion]
        );
      }

      return { usuario, comercio };
    });

    const token = signToken({
      userId: resultado.usuario.id,
      rol: resultado.usuario.rol,
      comercioId: resultado.usuario.comercio_id,
    });

    await publicarComercioEnOsm(resultado.comercio);

    return res.status(201).json({
      mensaje: 'Comercio registrado correctamente',
      token,
      usuario: resultado.usuario,
      comercio: resultado.comercio,
    });
  } catch (error) {
    console.error('registroComercio:', error);
    const status = /invitación/i.test(error.message) ? 403 : 500;
    return responderError(res, status, error.message || 'Error al registrar comercio');
  }
}

/**
 * Login con email y contraseña.
 */
async function login(req, res) {
  try {
    const email = sanitizarInput(req.body.email, 255).toLowerCase();
    const password = String(req.body.password || '');

    if (!validarEmail(email) || !password) {
      return responderError(res, 400, 'Email y contraseña requeridos');
    }

    const selectCols = (await camposUsuario(true)).join(', ');
    const result = await query(
      `SELECT ${selectCols} FROM piku_usuarios WHERE LOWER(email) = $1 LIMIT 1`,
      [email]
    );

    if (!result.rows.length) {
      return responderError(res, 401, 'Credenciales inválidas');
    }

    const usuario = normalizarUsuario(result.rows[0]);
    if (usuario.activo === false) {
      return responderError(res, 401, 'Credenciales inválidas');
    }

    const ok = await bcrypt.compare(password, usuario.password_hash);
    if (!ok) return responderError(res, 401, 'Credenciales inválidas');

    delete usuario.password_hash;
    const token = signToken({
      userId: usuario.id,
      rol: usuario.rol,
      comercioId: usuario.comercio_id,
    });

    return res.json({ mensaje: 'Login exitoso', token, usuario });
  } catch (error) {
    console.error('login:', error);
    return responderError(res, 500, 'Error al iniciar sesión', { detail: error.message });
  }
}

/**
 * Registro de comercio con cuenta Google.
 */
async function registroComercioGoogle(req, res) {
  try {
    const idToken = String(req.body.idToken || req.body.id_token || '').trim();
    if (!idToken) return responderError(res, 400, 'Token de Google requerido');

    const google = await verificarIdTokenGoogle(idToken);
    const nombre = sanitizarInput(req.body.nombre, 255) || google.nombre;
    const nombreComercio = sanitizarInput(req.body.nombreComercio || req.body.nombre_comercio, 255);
    const codigoInvitacion = sanitizarInput(req.body.codigoInvitacion || req.body.codigo_invitacion, 64);
    const tipoRaw = req.body.tipoComercio || req.body.tipo_comercio || req.body.categoria;
    const tipoInfo = normalizarTipoComercio(tipoRaw);

    if (!nombre || !nombreComercio) {
      return responderError(res, 400, 'Nombre del responsable y del comercio son requeridos');
    }

    let direccionData;
    try {
      direccionData = await resolverDireccionRegistro(req.body, {
        lat: req.body.lat,
        lon: req.body.lon,
      });
    } catch (e) {
      return responderError(res, e.status || 400, e.message);
    }

    const telCheck = validarTelefonoComercio(req.body.telefono);
    if (!telCheck.ok) return responderError(res, 400, telCheck.mensaje);
    const telefonoComercio = telCheck.normalizado;

    const selectCols = (await camposUsuario()).join(', ');
    const existente = await query(
      `SELECT ${selectCols} FROM piku_usuarios WHERE LOWER(email) = $1 LIMIT 1`,
      [google.email]
    );

    if (existente.rows.length) {
      const usuario = normalizarUsuario(existente.rows[0]);
      if (usuario.rol === 'comercio') {
        if (!usuario.activo) return responderError(res, 403, 'Cuenta desactivada');
        const token = signToken({
          userId: usuario.id,
          rol: usuario.rol,
          comercioId: usuario.comercio_id,
        });
        return res.json({
          mensaje: 'Sesión de comercio iniciada',
          token,
          usuario,
        });
      }
      return responderError(
        res,
        409,
        'Este email ya está registrado como cliente. Usá otro correo o ingresá con Google como cliente.'
      );
    }

    const colsComercio = await columnasTabla('piku_comercios');
    const colsInv = await columnasTabla('piku_invitaciones_comercio');
    const colsUsuario = await columnasTabla('piku_usuarios');

    const resultado = await withTransaction(async (client) => {
      const codigoEnv = process.env.PIKU_CODIGO_INVITACION || '';
      const codigoValido =
        codigoInvitacion &&
        (codigoEnv && codigoInvitacion.toUpperCase() === codigoEnv.toUpperCase());

      if (!codigoValido) {
        if (!codigoInvitacion) throw new Error('Código de invitación requerido');
        const invCols = ['id'];
        if (tiene(colsInv, 'usado')) invCols.push('usado');
        if (tiene(colsInv, 'expires_at')) invCols.push('expires_at');
        const inv = await client.query(
          `SELECT ${invCols.join(', ')} FROM piku_invitaciones_comercio
           WHERE UPPER(codigo) = UPPER($1) LIMIT 1`,
          [codigoInvitacion]
        );
        if (!inv.rows.length) throw new Error('Invitación inválida');
        if (inv.rows[0].usado) throw new Error('Invitación ya utilizada');
        if (inv.rows[0].expires_at && new Date(inv.rows[0].expires_at) < new Date()) {
          throw new Error('Invitación expirada');
        }
      }

      const hash = await bcrypt.hash(`google:${google.googleId}:${Date.now()}`, 10);

      const comercioCampos = ['nombre'];
      const comercioVals = [nombreComercio];
      const addComercio = (col, val) => {
        if (tiene(colsComercio, col)) {
          comercioCampos.push(col);
          comercioVals.push(val);
        }
      };
      addComercio('direccion', direccionData.direccion);
      addComercio('lat', direccionData.lat);
      addComercio('lon', direccionData.lon);
      addComercio('suscripcion_activa', true);
      addComercio('categoria', tipoInfo.categoria);
      addComercio('tipo_comercio', tipoInfo.id);
      addComercio('icono_emoji', tipoInfo.emoji);
      addComercio('telefono_contacto', telefonoComercio);

      const comercioPlaceholders = comercioVals.map((_, i) => `$${i + 1}`).join(', ');
      const comercioReturning = ['id', 'nombre'];
      for (const c of ['direccion', 'lat', 'lon', 'categoria', 'tipo_comercio', 'icono_emoji', 'telefono_contacto']) {
        if (tiene(colsComercio, c)) comercioReturning.push(c);
      }

      const comercioInsert = await client.query(
        `INSERT INTO piku_comercios (${comercioCampos.join(', ')})
         VALUES (${comercioPlaceholders})
         RETURNING ${comercioReturning.join(', ')}`,
        comercioVals
      );
      const comercio = comercioInsert.rows[0];

      const usuarioCampos = ['email', 'password_hash', 'nombre', 'rol'];
      const usuarioVals = [google.email, hash, nombre, 'comercio'];
      if (tiene(colsUsuario, 'telefono')) {
        usuarioCampos.push('telefono');
        usuarioVals.push(telefonoComercio);
      }
      if (tiene(colsUsuario, 'comercio_id')) {
        usuarioCampos.push('comercio_id');
        usuarioVals.push(comercio.id);
      }
      agregarCamposDireccionUsuario(colsUsuario, usuarioCampos, usuarioVals, direccionData);
      if (tiene(colsUsuario, 'google_id')) {
        usuarioCampos.push('google_id');
        usuarioVals.push(google.googleId);
      }
      if (tiene(colsUsuario, 'avatar_url') && google.picture) {
        usuarioCampos.push('avatar_url');
        usuarioVals.push(google.picture);
      }

      const usuarioReturning = ['id', 'email', 'nombre', 'rol'];
      if (tiene(colsUsuario, 'comercio_id')) usuarioReturning.push('comercio_id');

      const usuarioInsert = await client.query(
        `INSERT INTO piku_usuarios (${usuarioCampos.join(', ')})
         VALUES (${usuarioVals.map((_, i) => `$${i + 1}`).join(', ')})
         RETURNING ${usuarioReturning.join(', ')}`,
        usuarioVals
      );
      const usuario = normalizarUsuario(usuarioInsert.rows[0]);

      const linkCol = tiene(colsComercio, 'usuario_id')
        ? 'usuario_id'
        : tiene(colsComercio, 'owner_usuario_id')
          ? 'owner_usuario_id'
          : null;
      if (linkCol) {
        await client.query(`UPDATE piku_comercios SET ${linkCol} = $1 WHERE id = $2`, [
          usuario.id,
          comercio.id,
        ]);
      }

      try {
        await client.query(
          `INSERT INTO piku_reglas_puntos (comercio_id, puntos_por_peso, monto_minimo, puntos_fijos, max_puntos_por_dia)
           VALUES ($1, 1, 0, 10, 500)
           ON CONFLICT (comercio_id) DO NOTHING`,
          [comercio.id]
        );
      } catch (_e) {
        /* opcional */
      }

      if (codigoInvitacion && tiene(colsInv, 'usado')) {
        const sets = ['usado = TRUE'];
        if (tiene(colsInv, 'comercio_id')) sets.push('comercio_id = $1');
        await client.query(
          `UPDATE piku_invitaciones_comercio SET ${sets.join(', ')} WHERE UPPER(codigo) = UPPER($${tiene(colsInv, 'comercio_id') ? '2' : '1'})`,
          tiene(colsInv, 'comercio_id') ? [comercio.id, codigoInvitacion] : [codigoInvitacion]
        );
      }

      return { usuario, comercio };
    });

    const token = signToken({
      userId: resultado.usuario.id,
      rol: resultado.usuario.rol,
      comercioId: resultado.usuario.comercio_id,
    });

    await publicarComercioEnOsm(resultado.comercio);

    return res.status(201).json({
      mensaje: 'Comercio registrado con Google',
      token,
      usuario: resultado.usuario,
      comercio: resultado.comercio,
    });
  } catch (error) {
    console.error('registroComercioGoogle:', error);
    const status = error.status || (/invitación/i.test(error.message) ? 403 : 500);
    return responderError(res, status, error.message || 'Error al registrar comercio con Google');
  }
}

/**
 * Login o registro con Google (idToken de Sign-In).
 */
async function loginGoogle(req, res) {
  try {
    const idToken = String(req.body.idToken || req.body.id_token || '').trim();
    if (!idToken) return responderError(res, 400, 'Token de Google requerido');

    const google = await verificarIdTokenGoogle(idToken);
    const email = google.email;
    const googleId = google.googleId;
    const nombre = google.nombre;
    const picture = google.picture;

    const { selectCols, tieneGoogleId } = await prepararConsultaUsuario();
    const cols = await columnasTabla('piku_usuarios');

    let result = await query(
      `SELECT ${selectCols} FROM piku_usuarios
       WHERE ${tieneGoogleId ? 'google_id = $1 OR ' : ''}LOWER(email) = $${tieneGoogleId ? '2' : '1'} LIMIT 1`,
      tieneGoogleId ? [googleId, email] : [email]
    );

    let usuario;
    if (!result.rows.length) {
      const hash = await bcrypt.hash(`google:${googleId}`, 4);
      const insertCampos = ['email', 'password_hash', 'nombre', 'rol'];
      const insertVals = [email, hash, nombre, 'cliente'];
      if (tieneGoogleId) {
        insertCampos.push('google_id');
        insertVals.push(googleId);
      }
      if (tiene(cols, 'avatar_url')) {
        insertCampos.push('avatar_url');
        insertVals.push(picture || null);
      }
      const insert = await query(
        `INSERT INTO piku_usuarios (${insertCampos.join(', ')})
         VALUES (${insertVals.map((_, i) => `$${i + 1}`).join(', ')})
         RETURNING ${selectCols}`,
        insertVals
      );
      usuario = normalizarUsuario(insert.rows[0]);
      withTransaction(async (client) => otorgarBonoBienvenida(client, usuario.id)).catch((e) => {
        console.warn('bono bienvenida loginGoogle async:', e.message);
      });
    } else {
      usuario = normalizarUsuario(result.rows[0]);
      if (!usuario.activo) return responderError(res, 403, 'Cuenta desactivada');
      const updates = [];
      const vals = [];
      if (tieneGoogleId && !usuario.google_id) {
        updates.push(`google_id = $${vals.length + 1}`);
        vals.push(googleId);
        usuario.google_id = googleId;
      }
      if (picture && !usuario.avatar_url && tiene(cols, 'avatar_url')) {
        updates.push(`avatar_url = $${vals.length + 1}`);
        vals.push(picture);
        usuario.avatar_url = picture;
      }
      if (updates.length) {
        vals.push(usuario.id);
        query(
          `UPDATE piku_usuarios SET ${updates.join(', ')}, updated_at = NOW() WHERE id = $${vals.length}`,
          vals
        ).catch(() => {});
      }
    }

    const token = signToken({
      userId: usuario.id,
      rol: usuario.rol,
      comercioId: usuario.comercio_id,
    });

    return res.json({
      mensaje: 'Sesión con Google iniciada',
      token,
      usuario,
    });
  } catch (error) {
    console.error('loginGoogle:', error);
    const detail = error.response?.data?.error_description || error.message;
    return responderError(res, 401, 'No se pudo iniciar sesión con Google', { detail });
  }
}

/**
 * Perfil del usuario autenticado.
 */
async function perfil(req, res) {
  try {
    const { password_hash, ...usuario } = req.user;
    let comercio = null;

    if (usuario.comercio_id) {
      const c = await query(
        'SELECT id, usuario_id, nombre, direccion, lat, lon, logo_url, suscripcion_activa, created_at FROM piku_comercios WHERE id = $1',
        [usuario.comercio_id]
      );
      comercio = c.rows[0] || null;
    }

    return res.json({ usuario, comercio });
  } catch (error) {
    console.error('perfil:', error);
    return responderError(res, 500, 'Error al obtener perfil');
  }
}

/**
 * Actualizar perfil del usuario autenticado.
 */
async function actualizarPerfil(req, res) {
  try {
    const nombre = req.body.nombre != null ? sanitizarInput(req.body.nombre, 255) : undefined;
    const telefono = req.body.telefono != null ? sanitizarInput(req.body.telefono, 50) : undefined;
    const avatarUrl =
      req.body.avatarUrl != null
        ? sanitizarInput(req.body.avatarUrl, 500)
        : req.body.avatar_url != null
          ? sanitizarInput(req.body.avatar_url, 500)
          : undefined;
    const password = req.body.password != null ? String(req.body.password) : undefined;
    const direccionEntrega =
      req.body.direccionEntrega != null
        ? sanitizarInput(req.body.direccionEntrega, 500)
        : req.body.direccion_entrega != null
          ? sanitizarInput(req.body.direccion_entrega, 500)
          : undefined;
    const ciudad = req.body.ciudad != null ? sanitizarInput(req.body.ciudad, 100) : undefined;
    const provincia = req.body.provincia != null ? sanitizarInput(req.body.provincia, 100) : undefined;
    const codigoPostal =
      req.body.codigoPostal != null
        ? sanitizarInput(req.body.codigoPostal, 20)
        : req.body.codigo_postal != null
          ? sanitizarInput(req.body.codigo_postal, 20)
          : undefined;
    const notasEntrega =
      req.body.notasEntrega != null
        ? sanitizarInput(req.body.notasEntrega, 300)
        : req.body.notas_entrega != null
          ? sanitizarInput(req.body.notas_entrega, 300)
          : undefined;

    const campos = [];
    const valores = [];
    let idx = 1;

    if (nombre) {
      campos.push(`nombre = $${idx++}`);
      valores.push(nombre);
    }
    if (telefono !== undefined) {
      campos.push(`telefono = $${idx++}`);
      valores.push(telefono || null);
    }
    if (avatarUrl !== undefined) {
      campos.push(`avatar_url = $${idx++}`);
      valores.push(avatarUrl || null);
    }
    if (direccionEntrega !== undefined) {
      campos.push(`direccion_entrega = $${idx++}`);
      valores.push(direccionEntrega || null);
    }
    if (ciudad !== undefined) {
      campos.push(`ciudad = $${idx++}`);
      valores.push(ciudad || null);
    }
    if (provincia !== undefined) {
      campos.push(`provincia = $${idx++}`);
      valores.push(provincia || null);
    }
    if (codigoPostal !== undefined) {
      campos.push(`codigo_postal = $${idx++}`);
      valores.push(codigoPostal || null);
    }
    if (notasEntrega !== undefined) {
      campos.push(`notas_entrega = $${idx++}`);
      valores.push(notasEntrega || null);
    }
    if (password) {
      if (password.length < 6) return responderError(res, 400, 'Contraseña demasiado corta');
      campos.push(`password_hash = $${idx++}`);
      valores.push(await bcrypt.hash(password, 10));
    }

    if (!campos.length) return responderError(res, 400, 'No hay datos para actualizar');

    campos.push('updated_at = NOW()');
    valores.push(req.user.id);

    const sql = `UPDATE piku_usuarios SET ${campos.join(', ')} WHERE id = $${idx}
                 RETURNING id, email, nombre, telefono, rol, avatar_url, puntos_saldo, comercio_id,
                           direccion_entrega, ciudad, provincia, codigo_postal, notas_entrega`;
    const updated = await query(sql, valores);

    return res.json({
      mensaje: 'Perfil actualizado',
      usuario: updated.rows[0],
    });
  } catch (error) {
    console.error('actualizarPerfil:', error);
    return responderError(res, 500, 'Error al actualizar perfil');
  }
}

module.exports = {
  registroCliente,
  registroComercio,
  registroComercioGoogle,
  login,
  loginGoogle,
  perfil,
  actualizarPerfil,
};
