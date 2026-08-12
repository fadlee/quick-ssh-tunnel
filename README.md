# Quick SSH Tunnel

A Raycast extension for creating and managing SSH tunnels without maintaining a permanent configuration list.

Quick SSH Tunnel is designed for short, repeatable workflows: choose an SSH target, enter a port, optionally change the remote host, and connect. Successful connections are remembered locally so they can be reconnected later.

## Features

- Create SSH tunnels from Raycast with a compact form.
- Support for local port forwarding (`ssh -L`).
- Support for SOCKS5 dynamic proxy tunnels (`ssh -D`).
- SSH compression enabled by default, with an option to disable it.
- Use an SSH target such as `user@host` or an alias from `~/.ssh/config`.
- Keep tunnels running after the Raycast window closes.
- Show active tunnels separately from recent connections.
- Reconnect a recent connection without entering its settings again.
- Clone a connection into a pre-filled form with **Clone and Connect**.
- Prefill a new connection from the current search text when no result is found.
- Detect stale processes and avoid treating a reused PID as the wrong tunnel.
- Reject local-port conflicts before starting a new tunnel.
- Keep up to 50 unique successful connections in local history.

## Requirements

- macOS
- Raycast
- OpenSSH (`ssh`)
- An SSH key or another non-interactive SSH authentication method
- `ssh-agent` and/or a configured `~/.ssh/config` entry

The extension does not provide an interactive terminal. Password prompts are not supported.

## Installation

### Install from a local checkout

1. Clone the repository:

   ```bash
   git clone https://github.com/fadlee/quick-ssh-tunnel.git
   cd quick-ssh-tunnel
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

3. Start Raycast development mode:

   ```bash
   npm run dev
   ```

4. Raycast will load the extension in development mode. Keep the process running while developing.

### Build the extension

```bash
npm run build
```

The build output is generated in `dist/`.

## SSH Authentication

Quick SSH Tunnel delegates authentication and SSH configuration to OpenSSH. Configure your hosts in `~/.ssh/config` or use a direct target such as `user@example.com`.

Example SSH config:

```sshconfig
Host staging-db
    HostName staging.example.com
    User deploy
    IdentityFile ~/.ssh/id_ed25519
    ServerAliveInterval 30
```

The form can then use:

```text
staging-db
```

If the private key is passphrase-protected, load it into your agent before connecting:

```bash
ssh-add ~/.ssh/id_ed25519
```

You can test the same authentication outside Raycast with:

```bash
ssh -o BatchMode=yes staging-db
```

Because the extension starts SSH without a terminal, an authentication method that requires typing a password or passphrase during connection will fail. Use `ssh-agent`, macOS Keychain integration, or another non-interactive OpenSSH setup.

## Usage

Open Raycast and run **Quick SSH Tunnel**.

### Create a local port-forwarding tunnel

1. Choose **New Connection**.
2. Keep **Tunnel Type** set to **Local Port Forwarding**.
3. Enter an SSH target, for example:

   ```text
   deploy@staging.example.com
   ```

4. Enter a port, for example `5432`.
5. Optionally change **Remote Host**. It defaults to `127.0.0.1` and is resolved from the SSH server's point of view.
6. Leave **Compression** enabled unless you have a reason to disable it.
7. Choose **Connect**.

For port `5432` and remote host `127.0.0.1`, the extension runs the equivalent of:

```bash
ssh -N -L 5432:127.0.0.1:5432 -C \
  -o BatchMode=yes \
  -o ExitOnForwardFailure=yes \
  -o ServerAliveInterval=30 \
  -o ServerAliveCountMax=3 \
  deploy@staging.example.com
```

The service is then available locally at:

```text
localhost:5432
```

### Create a SOCKS5 proxy

1. Choose **New Connection**.
2. Set **Tunnel Type** to **SOCKS5 Proxy**.
3. Enter the SSH target.
4. Enter the local proxy port, for example `1080`.
5. Choose whether SSH compression should be enabled.
6. Choose **Connect**.

The extension runs the equivalent of:

```bash
ssh -N -D 1080 -C \
  -o BatchMode=yes \
  -o ExitOnForwardFailure=yes \
  -o ServerAliveInterval=30 \
  -o ServerAliveCountMax=3 \
  deploy@staging.example.com
