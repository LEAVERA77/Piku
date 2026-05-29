import * as Google from "expo-auth-session/providers/google";
import * as WebBrowser from "expo-web-browser";
import { GOOGLE_IOS_CLIENT_ID, GOOGLE_WEB_CLIENT_ID } from "./config";

WebBrowser.maybeCompleteAuthSession();

/**
 * Abre la cuenta Google guardada en el iPhone/Android (selector del sistema).
 * Devuelve idToken para enviar al backend — el usuario no escribe contraseña.
 */
export function useGoogleAuth() {
  const [request, response, promptAsync] = Google.useAuthRequest({
    webClientId: GOOGLE_WEB_CLIENT_ID,
    iosClientId: GOOGLE_IOS_CLIENT_ID,
  });

  const idToken =
    response?.type === "success"
      ? response.authentication?.idToken ?? response.params?.id_token
      : undefined;

  return {
    request,
    idToken,
    promptAsync,
    configured: GOOGLE_WEB_CLIENT_ID.length > 10,
  };
}
