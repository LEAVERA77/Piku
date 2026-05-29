import { useEffect, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { clearToken, fetchSaldo } from "../src/api";

export default function HomeScreen() {
  const router = useRouter();
  const [puntos, setPuntos] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchSaldo()
      .then((s) => setPuntos(s.puntos))
      .catch((e) => setError(e.message));
  }, []);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Piku</Text>
      {puntos != null ? (
        <Text style={styles.puntos}>{puntos} puntos</Text>
      ) : (
        <Text>Cargando saldo…</Text>
      )}
      {error ? <Text style={styles.error}>{error}</Text> : null}
      <Text style={styles.note}>
        Cliente iOS/Android (Expo). La app completa con mapa y QR está en Android nativo (carpeta app/).
      </Text>
      <Pressable
        style={styles.logout}
        onPress={async () => {
          await clearToken();
          router.replace("/");
        }}
      >
        <Text style={styles.logoutText}>Cerrar sesión</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, justifyContent: "center", backgroundColor: "#F5F5F5" },
  title: { fontSize: 32, fontWeight: "800", color: "#00A86B", textAlign: "center" },
  puntos: { fontSize: 28, fontWeight: "700", textAlign: "center", marginVertical: 16 },
  error: { color: "#c62828", textAlign: "center" },
  note: { fontSize: 13, color: "#6B6B6B", textAlign: "center", marginTop: 24 },
  logout: {
    marginTop: 32,
    padding: 14,
    backgroundColor: "#00A86B",
    borderRadius: 14,
    alignItems: "center",
  },
  logoutText: { color: "#fff", fontWeight: "700" },
});
