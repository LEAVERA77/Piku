import type { ExpoConfig } from "expo/config";

// Mismo ID Web que GOOGLE_CLIENT_ID en Render y google.webClientId en Android config.json
/** Mismo que GOOGLE_CLIENT_ID en Render — un solo valor para verificar tokens (no por usuario). */
const GOOGLE_WEB_CLIENT_ID =
  process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID ??
  "TU_WEB_CLIENT_ID.apps.googleusercontent.com";
/** Cliente iOS en Google Cloud (Bundle ID com.piku.app) — distinto del Web, solo para el selector en iPhone. */
const GOOGLE_IOS_CLIENT_ID =
  process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID ?? GOOGLE_WEB_CLIENT_ID;

const config: ExpoConfig = {
  name: "Piku",
  slug: "piku",
  version: "1.0.0",
  orientation: "portrait",
  scheme: "piku",
  userInterfaceStyle: "light",
  ios: {
    supportsTablet: true,
    bundleIdentifier: "com.piku.app",
  },
  android: {
    package: "com.piku.app",
  },
  plugins: ["expo-router", "expo-secure-store"],
  extra: {
    apiBaseUrl:
      process.env.EXPO_PUBLIC_API_BASE_URL ?? "https://piku-324e.onrender.com",
    googleWebClientId: GOOGLE_WEB_CLIENT_ID,
    googleIosClientId: GOOGLE_IOS_CLIENT_ID,
    eas: {
      projectId: "piku-mobile",
    },
  },
};

export default config;
