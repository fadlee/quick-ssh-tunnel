import React, { useCallback, useMemo, useState } from "react";
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  View,
} from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { connectionKey, formatSshCommand, validateConnection } from "@shared/core";
import type { Connection } from "@shared/types";
import { getRunningTunnels, startTunnel } from "../lib/tunnel";
import {
  findConnectionByKey,
  loadConnections,
  newId,
  saveConnection,
} from "../lib/store";

type RootStackParamList = {
  ConnectionList: undefined;
  ConnectionForm: { connection?: Connection };
};

type Props = NativeStackScreenProps<RootStackParamList, "ConnectionForm">;

export function ConnectionFormScreen({ navigation, route }: Props) {
  const editing = route.params?.connection;

  const [mode, setMode] = useState<Connection["mode"]>(
    editing?.mode ?? "forward",
  );
  const [sshTarget, setSshTarget] = useState(editing?.sshTarget ?? "");
  const [port, setPort] = useState(
    editing ? String(editing.port) : "",
  );
  const [remoteHost, setRemoteHost] = useState(
    editing?.remoteHost ?? "127.0.0.1",
  );
  const [compression, setCompression] = useState(
    editing?.compression ?? false,
  );
  const [errors, setErrors] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [connecting, setConnecting] = useState(false);

  const previewCommand = useMemo(() => {
    const conn: Connection = {
      id: editing?.id ?? "preview",
      mode,
      sshTarget: sshTarget || "user@host",
      port: Number(port) || 0,
      remoteHost: remoteHost || "127.0.0.1",
      compression,
      lastUsedAt: 0,
    };
    try {
      return formatSshCommand(conn);
    } catch {
      return "";
    }
  }, [mode, sshTarget, port, remoteHost, compression, editing?.id]);

  const handleSave = useCallback(async () => {
    const conn: Connection = {
      id: editing?.id ?? newId(),
      mode,
      sshTarget: sshTarget.trim(),
      port: Number(port),
      remoteHost: mode === "socks5" ? "127.0.0.1" : remoteHost.trim(),
      compression,
      lastUsedAt: Date.now(),
    };

    const validationErrors = validateConnection(conn);
    if (validationErrors.length > 0) {
      setErrors(validationErrors);
      return;
    }

    // Reuse the existing connection ID if an identical one already exists
    // (same dedup logic as the CLI via connectionKey).
    const existing = await findConnectionByKey(connectionKey(conn));
    if (existing) conn.id = existing.id;

    // Port conflict check: is another *running* tunnel using the same port?
    try {
      const runningIds = await getRunningTunnels();
      const allConnections = await loadConnections();
      const conflict = allConnections.find(
        (item) =>
          item.id !== conn.id &&
          item.port === conn.port &&
          runningIds.includes(item.id),
      );
      if (conflict) {
        setErrors([
          `Port ${conn.port} is in use by active tunnel (${conflict.sshTarget})`,
        ]);
        return;
      }
    } catch {
      // native module not ready — proceed without conflict check
    }

    setErrors([]);
    setSaving(true);
    try {
      await saveConnection(conn);
    } catch (err) {
      setErrors([`Failed to save: ${String(err)}`]);
      setSaving(false);
      return;
    }
    setSaving(false);

    // Start the tunnel after saving (same behaviour as CLI/Raycast).
    setConnecting(true);
    try {
      await startTunnel(conn);
      navigation.goBack();
    } catch (err) {
      setErrors([`Failed to connect: ${String(err)}`]);
    } finally {
      setConnecting(false);
    }
  }, [editing, mode, sshTarget, port, remoteHost, compression, navigation]);

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
    >
      {/* Mode selector */}
      <Text style={styles.label}>Mode</Text>
      <View style={styles.modeRow}>
        <ModeButton
          label="Local Forward (-L)"
          active={mode === "forward"}
          onPress={() => setMode("forward")}
        />
        <ModeButton
          label="SOCKS5 (-D)"
          active={mode === "socks5"}
          onPress={() => setMode("socks5")}
        />
      </View>

      {/* SSH Target */}
      <Text style={styles.label}>SSH Target</Text>
      <TextInput
        style={styles.input}
        placeholder="user@host or alias"
        placeholderTextColor="#555"
        value={sshTarget}
        onChangeText={setSshTarget}
        autoCapitalize="none"
        autoCorrect={false}
      />

      {/* Port */}
      <Text style={styles.label}>Local Port</Text>
      <TextInput
        style={styles.input}
        placeholder="8080"
        placeholderTextColor="#555"
        value={port}
        onChangeText={setPort}
        keyboardType="numeric"
      />

      {/* Remote Host (hidden for SOCKS5) */}
      {mode === "forward" && (
        <>
          <Text style={styles.label}>Remote Host</Text>
          <TextInput
            style={styles.input}
            placeholder="127.0.0.1"
            placeholderTextColor="#555"
            value={remoteHost}
            onChangeText={setRemoteHost}
            autoCapitalize="none"
            autoCorrect={false}
          />
        </>
      )}

      {/* Compression */}
      <View style={styles.toggleRow}>
        <Text style={styles.toggleLabel}>Compression (-C)</Text>
        <Switch
          value={compression}
          onValueChange={setCompression}
          trackColor={{ false: "#333", true: "rgba(76,175,80,0.4)" }}
          thumbColor={compression ? "#4caf50" : "#666"}
        />
      </View>

      {/* Preview */}
      <Text style={styles.label}>Command Preview</Text>
      <View style={styles.preview}>
        <Text style={styles.previewText}>{previewCommand}</Text>
      </View>

      {/* Errors */}
      {errors.length > 0 && (
        <View style={styles.errorBox}>
          {errors.map((err, i) => (
            <Text key={i} style={styles.errorText}>
              • {err}
            </Text>
          ))}
        </View>
      )}


      {/* Actions */}
      <View style={styles.actions}>
        <Pressable
          style={[styles.btn, styles.cancelBtn]}
          onPress={() => navigation.goBack()}
        >
          <Text style={styles.cancelBtnText}>Cancel</Text>
        </Pressable>
        <Pressable
          style={[
            styles.btn,
            styles.saveBtn,
            (saving || connecting) && styles.saveBtnDisabled,
          ]}
          onPress={handleSave}
          disabled={saving || connecting}
        >
          <Text style={styles.saveBtnText}>
            {connecting
              ? "Connecting…"
              : saving
                ? "Saving…"
                : "Save & Connect"}
          </Text>
        </Pressable>
      </View>
    </ScrollView>
  );
}

