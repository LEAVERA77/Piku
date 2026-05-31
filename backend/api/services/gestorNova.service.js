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

/**
 * Llama a Groq vía el API de GestorNova (recomendado) o directo con GROQ_API_KEY.
 */
async function completarChat({ system, user }) {
  const baseUrl = (process.env.GESTORNOVA_API_URL || 'https://api-gestornova.onrender.com')
    .trim()
    .replace(/^["']|["']$/g, '')
    .replace(/\\n/g, '')
    .replace(/\/$/, '');
  const gestorPaths = ['/api/ia/chat', '/api/ia/completions', '/api/chat'];
  const messages = [
    { role: 'system', content: system },
    { role: 'user', content: user },
  ];

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

  const apiKey = String(process.env.GROQ_API_KEY || '').trim();
  if (!apiKey) {
    return {
      ok: false,
      error:
        'IA no disponible. Configurá GESTORNOVA_API_URL o GROQ_API_KEY en Render (misma clave que GestorNova).',
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

module.exports = { completarChat, extraerTextoRespuesta };
