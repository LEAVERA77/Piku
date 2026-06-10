const { query } = require('./neon.service');
const { columnasTabla, tiene } = require('../utils/schema.util');
const crypto = require('crypto');
const {
  BONO_BIENVENIDA,
  BONO_CUMPLEANOS,
  BONO_COMPARTIR,
  BONO_REFERIDO_INVITADOR,
  BONO_REFERIDO_INVITADO,
} = require('../constants/puntos.constants');
const { obtenerPesosPorDolar } = require('./dolar.service');
const {
  calcularPuntosDesdePesos,
  calcularDescuentoArs,
  resumenPikuPoints,
  VALOR_DE_1_PUNTO_USD,
} = require('../utils/pikuPoints.util');

function saldoSeguro(valor) {
  const n = parseInt(valor, 10);
  if (Number.isNaN(n) || n < 0) return 0;
  return n;
}

function generarCodigoReferido() {
  return crypto.randomBytes(4).toString('hex').toUpperCase();
}

/**
 * Piku Points: 1 PP por cada 1 USD gastado (según cotización blue del día),
 * ajustado por las reglas del comercio:
 * - activo=false  → el programa está pausado, no se otorgan puntos.
 * - monto_minimo  → compras menores no suman.
 * - puntos_por_peso → multiplicador sobre los PP base (ej. 2 = puntos dobles).
 * - puntos_fijos  → bono fijo por visita/compra que califica.
 */
async function calcularPuntosCompra(montoTransaccion, reglas = null) {
  if (reglas && reglas.activo === false) return 0;

  const monto = parseFloat(montoTransaccion) || 0;
  const minimo = parseFloat(reglas?.monto_minimo) || 0;
  if (monto < minimo) return 0;

  const pesosPorDolar = await obtenerPesosPorDolar();
  const base = calcularPuntosDesdePesos(monto, pesosPorDolar);

  const multiplicador = parseFloat(reglas?.puntos_por_peso);
  const conMultiplicador =
    Number.isFinite(multiplicador) && multiplicador > 0
      ? Math.floor(base * multiplicador)
      : base;

  const fijos = parseInt(reglas?.puntos_fijos, 10);
  const bonoFijo = Number.isFinite(fijos) && fijos > 0 ? fijos : 0;

  return conMultiplicador + bonoFijo;
}

async function resumenSaldoPuntos(puntos) {
  const pesosPorDolar = await obtenerPesosPorDolar();
  return resumenPikuPoints(puntos, pesosPorDolar);
}

async function resumenPuntosGanados(puntosGanados) {
  const pesosPorDolar = await obtenerPesosPorDolar();
  const valorCanjeArs = calcularDescuentoArs(puntosGanados, pesosPorDolar);
  return {
    puntos: puntosGanados,
    pesosPorDolar,
    valorPuntoUsd: VALOR_DE_1_PUNTO_USD,
    valorCanjeArs,
  };
}

async function ejecutarQuery(clientOrPool, text, params) {
  if (clientOrPool && typeof clientOrPool.query === 'function') {
    return clientOrPool.query(text, params);
  }
  return query(text, params);
}

/**
 * Suma puntos y registra transacción (tipo ganado).
 */
async function acreditarPuntos(client, { usuarioId, comercioId, puntos, descripcion, extras = {} }) {
  const pts = parseInt(puntos, 10) || 0;
  if (pts <= 0) return { puntos: 0, saldo: null };

  const user = await client.query(
    'SELECT puntos_saldo FROM piku_usuarios WHERE id = $1 FOR UPDATE',
    [usuarioId]
  );
  if (!user.rows.length) throw new Error('Usuario no encontrado');

  const saldoAnterior = saldoSeguro(user.rows[0].puntos_saldo);
  const nuevoSaldo = saldoAnterior + pts;

  await client.query(
    'UPDATE piku_usuarios SET puntos_saldo = $1, updated_at = NOW() WHERE id = $2',
    [nuevoSaldo, usuarioId]
  );

  const campos = ['usuario_id', 'comercio_id', 'tipo', 'puntos', 'descripcion'];
  const vals = [usuarioId, comercioId || null, 'ganado', pts, descripcion];
  if (extras.qrCodigoId) {
    campos.push('qr_codigo_id');
    vals.push(extras.qrCodigoId);
  }
  if (extras.recompensaId) {
    campos.push('recompensa_id');
    vals.push(extras.recompensaId);
  }

  await client.query(
    `INSERT INTO piku_transacciones_puntos (${campos.join(', ')})
     VALUES (${vals.map((_, i) => `$${i + 1}`).join(', ')})`,
    vals
  );

  return { puntos: pts, saldo: nuevoSaldo };
}

/**
 * Descuenta puntos sin permitir saldo negativo.
 */
