const { query } = require('../services/neon.service');

async function resolveComercioId(req) {
  if (req.user?.comercio_id) return req.user.comercio_id;

  const porUsuario = await query(
    'SELECT id FROM piku_comercios WHERE usuario_id = $1 LIMIT 1',
    [req.user.id]
  );
  if (!porUsuario.rows.length) return null;

  const comercioId = porUsuario.rows[0].id;
  req.user.comercio_id = comercioId;
  await query(
    'UPDATE piku_usuarios SET comercio_id = $1, updated_at = NOW() WHERE id = $2 AND comercio_id IS NULL',
    [comercioId, req.user.id]
  ).catch(() => {});
  return comercioId;
}

module.exports = { resolveComercioId };
