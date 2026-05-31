const GROQ_API_URL = 'https://api.groq.com/openai/v1/chat/completions';
const DEFAULT_MODEL = process.env.GROQ_MODEL || 'llama-3.3-70b-versatile';

function extraerTextoRespuesta(data) {
  if (!data || typeof data !== 'object') return null;
  if (typeof data.respuesta === 'string') return data.respuesta;
  if (typeof data.message === 'string') return data.message;
  if (typeof data.content === 'string') return data.content;
  if (typeof data.text === 'string') return data.text;
  const choice = data.choices?.[0];
  if (choice?.message?.content) return choice.message.content;
  if (typeof choice?.text === 'string') return choice.text;
  return null;
}

function normalizarBaseUrl(raw) {
  return String(raw || '')
    .trim()
    .replace(/^["']|["']$/g, '')
    .replace(/\\n/g, '')
    .replace(/\/$/, '');
}

async function llamarGroqDirecto(messages) {
  const apiKey = String(process.env.GROQ_API_KEY || '').trim();
  if (!apiKey) {
    return {
      ok: false,
      error: 'Configurá GROQ_API_KEY en Render para el chat de Piku.',
    };
  }

  try {
    const res = await fetch(GROQ_API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: DEFAULT_MODEL,
        messages,
        temperature: 0.6,
        max_tokens: 800,
      }),
      signal: AbortSignal.timeout(45000),
    });
    if (!res.ok) {
      const errBody = await res.text();
      return { ok: false, error: `Groq error ${res.status}: ${errBody.slice(0, 200)}` };
    }
    const data = await res.json();
    const texto = extraerTextoRespuesta(data);
    if (!texto) return { ok: false, error: 'Respuesta vacía de Groq' };
    return { ok: true, texto, via: 'groq_directo' };
  } catch (error) {
    return { ok: false, error: error.message };
  }
}

async function llamarGestorNovaProxy(baseUrl, messages, user, system) {
  const gestorPaths = ['/api/ia/chat', '/api/ia/completions', '/api/chat', '/api/piku/chat'];

  for (const path of gestorPaths) {
    try {
      const res = await fetch(`${baseUrl}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ messages, model: DEFAULT_MODEL, prompt: user, system }),
        signal: AbortSignal.timeout(45000),
      });
      if (!res.ok) continue;
      const data = await res.json();
      const texto = extraerTextoRespuesta(data);
      if (texto) return { ok: true, texto, via: 'gestornova' };
    } catch (error) {
      console.warn(`GestorNova ${path}:`, error.message);
    }
  }
  return { ok: false, error: 'GestorNova no expuso un endpoint de IA compatible (404)' };
}

/**
 * Chat IA: primero Groq directo (GROQ_API_KEY), opcional proxy GestorNova.
 */
async function completarChat({ system, user }) {
  const messages = [
    { role: 'system', content: system },
    { role: 'user', content: user },
  ];

  const groq = await llamarGroqDirecto(messages);
  if (groq.ok) return groq;

  if (process.env.DISABLE_GESTORNOVA_IA === 'true') {
    return groq;
  }

  const baseUrl = normalizarBaseUrl(process.env.GESTORNOVA_API_URL);
  if (!baseUrl) {
    return groq;
  }

  const proxy = await llamarGestorNovaProxy(baseUrl, messages, user, system);
  if (proxy.ok) return proxy;

  return {
    ok: false,
    error: groq.error || proxy.error,
  };
}

module.exports = { completarChat, extraerTextoRespuesta };
