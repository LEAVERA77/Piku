const { query, withTransaction } = require('./neon.service');
const { columnasTabla, tiene } = require('../utils/schema.util');
const { acreditarPuntos } = require('./puntos.service');

const TIPOS_VALIDOS = new Set([
  'escaneos_totales',
  'escaneos_rubros',
  'escaneos_mismo_comercio',
  'dias_consecutivos',
]);

async function tablaDesafiosDisponible() {
  try {
    const cols = await columnasTabla('piku_desafios');
    return cols.size > 0;
  } catch {
    return false;
  }
}

function inicioSemanaActual() {
  return `date_trunc('week', CURRENT_DATE)::date`;
}

function finSemanaActual() {
  return `(date_trunc('week', CURRENT_DATE) + interval '6 days')::date`;
}

/** Renueva vigencia de desafíos semanales si la semana expiró. */
async function asegurarDesafiosSemanales() {
  if (!(await tablaDesafiosDisponible())) return;
  await query(
    `UPDATE piku_desafios
     SET vigencia_desde = ${inicioSemanaActual()},
         vigencia_hasta = ${finSemanaActual()}
     WHERE activo = TRUE
       AND (vigencia_hasta IS NULL OR vigencia_hasta < CURRENT_DATE)`
  );
}

function rubroSql(alias = 'c') {
  return `COALESCE(NULLIF(${alias}.tipo_comercio, ''), NULLIF(${alias}.categoria, ''), 'otro')`;
}

async function escaneosEnPeriodo(usuarioId, desde, hasta) {
  const cols = await columnasTabla('piku_transacciones_puntos');
  if (!tiene(cols, 'qr_codigo_id')) {
    return [];
  }
  const result = await query(
    `SELECT t.id, t.comercio_id, t.created_at::date AS dia, ${rubroSql('c')} AS rubro
     FROM piku_transacciones_puntos t
     LEFT JOIN piku_comercios c ON c.id = t.comercio_id
     WHERE t.usuario_id = $1
       AND t.tipo = 'ganado'
       AND t.qr_codigo_id IS NOT NULL
       AND t.created_at::date BETWEEN $2::date AND $3::date
     ORDER BY t.created_at ASC`,
    [usuarioId, desde, hasta]
  );
  return result.rows;
}

function calcularProgreso(tipo, objetivo, escaneos) {
  switch (tipo) {
    case 'escaneos_totales':
      return Math.min(objetivo, escaneos.length);
    case 'escaneos_rubros': {
      const rubros = new Set(escaneos.map((e) => e.rubro).filter(Boolean));
      return Math.min(objetivo, rubros.size);
    }
    case 'escaneos_mismo_comercio': {
      const porComercio = {};
      escaneos.forEach((e) => {
        if (!e.comercio_id) return;
        porComercio[e.comercio_id] = (porComercio[e.comercio_id] || 0) + 1;
      });
      const max = Object.values(porComercio).reduce((m, n) => Math.max(m, n), 0);
      return Math.min(objetivo, max);
    }
    case 'dias_consecutivos': {
      const diasUnicos = [...new Set(escaneos.map((e) => String(e.dia)))].sort();
      if (!diasUnicos.length) return 0;
      let mejor = 1;
      let actual = 1;
      for (let i = 1; i < diasUnicos.length; i += 1) {
        const prev = new Date(diasUnicos[i - 1]);
        const cur = new Date(diasUnicos[i]);
        const diff = (cur - prev) / (1000 * 60 * 60 * 24);
        if (diff === 1) {
          actual += 1;
          mejor = Math.max(mejor, actual);
        } else if (diff > 1) {
          actual = 1;
        }
      }
      return Math.min(objetivo, mejor);
    }
    default:
      return 0;
  }
}

async function yaCompletadoEnVigencia(usuarioId, desafioId, desde, hasta) {
  const r = await query(
    `SELECT 1 FROM piku_desafios_completados
     WHERE usuario_id = $1 AND desafio_id = $2
       AND fecha_completado::date BETWEEN $3::date AND $4::date
     LIMIT 1`,
    [usuarioId, desafioId, desde, hasta]
  );
  return r.rows.length > 0;
}

async function listarDesafiosConProgreso(usuarioId) {
  if (!(await tablaDesafiosDisponible())) {
    return [];
  }
  await asegurarDesafiosSemanales();

  const desafios = await query(
    `SELECT id, titulo, descripcion, tipo, objetivo, recompensa, vigencia_desde, vigencia_hasta
     FROM piku_desafios
     WHERE activo = TRUE
       AND (vigencia_desde IS NULL OR vigencia_desde <= CURRENT_DATE)
       AND (vigencia_hasta IS NULL OR vigencia_hasta >= CURRENT_DATE)
     ORDER BY recompensa ASC`
  );

  const items = [];
  for (const d of desafios.rows) {
    const desde = d.vigencia_desde || new Date().toISOString().slice(0, 10);
    const hasta = d.vigencia_hasta || new Date().toISOString().slice(0, 10);
    const escaneos = await escaneosEnPeriodo(usuarioId, desde, hasta);
    const progreso = calcularProgreso(d.tipo, d.objetivo, escaneos);
    const completado = await yaCompletadoEnVigencia(usuarioId, d.id, desde, hasta);
    items.push({
      id: d.id,
      titulo: d.titulo,
      descripcion: d.descripcion,
      tipo: d.tipo,
      objetivo: d.objetivo,
      recompensa: d.recompensa,
      progreso,
      completado,
      listoParaCompletar: !completado && progreso >= d.objetivo,
      vigenciaDesde: d.vigencia_desde,
      vigenciaHasta: d.vigencia_hasta,
    });
  }
  return items;
}

async function completarDesafio(usuarioId, desafioId) {
  if (!(await tablaDesafiosDisponible())) {
    throw new Error('Desafíos no disponibles');
  }

  const desafioRes = await query(
    `SELECT id, titulo, tipo, objetivo, recompensa, vigencia_desde, vigencia_hasta
     FROM piku_desafios
     WHERE id = $1 AND activo = TRUE`,
    [desafioId]
  );
  if (!desafioRes.rows.length) throw new Error('Desafío no encontrado');
  const d = desafioRes.rows[0];

  const desde = d.vigencia_desde || new Date().toISOString().slice(0, 10);
  const hasta = d.vigencia_hasta || new Date().toISOString().slice(0, 10);

  if (await yaCompletadoEnVigencia(usuarioId, desafioId, desde, hasta)) {
    throw new Error('Ya completaste este desafío esta semana');
  }

  const escaneos = await escaneosEnPeriodo(usuarioId, desde, hasta);
  const progreso = calcularProgreso(d.tipo, d.objetivo, escaneos);
  if (progreso < d.objetivo) {
    throw new Error(`Progreso insuficiente (${progreso}/${d.objetivo})`);
  }

  return withTransaction(async (client) => {
    await client.query(
      `INSERT INTO piku_desafios_completados (usuario_id, desafio_id)
       VALUES ($1, $2)`,
      [usuarioId, desafioId]
    );

    const { puntos, saldo } = await acreditarPuntos(client, {
      usuarioId,
      comercioId: null,
      puntos: d.recompensa,
      descripcion: `Desafío completado: ${d.titulo}`,
    });

    return {
      mensaje: `¡Desafío "${d.titulo}" completado! +${d.recompensa} PP`,
      puntosOtorgados: puntos,
      saldo,
      desafio: d.titulo,
    };
  });
}

module.exports = {
  listarDesafiosConProgreso,
  completarDesafio,
  asegurarDesafiosSemanales,
  TIPOS_VALIDOS,
};
