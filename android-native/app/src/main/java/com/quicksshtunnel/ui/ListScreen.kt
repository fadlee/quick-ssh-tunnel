package com.quicksshtunnel.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicksshtunnel.Connection
import com.quicksshtunnel.ConnectionStore
import com.quicksshtunnel.SshTunnelManager
import com.quicksshtunnel.TunnelService
import com.quicksshtunnel.formatConnection
import com.quicksshtunnel.formatSshCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    onHelp: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var connections by remember { mutableStateOf(ConnectionStore.loadConnections(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedConnection by remember { mutableStateOf<Connection?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Connection?>(null) }
    var pollTick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            connections = ConnectionStore.loadConnections(context)
            pollTick++
        }
    }

    val sortedConnections by remember(connections, pollTick) {
        derivedStateOf {
            connections.sortedWith(
                compareByDescending<Connection> { SshTunnelManager.isRunning(it.id) }
                    .thenByDescending { it.lastUsedAt }
            )
        }
    }

    val filtered by remember(sortedConnections, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                sortedConnections
            } else {
                val q = searchQuery.lowercase()
                sortedConnections.filter {
                    it.sshTarget.lowercase().contains(q) ||
                        formatConnection(it).lowercase().contains(q)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SSH Tunnels",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onHelp) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.HelpOutline,
                    contentDescription = "Help",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onNew) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ActiveColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Add,
                        contentDescription = "New connection",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = {
                Text("Search connections…", color = TextSecondary, fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = null, tint = TextSecondary)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matches found" else "No connections yet",
                        fontSize = 16.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Try a different search term" else "Tap + New to create your first SSH tunnel",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { conn ->
                    ConnectionItem(
                        connection = conn,
                        running = SshTunnelManager.isRunning(conn.id),
                        onClick = { selectedConnection = conn },
                    )
                }
            }
        }
    }

    selectedConnection?.let { conn ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val running = SshTunnelManager.isRunning(conn.id)

        ModalBottomSheet(
            onDismissRequest = { selectedConnection = null },
            sheetState = sheetState,
            containerColor = SurfaceColor,
        ) {
            Column(
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                Text(
                    text = conn.sshTarget,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                Text(
                    text = formatConnection(conn),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))

                SheetAction(
                    text = if (running) "Stop" else "Start",
                    color = if (running) ErrorColor else ActiveColor,
                ) {
                    scope.launch {
                        if (running) {
                            withContext(Dispatchers.IO) {
                                SshTunnelManager.stopTunnel(conn.id)
                            }
                            if (SshTunnelManager.activeCount() == 0) {
                                TunnelService.stop(context)
                            } else {
                                TunnelService.updateNotification(context, SshTunnelManager.activeCount())
                            }
                        } else {
                            val result = withContext(Dispatchers.IO) {
                                SshTunnelManager.startTunnel(conn)
                            }
                            if (result.isSuccess) {
                                TunnelService.start(context, SshTunnelManager.activeCount())
                            } else {
                                Toast.makeText(
                                    context,
                                    result.exceptionOrNull()?.message ?: "Failed to start tunnel",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                        pollTick++
                        selectedConnection = null
                    }
                }

                SheetAction(text = "Edit", color = TextPrimary) {
                    val id = conn.id
                    selectedConnection = null
                    onEdit(id)
                }

                SheetAction(text = "Copy SSH Command", color = TextPrimary) {
                    val cmd = formatSshCommand(conn)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("SSH Command", cmd))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    selectedConnection = null
                }

                SheetAction(text = "Delete", color = ErrorColor) {
                    showDeleteDialog = conn
                    selectedConnection = null
                }

                SheetAction(text = "Cancel", color = TextSecondary) {
                    selectedConnection = null
                }
            }
        }
    }

    showDeleteDialog?.let { conn ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete connection?") },
            text = { Text("Remove \"${conn.sshTarget}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (SshTunnelManager.isRunning(conn.id)) {
                            withContext(Dispatchers.IO) {
                                SshTunnelManager.stopTunnel(conn.id)
                            }
                            if (SshTunnelManager.activeCount() == 0) {
                                TunnelService.stop(context)
                            }
                        }
                        ConnectionStore.removeConnection(context, conn.id)
                        connections = ConnectionStore.loadConnections(context)
                        showDeleteDialog = null
                    }
                }) { Text("Delete", color = ErrorColor) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ConnectionItem(
    connection: Connection,
    running: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(if (running) ActiveColor else InactiveColor),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = connection.sshTarget,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatConnection(connection),
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        StatusBadge(running = running)
    }
}

@Composable
private fun SheetAction(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
}
