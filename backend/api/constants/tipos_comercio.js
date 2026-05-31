const TIPOS_COMERCIO = [
  { id: 'cafeteria', label: 'Cafetería', emoji: '☕', categoria: 'cafeteria' },
  { id: 'restaurante', label: 'Restaurante', emoji: '🍽️', categoria: 'restaurant' },
  { id: 'ropa', label: 'Tienda de ropa', emoji: '👕', categoria: 'indumentaria' },
  { id: 'supermercado', label: 'Supermercado', emoji: '🛒', categoria: 'supermercado' },
  { id: 'farmacia', label: 'Farmacia', emoji: '💊', categoria: 'farmacia' },
  { id: 'peluqueria', label: 'Peluquería', emoji: '✂️', categoria: 'servicios' },
  { id: 'libreria', label: 'Librería', emoji: '📚', categoria: 'otros' },
  { id: 'otro', label: 'Otro', emoji: '🏪', categoria: 'otros' },
];

function normalizarTipoComercio(raw) {
  const id = String(raw || 'otro')
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
  const found = TIPOS_COMERCIO.find((t) => t.id === id || t.categoria === id);
  return found || TIPOS_COMERCIO.find((t) => t.id === 'otro');
}

function emojiParaTipo(raw) {
  return normalizarTipoComercio(raw).emoji;
}

module.exports = { TIPOS_COMERCIO, normalizarTipoComercio, emojiParaTipo };
