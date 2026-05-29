import Constants from "expo-constants";

type Extra = {
  apiBaseUrl?: string;
  googleWebClientId?: string;
  googleIosClientId?: string;
};

const extra = (Constants.expoConfig?.extra ?? {}) as Extra;

export const API_BASE_URL = (
  extra.apiBaseUrl ?? "https://piku-324e.onrender.com"
).replace(/\/$/, "");

/** Mismo valor que GOOGLE_CLIENT_ID en Render — fijo para toda la app, no por usuario. */
export const GOOGLE_WEB_CLIENT_ID = extra.googleWebClientId ?? "";
export const GOOGLE_IOS_CLIENT_ID =
  extra.googleIosClientId ?? extra.googleWebClientId ?? "";