```

Configure an application to use:

```text
SOCKS5 localhost:1080
```

SOCKS5 mode does not use **Remote Host**. The proxy allows applications to request destinations through the SSH server.

### Search-to-form prefill

The main list uses Raycast's search bar. If the search produces no matching history item, press Enter and choose **New Connection**. The search text is automatically prefilled as the **SSH Target**.

For example, searching for:

```text
prod-web
```

prefills:

```text
SSH Target: prod-web
```

### Active tunnels and recent connections

The list contains two sections:

- **Active Tunnels**: connections whose detached SSH process is currently running.
- **Recent Connections**: successful connections that are not currently running.

An active tunnel is shown only in the active section. After it stops, it becomes available again in recent connections.

Select a connection and press Enter to open **Connection Actions**. Available actions include:

- **Connect** or **Stop Tunnel**
- **Copy Local Address** for active tunnels
- **Edit and Connect**
- **Clone and Connect**
- **Delete History**

### Clone and Connect

**Clone and Connect** opens a new form prefilled from the selected connection. The clone receives a new internal ID, and is not written to history until it connects successfully.

This is useful when you want to create a variation of an existing tunnel, such as:

- another local port
- a different SSH target
- a different remote host
- switching between local forwarding and SOCKS5
- changing compression

### Reconnect a recent connection

Select a stopped item in **Recent Connections** and choose **Connect**. The saved parameters are used directly; the form is not opened.

If the connection is already active, Quick SSH Tunnel does not start a second process. It reports that the tunnel is already running.

## Keyboard Shortcuts

| Shortcut | Action                                      |
| -------- | ------------------------------------------- |
| `Space`  | Connect or stop the selected tunnel         |
| `Cmd+N`  | Create a new connection                     |
| `Cmd+E`  | Edit and connect                            |
| `Cmd+D`  | Clone and connect                           |
| `Cmd+.`  | Copy the local address for an active tunnel |
| `Ctrl+X` | Delete history                              |

Press Enter on a connection to open its action submenu.

## Connection Rules

### Local forwarding

The single **Port** field is used for both the local and remote port:

```text
local_port:remote_host:remote_port
```

For example, port `8080` and remote host `127.0.0.1` creates:

```text
-L 8080:127.0.0.1:8080
```

The remote host is interpreted from the SSH server. It can be a simple IPv4 address or hostname, such as:

```text
127.0.0.1
localhost
database.internal
```

### SOCKS5

The **Port** field is the local SOCKS5 listening port:

```text
-D <port>
```

Remote Host is not required in SOCKS5 mode.

### Port conflicts

A local port can only be used by one active tunnel. Quick SSH Tunnel rejects a connection when another active connection already uses the same port. It does not automatically change the port or stop another tunnel.

### Connection history

History is stored only after a tunnel starts successfully. Connections are considered unique by:

- tunnel type
- SSH target
- port
- remote host
- compression setting

The history is limited to the 50 most recent unique connections. Reusing a connection moves it to the top rather than creating a duplicate.

## Process Lifecycle

The SSH process is started detached from Raycast and is unreferenced from the extension process. It continues running after the Raycast window closes.

Quick SSH Tunnel stores the process ID and a forwarding specification locally. When checking status, it verifies both:

1. the PID is still alive, and
2. the process command line still matches the expected SSH target and forwarding arguments.

This prevents a recycled PID from being mistaken for the original tunnel.

SSH is started with:

- `-N`: do not execute a remote command
- `-C` when compression is enabled
- `-o BatchMode=yes`: never wait for interactive authentication
- `-o ExitOnForwardFailure=yes`: fail if the requested forwarding cannot be created
- `-o ServerAliveInterval=30`
- `-o ServerAliveCountMax=3`

Quick SSH Tunnel does not automatically reconnect a tunnel after it exits. Use the recent connection's **Connect** action to start it again.

## Local Storage

The extension stores its data under:

```text
~/.config/quick-ssh-tunnel/
```

Files:

```text
connections.json  # successful connection history
state.json        # active tunnel PIDs and start times
```

The stored data contains connection settings and process metadata. It does not store SSH passwords or private keys.

To reset the extension's local history and state, first stop active tunnels from Raycast, then remove the directory:

```bash
rm -rf ~/.config/quick-ssh-tunnel
```

Only remove this directory if you are sure no tunnel managed by the extension is still running.

## Troubleshooting

### Authentication failed

Check that:

- the SSH target is correct
- the host exists in `~/.ssh/config`, if using an alias
- the required key is available
- the key's passphrase has been loaded with `ssh-add`
- `ssh -o BatchMode=yes <target>` succeeds in Terminal

### The tunnel immediately stops

Common causes include:

- the SSH host is unreachable
- authentication failed
- the requested local port is already in use
- the remote host or port cannot be reached from the SSH server
- the SSH configuration contains an interactive prompt

For more detail, run the equivalent SSH command in Terminal. The extension intentionally keeps the MVP UI focused and does not expose an SSH log viewer.

### The port is already in use

Choose another local port or stop the active connection currently using that port. Quick SSH Tunnel intentionally does not stop or reassign another tunnel automatically.

### A tunnel is shown as stopped even though a process appears to exist

The extension checks the SSH process command line as well as the PID. A process with the same PID but different forwarding arguments is not considered the requested tunnel.

### SOCKS5 is connected but an application cannot browse through it

Verify that:

- the application is configured for SOCKS5, not HTTP proxy
- the proxy address is `localhost`
- the proxy port matches the connection's port
- DNS behavior is configured according to the application's SOCKS5 settings
- the SSH server can reach the requested destination

## Development

Install dependencies:

```bash
npm install
```

Run the extension in Raycast development mode:

```bash
npm run dev
```

Run linting:

```bash
npm run lint
```

Automatically fix lint and formatting issues:

```bash
npm run fix-lint
```

Build the extension:

```bash
npm run build
```

Run the focused tests with Bun:

```bash
bun test tests/core.test.ts tests/store.test.ts
```

The tests cover SSH argument construction, SOCKS5 behavior, input validation, connection identity, history limits, and cloning.

## Project Structure

```text
src/
├── quick-ssh-tunnel.tsx  # Main Raycast list and connection actions
├── connection-form.tsx   # New, edit, and clone connection form
└── lib/
    ├── core.ts           # SSH arguments, validation, identity, labels
    ├── process.ts        # Detached SSH process lifecycle and status checks
    └── store.ts           # Local history and process state persistence

tests/
├── core.test.ts
└── store.test.ts
```

## Scope and Non-Goals

Quick SSH Tunnel intentionally focuses on fast, non-interactive SSH forwarding. It currently does not provide:

- password prompts
- private-key selection in the form
- arbitrary SSH argument input
- automatic reconnect
- SSH log files or a log viewer
- manual connection names
- SOCKS5 authentication settings
- separate local and remote port fields

Use `~/.ssh/config` for advanced OpenSSH configuration such as identity files, jump hosts, host aliases, and server-specific options.

## License

MIT
