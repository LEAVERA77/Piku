/**
 * Restringe acceso según rol del usuario autenticado.
 */
function requireRole(...rolesPermitidos) {
  const normalizados = rolesPermitidos.map((r) => String(r).toLowerCase());

  return (req, res, next) => {
    const rol = String(req.user?.rol || '').toLowerCase();
    if (!req.user || !normalizados.includes(rol)) {
      return res.status(403).json({
        error: 'No tenés permiso para esta acción',
        detalle: `Se requiere rol: ${rolesPermitidos.join(' o ')}. Tu rol actual: ${rol || 'desconocido'}`,
        rolRequerido: rolesPermitidos,
      });
    }
    return next();
  };
}

const soloCliente = requireRole('cliente');
const soloComercio = requireRole('comercio');
const soloAdmin = requireRole('admin');

module.exports = { requireRole, soloCliente, soloComercio, soloAdmin };
