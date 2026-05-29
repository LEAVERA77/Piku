import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Image,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { useRouter } from "expo-router";
import { getToken, loginEmail, loginGoogle } from "../src/api";
import { useGoogleAuth } from "../src/googleAuth";

export default function LoginScreen() {
  const router = useRouter();
  const { idToken, promptAsync, request, configured } = useGoogleAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getToken().then((t) => {
      if (t) router.replace("/home");
    });
  }, [router]);

  useEffect(() => {
    if (!idToken) return;
    setLoading(true);
    loginGoogle(idToken)
      .then(() => router.replace("/home"))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [idToken, router]);

  const onEmailLogin = async () => {
    setError(null);
    setLoading(true);
    try {
      await loginEmail(email.trim(), password);
      router.replace("/home");
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.logo}>Piku</Text>
      <Text style={styles.tagline}>Tus puntos, tus descuentos</Text>

      <TextInput
        style={styles.input}
        placeholder="Email"
        autoCapitalize="none"
        keyboardType="email-address"
        value={email}
        onChangeText={setEmail}
      />
      <TextInput
        style={styles.input}
        placeholder="Contraseña"
        secureTextEntry
        value={password}
        onChangeText={setPassword}
      />

      {error ? <Text style={styles.error}>{error}</Text> : null}

      <Pressable style={styles.btnPrimary} onPress={onEmailLogin} disabled={loading}>
        <Text style={styles.btnText}>Iniciar sesión</Text>
      </Pressable>

      <Pressable
        style={styles.btnGoogle}
        disabled={!request || loading || !configured}
        onPress={() => {
          setError(null);
          if (!configured) {
            setError("Configurá googleWebClientId (mismo ID que GOOGLE_CLIENT_ID en Render)");
            return;
          }
          promptAsync();
        }}
      >
        <Image
          source={{
            uri: "https://developers.google.com/identity/images/g-logo.png",
          }}
          style={styles.googleIcon}
        />
        <Text style={styles.btnGoogleText}>Continuar con Google</Text>
      </Pressable>

      {!configured ? (
        <Text style={styles.hint}>
          Un solo ID Web para todos los usuarios — ver app.config.ts y Render GOOGLE_CLIENT_ID
        </Text>
      ) : null}

      {loading ? <ActivityIndicator style={{ marginTop: 16 }} color="#00A86B" /> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, justifyContent: "center", backgroundColor: "#F5F5F5" },
  logo: { fontSize: 42, fontWeight: "800", color: "#00A86B", textAlign: "center" },
  tagline: { fontSize: 16, color: "#6B6B6B", textAlign: "center", marginBottom: 24 },
  input: {
    backgroundColor: "#fff",
    borderRadius: 12,
    padding: 14,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: "#ddd",
  },
  error: { color: "#c62828", marginBottom: 8 },
  btnPrimary: {
    backgroundColor: "#FF6B35",
    padding: 14,
    borderRadius: 14,
    alignItems: "center",
    marginTop: 8,
  },
  btnText: { color: "#fff", fontWeight: "700" },
  btnGoogle: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#fff",
    padding: 14,
    borderRadius: 14,
    marginTop: 12,
    borderWidth: 1,
    borderColor: "#ddd",
  },
  googleIcon: { width: 22, height: 22, marginRight: 10 },
  btnGoogleText: { fontWeight: "600" },
  hint: { fontSize: 12, color: "#6B6B6B", marginTop: 12, textAlign: "center" },
});