function ModeButton({
  label,
  active,
  onPress,
}: {
  label: string;
  active: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      style={[styles.modeBtn, active && styles.modeBtnActive]}
      onPress={onPress}
    >
      <Text style={[styles.modeBtnText, active && styles.modeBtnTextActive]}>
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#1a1a1a",
  },
  content: {
    padding: 20,
    paddingBottom: 40,
  },
  label: {
    color: "#888",
    fontSize: 13,
    fontWeight: "600",
    marginTop: 16,
    marginBottom: 6,
    textTransform: "uppercase",
    letterSpacing: 0.5,
  },
  modeRow: {
    flexDirection: "row",
    gap: 8,
  },
  modeBtn: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "#333",
    alignItems: "center",
  },
  modeBtnActive: {
    borderColor: "#4caf50",
    backgroundColor: "rgba(76,175,80,0.1)",
  },
  modeBtnText: {
    color: "#666",
    fontSize: 13,
    fontWeight: "500",
  },
  modeBtnTextActive: {
    color: "#4caf50",
  },
  input: {
    backgroundColor: "#252525",
    color: "#e0e0e0",
    paddingHorizontal: 14,
    paddingVertical: 12,
    borderRadius: 8,
    fontSize: 15,
    borderWidth: 1,
    borderColor: "#2a2a2a",
  },
  toggleRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 20,
  },
  toggleLabel: {
    color: "#e0e0e0",
    fontSize: 15,
  },
  preview: {
    backgroundColor: "#111",
    borderRadius: 8,
    padding: 12,
    borderWidth: 1,
    borderColor: "#2a2a2a",
  },
  previewText: {
    color: "#4caf50",
    fontSize: 12,
    fontFamily: "monospace",
  },
  errorBox: {
    marginTop: 16,
    backgroundColor: "rgba(244,67,54,0.1)",
    borderRadius: 8,
    padding: 12,
    gap: 4,
  },
  errorText: {
    color: "#f44336",
    fontSize: 13,
  },
  actions: {
    flexDirection: "row",
    gap: 12,
    marginTop: 28,
  },
  btn: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: "center",
  },
  cancelBtn: {
    backgroundColor: "#252525",
  },
  cancelBtnText: {
    color: "#888",
    fontSize: 16,
    fontWeight: "600",
  },
  saveBtn: {
    backgroundColor: "#4caf50",
  },
  saveBtnDisabled: {
    opacity: 0.5,
  },
  saveBtnText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "600",
  },
});
