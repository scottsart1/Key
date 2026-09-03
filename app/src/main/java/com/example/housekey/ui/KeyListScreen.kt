package com.example.housekey.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.housekey.R
import com.example.housekey.data.KeyEntity
import com.example.housekey.data.KeyType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyListScreen(
    state: KeysUiState,
    capabilities: Capabilities,
    onAdd: () -> Unit,
    onEdit: (KeyEntity) -> Unit,
    onEmulate: (KeyEntity) -> Unit,
    onStop: () -> Unit,
    onDelete: (KeyEntity) -> Unit,
    onReadTag: () -> Unit,
    onAbout: () -> Unit,
) {
    val activeName = state.keys.firstOrNull { it.id == state.activeKeyId }?.name

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_keys)) },
                actions = {
                    IconButton(onClick = onReadTag) {
                        Icon(Icons.Filled.Nfc, contentDescription = stringResource(R.string.action_read_tag))
                    }
                    IconButton(onClick = onAbout) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.action_about))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CapabilityWarnings(capabilities)
            }
            item {
                StatusCard(activeKeyName = activeName, onStop = onStop)
            }
            if (state.keys.isEmpty()) {
                item { EmptyState() }
            } else {
                items(state.keys, key = { it.id }) { key ->
                    KeyRow(
                        key = key,
                        isActive = key.id == state.activeKeyId,
                        onEmulate = { onEmulate(key) },
                        onEdit = { onEdit(key) },
                        onDelete = { onDelete(key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.Nfc,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.empty_keys_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.empty_keys_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KeyRow(
    key: KeyEntity,
    isActive: Boolean,
    onEmulate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEmulate),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(iconFor(key.type), contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    key.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    keySubtitle(key),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isActive) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.status_active_format, key.name),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_edit))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit)) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

private fun iconFor(type: KeyType): ImageVector = when (type) {
    KeyType.NDEF_TEXT -> Icons.Filled.Notes
    KeyType.NDEF_URI -> Icons.Filled.Link
    KeyType.RAW_APDU -> Icons.Filled.Code
}

@Composable
private fun keySubtitle(key: KeyEntity): String {
    val typeLabel = when (key.type) {
        KeyType.NDEF_TEXT -> stringResource(R.string.type_ndef_text)
        KeyType.NDEF_URI -> stringResource(R.string.type_ndef_uri)
        KeyType.RAW_APDU -> stringResource(R.string.type_raw_apdu)
    }
    val detail = when (key.type) {
        KeyType.NDEF_TEXT, KeyType.NDEF_URI -> key.content
        KeyType.RAW_APDU -> key.aid
    }
    return if (detail.isBlank()) typeLabel else "$typeLabel  •  $detail"
}
