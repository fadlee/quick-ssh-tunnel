import React from "react";
import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { StatusBar } from "react-native";
import { ConnectionListScreen } from "./src/screens/ConnectionListScreen";
import { ConnectionFormScreen } from "./src/screens/ConnectionFormScreen";
import type { Connection } from "@shared/types";

export type RootStackParamList = {
  ConnectionList: undefined;
  ConnectionForm: { connection?: Connection };
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  return (
    <NavigationContainer
      theme={{
        dark: true,
        colors: {
          primary: "#4caf50",
          background: "#1a1a1a",
          card: "#252525",
          text: "#e0e0e0",
          border: "#2a2a2a",
          notification: "#4caf50",
        },
      }}
    >
      <StatusBar
        barStyle="light-content"
        backgroundColor="#1a1a1a"
      />
      <Stack.Navigator
        screenOptions={{
          headerStyle: { backgroundColor: "#252525" },
          headerTintColor: "#e0e0e0",
          headerShadowVisible: false,
          contentStyle: { backgroundColor: "#1a1a1a" },
          headerTitleStyle: { fontSize: 17, fontWeight: "600" },
        }}
      >
        <Stack.Screen
          name="ConnectionList"
          component={ConnectionListScreen}
          options={{ title: "SSH Tunnels" }}
        />
        <Stack.Screen
          name="ConnectionForm"
          component={ConnectionFormScreen}
          options={{
            title: "Connection",
            presentation: "modal",
          }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
