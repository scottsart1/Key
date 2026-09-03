package com.example.housekey.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.housekey.data.KeyType
import com.example.housekey.nfc.TagReading

private const val ROUTE_LIST = "list"
private const val ROUTE_EDIT = "edit"
private const val ROUTE_READ = "read"
private const val ROUTE_ABOUT = "about"

/**
 * Top-level composable owning simple state-based navigation between the four
 * screens. Reader-mode control is delegated to the hosting activity.
 */
@Composable
fun AppRoot(
    viewModel: KeyViewModel,
    enableReader: ((TagReading) -> Unit) -> Unit,
    disableReader: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val capabilities by viewModel.capabilities.collectAsState()

    var route by rememberSaveable { mutableStateOf(ROUTE_LIST) }
    var editId by rememberSaveable { mutableStateOf(-1L) }
    var reading by remember { mutableStateOf<TagReading?>(null) }

    BackHandler(enabled = route != ROUTE_LIST) { route = ROUTE_LIST }

    when (route) {
        ROUTE_LIST -> KeyListScreen(
            state = state,
            capabilities = capabilities,
            onAdd = {
                editId = -1L
                route = ROUTE_EDIT
            },
            onEdit = { key ->
                editId = key.id
                route = ROUTE_EDIT
            },
            onEmulate = viewModel::setActive,
            onStop = viewModel::stopEmulating,
            onDelete = viewModel::delete,
            onReadTag = {
                reading = null
                route = ROUTE_READ
            },
            onAbout = { route = ROUTE_ABOUT },
        )

        ROUTE_EDIT -> {
            var initial by remember(editId) { mutableStateOf<KeyDraft?>(null) }
            LaunchedEffect(editId) {
                initial = if (editId >= 0) {
                    viewModel.loadDraft(editId) ?: KeyDraft()
                } else {
                    viewModel.consumePendingImport() ?: KeyDraft()
                }
            }
            initial?.let { init ->
                AddEditKeyScreen(
                    initial = init,
                    isEdit = editId >= 0,
                    onSave = {
                        viewModel.save(it)
                        route = ROUTE_LIST
                    },
                    onBack = { route = ROUTE_LIST },
                )
            }
        }

        ROUTE_READ -> ReadTagScreen(
            reading = reading,
            onEnableReader = { enableReader { r -> reading = r } },
            onDisableReader = disableReader,
            onImport = { r ->
                viewModel.stageImport(
                    KeyDraft(
                        name = "Imported tag",
                        type = r.importType ?: KeyType.NDEF_TEXT,
                        content = r.ndefContent.orEmpty(),
                        note = "UID: ${r.uid}",
                    ),
                )
                editId = -1L
                reading = null
                route = ROUTE_EDIT
            },
            onBack = { route = ROUTE_LIST },
        )

        ROUTE_ABOUT -> AboutScreen(onBack = { route = ROUTE_LIST })
    }
}
