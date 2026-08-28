# Quick SSH Tunnel — Android

React Native Android app for managing SSH tunnels, sharing pure logic with the [Raycast extension](../) and CLI in this repo.

## Features

- Local port forwarding (`-L`) and SOCKS5 dynamic forwarding (`-D`)
- Persistent tunnels via Android Foreground Service
- Connection history with dedup (same JSON format as the CLI)
- Port conflict detection against running tunnels
- Save & Connect in one action
- Dark developer-tool aesthetic

## Prerequisites

- **Node.js** 18+
- **JDK** 17 (required by Android Gradle Plugin 8.x)
- **Android SDK** with `compileSdkVersion 34` (Android 14)
- **Android NDK** (optional, only if building native code)

## Setup

```bash
cd android-app
npm install
```

### Start Metro bundler

```bash
npm start
```

### Run on device / emulator

```bash
npm run android
```

### Build release APK

```bash
npm run build:android
```

## Shared Code

This app imports pure logic from the parent project's `src/lib/` directory:

- `src/lib/core.ts` — `buildArgs`, `formatSshCommand`, `connectionKey`, `validateConnection`, `formatConnection`
- `src/lib/types.ts` — `Connection` type

Metro is configured (`metro.config.js`) to watch the parent directory and resolve the `@shared/*` import alias to `../src/lib/*`. No files outside `android-app/` are modified.

## Gradle Wrapper

The Gradle wrapper scripts (`gradlew`, `gradlew.bat`) and `gradle-wrapper.properties` are included, but the `gradle-wrapper.jar` binary is **not** committed (it's a binary artifact).

If `./gradlew` fails with a missing jar error, generate it once:

```bash
cd android
gradle wrapper --gradle-version 8.6
```

Or if you don't have Gradle installed, use the Android Studio Gradle integration: open the `android/` folder in Android Studio and let it sync — it will generate the wrapper jar automatically.

## Project Structure

```
android-app/
├── App.tsx                      # Navigation entry point
├── index.js                     # RN registration
├── metro.config.js              # Watches parent src/lib/ for shared code
├── src/
│   ├── lib/
│   │   ├── store.ts             # AsyncStorage adapter (same JSON format as CLI)
│   │   └── tunnel.ts            # Native module bridge wrapper
│   ├── screens/
│   │   ├── ConnectionListScreen.tsx
│   │   └── ConnectionFormScreen.tsx
│   ├── components/
│   │   ├── ConnectionItem.tsx
│   │   └── StatusBadge.tsx
│   └── hooks/
│       └── useConnections.ts
└── android/
    ├── app/src/main/java/com/quicksshtunnel/
    │   ├── SshTunnelModule.kt   # RN native module (JSch bridge)
    │   ├── SshTunnelPackage.kt
    │   ├── TunnelService.kt     # Foreground service for background tunnels
    │   ├── MainApplication.kt
    │   └── MainActivity.kt
    ├── app/build.gradle
    ├── build.gradle
    ├── settings.gradle
    └── gradle/wrapper/gradle-wrapper.properties
```

## Native SSH Implementation

Tunnels are established via [JSch](https://www.jcraft.com/jsch/) (`com.jcraft:jsch:0.1.55`):

- **Local Forward (`-L`)**: `session.setPortForwardingL(port, remoteHost, port)`
- **SOCKS5 (`-D`)**: `session.setPortForwardingD(port)`

Active sessions are stored in a `ConcurrentHashMap` keyed by connection ID. The `TunnelService` foreground service keeps sessions alive in the background with a persistent notification.
