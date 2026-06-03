const { query } = require('../services/neon.service');
const { columnasTabla, tiene } = require('./schema.util');

async function sincronizarVinculo(usuarioId, comercioId) {
  const colsU = await columnasTabla('piku_usuarios');
  const colsC = await columnasTabla('piku_comercios');

  if (tiene(colsU, 'comercio_id')) {
    await query(
      `UPDATE piku_usuarios SET comercio_id = $1, updated_at = NOW()
       WHERE id = $2 AND (comercio_id IS NULL OR comercio_id = $1)`,
      [comercioId, usuarioId]
    ).catch(() => {});
  }
  if (tiene(colsC, 'usuario_id')) {
    await query(
      `UPDATE piku_comercios SET usuario_id = $1, updated_at = NOW()
       WHERE id = $2 AND (usuario_id IS NULL OR usuario_id = $1)`,
      [usuarioId, comercioId]
    ).catch(() => {});
  }
  if (tiene(colsC, 'owner_usuario_id')) {
    await query(
      `UPDATE piku_comercios SET owner_usuario_id = $1, updated_at = NOW()
       WHERE id = $2 AND (owner_usuario_id IS NULL OR owner_usuario_id = $1)`,
      [usuarioId, comercioId]
    ).catch(() => {});
  }
}

async function resolveComercioId(req) {
  if (!req.user?.id) return null;

  if (req.user.comercio_id) {
    await sincronizarVinculo(req.user.id, req.user.comercio_id);
    return req.user.comercio_id;
  }

  const cols = await columnasTabla('piku_comercios');
  const condiciones = [];
  if (tiene(cols, 'usuario_id')) condiciones.push('usuario_id = $1');
  if (tiene(cols, 'owner_usuario_id')) condiciones.push('owner_usuario_id = $1');

  if (condiciones.length) {
    const porUsuario = await query(
      `SELECT id FROM piku_comercios WHERE ${condiciones.join(' OR ')} LIMIT 1`,
      [req.user.id]
    );
    if (porUsuario.rows.length) {
      const comercioId = porUsuario.rows[0].id;
      req.user.comercio_id = comercioId;
      await sincronizarVinculo(req.user.id, comercioId);
      return comercioId;
    }
  }

  return null;
}

module.exports = { resolveComercioId, sincronizarVinculo };
