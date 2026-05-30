/**
 * Catálogo de rubros para filtros del mapa (slug → etiquetas de categoría en BD).
 */
const RUBROS = [
  { id: 'gastronomia', label: 'Gastronomía', categorias: ['gastronomia', 'restaurant', 'comida', 'bar'] },
  { id: 'cafeteria', label: 'Cafeterías', categorias: ['cafeteria', 'cafe', 'café'] },
  { id: 'indumentaria', label: 'Indumentaria', categorias: ['indumentaria', 'ropa', 'moda'] },
  { id: 'farmacia', label: 'Farmacia', categorias: ['farmacia', 'salud'] },
  { id: 'supermercado', label: 'Supermercados', categorias: ['supermercado', 'super', 'almacen'] },
  { id: 'otros', label: 'Otros', categorias: ['otros', 'general', 'servicios'] },
];

function normalizarCategoria(raw) {
  if (!raw) return 'otros';
  return String(raw)
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}

function rubroIdDesdeCategoria(categoria) {
  const cat = normalizarCategoria(categoria);
  const found = RUBROS.find((r) => r.categorias.some((c) => cat === c || cat.includes(c)));
  return found ? found.id : 'otros';
}

function categoriasParaRubros(rubroIds) {
  if (!rubroIds?.length) return null;
  const set = new Set();
  for (const id of rubroIds) {
    const rubro = RUBROS.find((r) => r.id === id);
    if (rubro) rubro.categorias.forEach((c) => set.add(c));
  }
  return set.size ? [...set] : null;
}

module.exports = { RUBROS, normalizarCategoria, rubroIdDesdeCategoria, categoriasParaRubros };