async function debitarPuntos(client, {
  usuarioId,
  comercioId = null,
  puntos,
  descripcion,
  extras = {},
}) {
  const pts = Math.abs(parseInt(puntos, 10) || 0);
  if (pts <= 0) return { puntos: 0, saldo: null };

  const user = await client.query(
    'SELECT puntos_saldo FROM piku_usuarios WHERE id = $1 FOR UPDATE',
    [usuarioId]
  );
  if (!user.rows.length) throw new Error('Usuario no encontrado');

  const saldoAnterior = saldoSeguro(user.rows[0].puntos_saldo);
  if (saldoAnterior < pts) throw new Error('Puntos insuficientes');

  const nuevoSaldo = saldoAnterior - pts;
  await client.query(
    'UPDATE piku_usuarios SET puntos_saldo = $1, updated_at = NOW() WHERE id = $2',
    [nuevoSaldo, usuarioId]
  );

  const campos = ['usuario_id', 'comercio_id', 'tipo', 'puntos', 'descripcion'];
  const vals = [usuarioId, comercioId, 'canjeado', -pts, descripcion];
  if (extras.recompensaId) {
    campos.push('recompensa_id');
    vals.push(extras.recompensaId);
  }
  if (extras.codigoCanje) {
    campos.push('codigo_canje');
    vals.push(extras.codigoCanje);
  }

  await client.query(
    `INSERT INTO piku_transacciones_puntos (${campos.join(', ')})
     VALUES (${vals.map((_, i) => `$${i + 1}`).join(', ')})`,
    vals
  );

  return { puntos: pts, saldo: nuevoSaldo };
}

async function asegurarCodigoReferido(client, usuarioId) {
  const cols = await columnasTabla('piku_usuarios');
  if (!tiene(cols, 'codigo_referido')) return null;

  const actual = await client.query(
    'SELECT codigo_referido FROM piku_usuarios WHERE id = $1',
    [usuarioId]
  );
  if (actual.rows[0]?.codigo_referido) return actual.rows[0].codigo_referido;

  for (let i = 0; i < 5; i += 1) {
    const codigo = generarCodigoReferido();
    try {
      await client.query(
        'UPDATE piku_usuarios SET codigo_referido = $1, updated_at = NOW() WHERE id = $2 AND codigo_referido IS NULL',
        [codigo, usuarioId]
      );
      return codigo;
    } catch (_err) {
      // colisión de código único
    }
  }
  return null;
}

async function yaRecibioBonificacion(client, usuarioId, patronDescripcion) {
  const r = await client.query(
    `SELECT id FROM piku_transacciones_puntos
     WHERE usuario_id = $1 AND descripcion ILIKE $2
     LIMIT 1`,
    [usuarioId, patronDescripcion]
  );
  return r.rows.length > 0;
}

async function otorgarBonoBienvenida(client, usuarioId) {
  const cols = await columnasTabla('piku_usuarios');
  if (tiene(cols, 'bono_bienvenida_otorgado')) {
    const u = await client.query(
      'SELECT bono_bienvenida_otorgado FROM piku_usuarios WHERE id = $1',
      [usuarioId]
    );
    if (u.rows[0]?.bono_bienvenida_otorgado) {
      return { otorgado: false, puntos: 0 };
    }
  }

  if (await yaRecibioBonificacion(client, usuarioId, '%bienvenida%')) {
    return { otorgado: false, puntos: 0 };
  }

  const { puntos, saldo } = await acreditarPuntos(client, {
    usuarioId,
    comercioId: null,
    puntos: BONO_BIENVENIDA,
    descripcion: `Bono de bienvenida — ${BONO_BIENVENIDA} PP 🎉`,
  });

  if (tiene(cols, 'bono_bienvenida_otorgado')) {
    await client.query(
      'UPDATE piku_usuarios SET bono_bienvenida_otorgado = TRUE WHERE id = $1',
      [usuarioId]
    );
  }

  return { otorgado: true, puntos, saldo };
}

async function otorgarBonoCompartir(usuarioId) {
  const prev = await query(
    `SELECT id FROM piku_transacciones_puntos
     WHERE usuario_id = $1 AND descripcion ILIKE '%compartir%'
       AND created_at >= CURRENT_DATE
     LIMIT 1`,
    [usuarioId]
  );
  if (prev.rows.length) {
    return { otorgado: false, mensaje: 'Ya recibiste puntos por compartir hoy' };
  }

  const { withTransaction } = require('./neon.service');
  const resultado = await withTransaction(async (client) =>
    acreditarPuntos(client, {
      usuarioId,
      comercioId: null,
      puntos: BONO_COMPARTIR,
      descripcion: `Compartir Piku — ${BONO_COMPARTIR} PP 📱`,
    })
  );

  return {
    otorgado: true,
    puntos: resultado.puntos,
    saldo: resultado.saldo,
    mensaje: `¡Sumaste ${resultado.puntos} PP por compartir!`,
  };
}

