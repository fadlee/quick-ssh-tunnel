package com.quicksshtunnel

// ── Data model ──────────────────────────────────────────────────────────────

enum class ConnectionMode { FORWARD, SOCKS5 }

data class Connection(
    val id: String,
    val mode: ConnectionMode,
    val sshTarget: String,
    val port: Int,
    val remoteHost: String,
    val compression: Boolean,
    val lastUsedAt: Long,
    val password: String? = null,
)

// ── Core logic — port of src/lib/core.ts ────────────────────────────────────

private val HOST_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9.-]*$")
private val SAFE_TOKEN = Regex("^[A-Za-z0-9_./:@=-]+$")

/** Build the ssh CLI argument list for a connection (mirrors buildArgs in core.ts). */
fun buildArgs(connection: Connection): List<String> {
    val args = mutableListOf("-N", if (connection.mode == ConnectionMode.SOCKS5) "-D" else "-L")
    args.add(
        if (connection.mode == ConnectionMode.SOCKS5) {
            connection.port.toString()
        } else {
            "${connection.port}:${connection.remoteHost}:${connection.port}"
        }
    )
    if (connection.compression) args.add("-C")
    args.addAll(
        listOf(
            "-o", "BatchMode=yes",
            "-o", "ExitOnForwardFailure=yes",
            "-o", "ServerAliveInterval=30",
            "-o", "ServerAliveCountMax=3",
            connection.sshTarget,
        )
    )
    return args
}

/** Shell-quote a single token, leaving safe tokens unquoted. */
fun shellQuote(value: String): String {
    return if (SAFE_TOKEN.matches(value)) {
        value
    } else {
        "'" + value.replace("'", "'\\''") + "'"
    }
}

/** Full ssh command string, shell-quoted. */
fun formatSshCommand(connection: Connection): String {
    return (listOf("ssh") + buildArgs(connection)).joinToString(" ") { shellQuote(it) }
}

/** Stable dedup key — same fields as core.ts connectionKey. */
fun connectionKey(connection: Connection): String {
    return listOf(
        connection.mode.name,
        connection.sshTarget,
        connection.port,
        connection.remoteHost,
        connection.compression,
    ).toString()
}

/** Validate a connection; returns a list of human-readable error strings (English). */
fun validateConnection(connection: Connection): List<String> {
    val errors = mutableListOf<String>()
    if (connection.sshTarget.isBlank()) {
        errors.add("SSH target is required")
    } else if (connection.sshTarget.any { it.isWhitespace() } || connection.sshTarget.startsWith("-")) {
        errors.add("SSH target must be user@host or alias without spaces")
    }
    if (connection.port !in 1..65535) {
        errors.add("Port must be 1–65535")
    }
    if (connection.mode != ConnectionMode.SOCKS5) {
        if (connection.remoteHost.isBlank() || !HOST_PATTERN.matches(connection.remoteHost)) {
            errors.add("Remote host must be a valid IP or hostname")
        }
    }
    return errors
}

/** Short human-readable summary of a connection. */
fun formatConnection(connection: Connection): String {
    return if (connection.mode == ConnectionMode.SOCKS5) {
        "${connection.sshTarget} · SOCKS5 · ${connection.port}"
    } else {
        "${connection.sshTarget} · ${connection.port} → ${connection.remoteHost}"
    }
}
