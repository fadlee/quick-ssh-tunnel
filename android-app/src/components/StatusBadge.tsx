import React from "react";
import { StyleSheet, Text, View } from "react-native";
import type { Status } from "../lib/tunnel";

type Props = {
  status: Status;
};

export function StatusBadge({ status }: Props) {
  const isRunning = status === "running";
  return (
    <View
      style={[
        styles.badge,
        isRunning ? styles.running : styles.stopped,
      ]}
    >
      <View
        style={[
          styles.dot,
          isRunning ? styles.dotRunning : styles.dotStopped,
        ]}
      />
      <Text
        style={[
          styles.text,
          isRunning ? styles.textRunning : styles.textStopped,
        ]}
      >
        {isRunning ? "Running" : "Stopped"}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 4,
    gap: 4,
  },
  running: {
    backgroundColor: "rgba(76, 175, 80, 0.15)",
  },
  stopped: {
    backgroundColor: "rgba(128, 128, 128, 0.15)",
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  dotRunning: {
    backgroundColor: "#4caf50",
  },
  dotStopped: {
    backgroundColor: "#888",
  },
  text: {
    fontSize: 11,
    fontWeight: "600",
  },
  textRunning: {
    color: "#4caf50",
  },
  textStopped: {
    color: "#888",
  },
});