async function otorgarBonoInvitacion(client, invitadorId, invitadoId) {
  const invitador = await acreditarPuntos(client, {
    usuarioId: invitadorId,
    comercioId: null,
    puntos: BONO_REFERIDO_INVITADOR,
    descripcion: `Invitaste a un amigo — ${BONO_REFERIDO_INVITADOR} PP 👥`,
  });
  const invitado = await acreditarPuntos(client, {
    usuarioId: invitadoId,
    comercioId: null,
    puntos: BONO_REFERIDO_INVITADO,
    descripcion: `Te invitaron a Piku — ${BONO_REFERIDO_INVITADO} PP 👥`,
  });
  return { invitador, invitado };
}

async function otorgarBonosCumpleanos() {
  const cols = await columnasTabla('piku_usuarios');
  if (!tiene(cols, 'fecha_nacimiento')) {
    console.log('ℹ️ Puntos cumpleaños: columna fecha_nacimiento no disponible');
    return 0;
  }

  const hoy = await query(
    `SELECT id, email FROM piku_usuarios
     WHERE rol = 'cliente'
       AND activo = TRUE
       AND fecha_nacimiento IS NOT NULL
       AND EXTRACT(MONTH FROM fecha_nacimiento) = EXTRACT(MONTH FROM CURRENT_DATE)
       AND EXTRACT(DAY FROM fecha_nacimiento) = EXTRACT(DAY FROM CURRENT_DATE)`,
    []
  );

  let otorgados = 0;
  const { withTransaction } = require('./neon.service');

  for (const user of hoy.rows) {
    const ya = await query(
      `SELECT id FROM piku_transacciones_puntos
       WHERE usuario_id = $1 AND descripcion ILIKE '%cumpleaños%'
         AND created_at >= CURRENT_DATE
       LIMIT 1`,
      [user.id]
    );
    if (ya.rows.length) continue;

    try {
      await withTransaction(async (client) => {
        await acreditarPuntos(client, {
          usuarioId: user.id,
          comercioId: null,
          puntos: BONO_CUMPLEANOS,
          descripcion: `🎂 ¡Feliz cumpleaños! Te regalamos ${BONO_CUMPLEANOS} PP`,
        });
      });
      otorgados += 1;
      console.log(`🎂 Bono cumpleaños: ${user.email}`);
    } catch (err) {
      console.warn(`Cumpleaños ${user.id}:`, err.message);
    }
  }

  return otorgados;
}

async function procesarInvitacionAmigo(client, invitadoId, codigoAmigo) {
  if (!codigoAmigo) return { otorgado: false };

  const cols = await columnasTabla('piku_usuarios');
  const codigo = String(codigoAmigo).trim().toUpperCase();
  if (!codigo) return { otorgado: false };

  let invitadorRes;
  if (tiene(cols, 'codigo_referido')) {
    invitadorRes = await client.query(
      `SELECT id FROM piku_usuarios
       WHERE (codigo_referido = $1 OR id::text = $1)
         AND id <> $2 AND rol = 'cliente' AND activo = TRUE
       LIMIT 1`,
      [codigo, invitadoId]
    );
  } else {
    invitadorRes = await client.query(
      `SELECT id FROM piku_usuarios
       WHERE id::text = $1 AND id <> $2 AND rol = 'cliente' AND activo = TRUE
       LIMIT 1`,
      [codigo, invitadoId]
    );
  }

  if (!invitadorRes.rows.length) return { otorgado: false };

  const invitadorId = invitadorRes.rows[0].id;
  const ya = await client.query(
    `SELECT id FROM piku_transacciones_puntos
     WHERE usuario_id = $1 AND descripcion ILIKE '%invitaron%'
     LIMIT 1`,
    [invitadoId]
  );
  if (ya.rows.length) return { otorgado: false };

  return { otorgado: true, ...(await otorgarBonoInvitacion(client, invitadorId, invitadoId)) };
}

module.exports = {
  BONO_BIENVENIDA,
  BONO_CUMPLEANOS,
  BONO_COMPARTIR,
  BONO_REFERIDO_INVITADOR,
  BONO_REFERIDO_INVITADO,
  saldoSeguro,
  calcularPuntosCompra,
  resumenSaldoPuntos,
  resumenPuntosGanados,
  acreditarPuntos,
  debitarPuntos,
  asegurarCodigoReferido,
  otorgarBonoBienvenida,
  otorgarBonoCompartir,
  otorgarBonoInvitacion,
  procesarInvitacionAmigo,
  otorgarBonosCumpleanos,
};
