const bcrypt = require('bcryptjs');
const axios = require('axios');
const { query, withTransaction } = require('../services/neon.service');
const { signToken } = require('../middleware/auth.middleware');
const { validarEmail, sanitizarInput, responderError } = require('../utils/helpers');

const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID || '';

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

    const existe = await query('SELECT id FROM piku_usuarios WHERE LOWER(email) = $1', [email]);
    if (existe.rows.length) return responderError(res, 409, 'El email ya está registrado');

    const hash = await bcrypt.hash(password, 10);
    const insert = await query(
      `INSERT INTO piku_usuarios (email, password_hash, nombre, telefono, rol)
       VALUES ($1, $2, $3, $4, 'cliente')
       RETURNING id, email, nombre, telefono, rol, puntos_saldo, created_at`,
      [email, hash, nombre, telefono || null]
    );

    const usuario = insert.rows[0];
    const token = signToken({ userId: usuario.id, rol: usuario.rol });

    return res.status(201).json({
      mensaje: 'Cliente registrado correctamente',
      token,
      usuario,
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
    const direccion = sanitizarInput(req.body.direccion, 500);
    const codigoInvitacion = sanitizarInput(req.body.codigoInvitacion || req.body.codigo_invitacion, 64);
    const lat = req.body.lat != null ? parseFloat(req.body.lat) : null;
    const lon = req.body.lon != null ? parseFloat(req.body.lon) : null;
    const categoria = sanitizarInput(req.body.categoria, 50);

    if (!validarEmail(email)) return responderError(res, 400, 'Email inválido');
    if (password.length < 6) return responderError(res, 400, 'La contraseña debe tener al menos 6 caracteres');
    if (!nombre || !nombreComercio) {
      return responderError(res, 400, 'Nombre del responsable y del comercio son requeridos');
    }

    const existe = await query('SELECT id FROM piku_usuarios WHERE LOWER(email) = $1', [email]);
    if (existe.rows.length) return responderError(res, 409, 'El email ya está registrado');

    const resultado = await withTransaction(async (client) => {
      // Validar invitación si no es admin autenticado
      const esAdmin = req.user?.rol === 'admin';
      if (!esAdmin) {
        const codigoEnv = process.env.PIKU_CODIGO_INVITACION || '';
        const codigoValido =
          codigoInvitacion &&
          (codigoEnv && codigoInvitacion.toUpperCase() === codigoEnv.toUpperCase());

        if (!codigoValido) {
          if (!codigoInvitacion) throw new Error('Código de invitación requerido');
          const inv = await client.query(
            `SELECT id, usado, expires_at FROM piku_invitaciones_comercio
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

      const comercioInsert = await client.query(
        `INSERT INTO piku_comercios (nombre, direccion, lat, lon, suscripcion_activa, categoria)
         VALUES ($1, $2, $3, $4, TRUE, $5)
         RETURNING id, nombre, direccion, lat, lon, categoria`,
        [nombreComercio, direccion || null, lat, lon, categoria || null]
      );
      const comercio = comercioInsert.rows[0];

      const usuarioInsert = await client.query(
        `INSERT INTO piku_usuarios (email, password_hash, nombre, telefono, rol, comercio_id)
         VALUES ($1, $2, $3, $4, 'comercio', $5)
         RETURNING id, email, nombre, rol, comercio_id`,
        [email, hash, nombre, sanitizarInput(req.body.telefono, 50) || null, comercio.id]
      );
      const usuario = usuarioInsert.rows[0];

      await client.query(
        'UPDATE piku_comercios SET usuario_id = $1 WHERE id = $2',
        [usuario.id, comercio.id]
      );

      await client.query(
        `INSERT INTO piku_reglas_puntos (comercio_id, puntos_por_peso, monto_minimo, puntos_fijos, max_puntos_por_dia)
         VALUES ($1, 1, 0, 10, 500)
         ON CONFLICT (comercio_id) DO NOTHING`,
        [comercio.id]
      );

      if (!esAdmin && codigoInvitacion) {
        await client.query(
          `UPDATE piku_invitaciones_comercio SET usado = TRUE, comercio_id = $1 WHERE UPPER(codigo) = UPPER($2)`,
          [comercio.id, codigoInvitacion]
        );
      }

      return { usuario, comercio };
    });

    const token = signToken({
      userId: resultado.usuario.id,
      rol: resultado.usuario.rol,
      comercioId: resultado.usuario.comercio_id,
    });

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

    const result = await query(
      `SELECT id, email, nombre, telefono, rol, password_hash, activo,
              puntos_saldo, comercio_id, avatar_url
       FROM piku_usuarios WHERE LOWER(email) = $1 LIMIT 1`,
      [email]
    );

    if (!result.rows.length || !result.rows[0].activo) {
      return responderError(res, 401, 'Credenciales inválidas');
    }

    const usuario = result.rows[0];
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
 * Login o registro con Google (idToken de Sign-In).
 */
async function loginGoogle(req, res) {
  try {
    const idToken = String(req.body.idToken || req.body.id_token || '').trim();
    if (!idToken) return responderError(res, 400, 'Token de Google requerido');

    const { data } = await axios.get('https://oauth2.googleapis.com/tokeninfo', {
      params: { id_token: idToken },
      timeout: 10000,
    });

    if (GOOGLE_CLIENT_ID && data.aud !== GOOGLE_CLIENT_ID) {
      return responderError(res, 401, 'Token de Google no válido para esta app');
    }

    const email = String(data.email || '').toLowerCase();
    const googleId = String(data.sub || '');
    const nombre = sanitizarInput(data.name || email.split('@')[0], 255);

    if (!email || !googleId) {
      return responderError(res, 401, 'No se pudo verificar la cuenta de Google');
    }

    let result = await query(
      `SELECT id, email, nombre, telefono, rol, activo, puntos_saldo, comercio_id, avatar_url, google_id
       FROM piku_usuarios WHERE google_id = $1 OR LOWER(email) = $2 LIMIT 1`,
      [googleId, email]
    );

    let usuario;
    if (!result.rows.length) {
      const hash = await bcrypt.hash(`google:${googleId}:${Date.now()}`, 10);
      const insert = await query(
        `INSERT INTO piku_usuarios (email, password_hash, nombre, rol, google_id, avatar_url)
         VALUES ($1, $2, $3, 'cliente', $4, $5)
         RETURNING id, email, nombre, telefono, rol, puntos_saldo, comercio_id, avatar_url, google_id`,
        [email, hash, nombre, googleId, data.picture || null]
      );
      usuario = insert.rows[0];
    } else {
      usuario = result.rows[0];
      if (!usuario.activo) return responderError(res, 403, 'Cuenta desactivada');
      if (!usuario.google_id) {
        await query('UPDATE piku_usuarios SET google_id = $1, updated_at = NOW() WHERE id = $2', [
          googleId,
          usuario.id,
        ]);
        usuario.google_id = googleId;
      }
      if (data.picture && !usuario.avatar_url) {
        await query('UPDATE piku_usuarios SET avatar_url = $1, updated_at = NOW() WHERE id = $2', [
          data.picture,
          usuario.id,
        ]);
        usuario.avatar_url = data.picture;
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
    const avatarUrl = req.body.avatarUrl != null ? sanitizarInput(req.body.avatarUrl, 500) : undefined;
    const password = req.body.password != null ? String(req.body.password) : undefined;

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
    if (password) {
      if (password.length < 6) return responderError(res, 400, 'Contraseña demasiado corta');
      campos.push(`password_hash = $${idx++}`);
      valores.push(await bcrypt.hash(password, 10));
    }

    if (!campos.length) return responderError(res, 400, 'No hay datos para actualizar');

    campos.push('updated_at = NOW()');
    valores.push(req.user.id);

    const sql = `UPDATE piku_usuarios SET ${campos.join(', ')} WHERE id = $${idx}
                 RETURNING id, email, nombre, telefono, rol, avatar_url, puntos_saldo, comercio_id`;
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
  login,
  loginGoogle,
  perfil,
  actualizarPerfil,
};
