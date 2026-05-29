import * as SecureStore from "expo-secure-store";
import { API_BASE_URL } from "./config";

const TOKEN_KEY = "piku_token";

export async function getToken(): Promise<string | null> {
  return SecureStore.getItemAsync(TOKEN_KEY);
}

export async function setToken(token: string): Promise<void> {
  await SecureStore.setItemAsync(TOKEN_KEY, token);
}

export async function clearToken(): Promise<void> {
  await SecureStore.deleteItemAsync(TOKEN_KEY);
}

export type LoginResponse = {
  token: string;
  usuario: {
    id: string;
    email: string;
    nombre: string;
    rol: string;
    puntos_saldo?: number;
  };
};

export async function loginGoogle(idToken: string): Promise<LoginResponse> {
  const res = await fetch(`${API_BASE_URL}/api/auth/google`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ idToken }),
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.mensaje || data.error || "Error al iniciar sesión con Google");
  }
  await setToken(data.token);
  return data;
}

export async function loginEmail(
  email: string,
  password: string
): Promise<LoginResponse> {
  const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.mensaje || data.error || "Credenciales inválidas");
  }
  await setToken(data.token);
  return data;
}

export async function fetchSaldo(): Promise<{ puntos: number }> {
  const token = await getToken();
  const res = await fetch(`${API_BASE_URL}/api/usuario/saldo`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.mensaje || "Error al cargar saldo");
  return { puntos: data.puntos ?? data.puntos_saldo ?? 0 };
}
