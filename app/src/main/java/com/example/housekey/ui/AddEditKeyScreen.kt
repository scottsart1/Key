package com.example.housekey.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.housekey.R
import com.example.housekey.data.ApduPairs
import com.example.housekey.data.KeyType
import com.example.housekey.util.Hex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditKeyScreen(
    initial: KeyDraft,
    isEdit: Boolean,
    onSave: (KeyDraft) -> Unit,
    onBack: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    var nameError by remember { mutableStateOf<Int?>(null) }
    var contentError by remember { mutableStateOf<Int?>(null) }
    var aidError by remember { mutableStateOf<Int?>(null) }
    var apduError by remember { mutableStateOf<Int?>(null) }

    fun validateAndSave() {
        nameError = if (draft.name.isBlank()) R.string.err_name_required else null
        contentError = null
        aidError = null
        apduError = null
        when (draft.type) {
            KeyType.NDEF_TEXT, KeyType.NDEF_URI ->
                if (draft.content.isBlank()) contentError = R.string.err_content_required
            KeyType.RAW_APDU -> {
                val aidBytes = Hex.decodeOrNull(draft.aid)
                if (aidBytes == null || aidBytes.size < 5 || aidBytes.size > 16) {
                    aidError = R.string.err_aid_invalid
                }
                if (ApduPairs.firstInvalidLine(draft.apduPairs) != null) {
                    apduError = R.string.err_hex_invalid
                }
            }
        }
        val ok = nameError == null && contentError == null && aidError == null && apduError == null
        if (ok) onSave(draft)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEdit) R.string.title_edit_key else R.string.title_add_key,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { validateAndSave() }) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = { Text(stringResource(R.string.label_name)) },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.label_type), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip(KeyType.NDEF_TEXT, draft.type, R.string.type_ndef_text) { draft = draft.copy(type = it) }
                TypeChip(KeyType.NDEF_URI, draft.type, R.string.type_ndef_uri) { draft = draft.copy(type = it) }
                TypeChip(KeyType.RAW_APDU, draft.type, R.string.type_raw_apdu) { draft = draft.copy(type = it) }
            }

            when (draft.type) {
                KeyType.NDEF_TEXT -> OutlinedTextField(
                    value = draft.content,
                    onValueChange = { draft = draft.copy(content = it) },
                    label = { Text(stringResource(R.string.label_text)) },
                    isError = contentError != null,
                    supportingText = contentError?.let { { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth(),
                )

                KeyType.NDEF_URI -> OutlinedTextField(
                    value = draft.content,
                    onValueChange = { draft = draft.copy(content = it) },
                    label = { Text(stringResource(R.string.label_uri)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    isError = contentError != null,
                    supportingText = contentError?.let { { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth(),
                )

                KeyType.RAW_APDU -> {
                    OutlinedTextField(
                        value = draft.aid,
                        onValueChange = { draft = draft.copy(aid = it) },
                        label = { Text(stringResource(R.string.label_aid)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        isError = aidError != null,
                        supportingText = aidError?.let { { Text(stringResource(it)) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.selectResponse,
                        onValueChange = { draft = draft.copy(selectResponse = it) },
                        label = { Text(stringResource(R.string.label_select_response)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.apduPairs,
                        onValueChange = { draft = draft.copy(apduPairs = it) },
                        label = { Text(stringResource(R.string.label_apdu_pairs)) },
                        supportingText = {
                            Text(apduError?.let { stringResource(it) } ?: stringResource(R.string.hint_apdu_pairs))
                        },
                        isError = apduError != null,
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            OutlinedTextField(
                value = draft.note,
                onValueChange = { draft = draft.copy(note = it) },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeChip(
    type: KeyType,
    selected: KeyType,
    labelRes: Int,
    onSelect: (KeyType) -> Unit,
) {
    FilterChip(
        selected = type == selected,
        onClick = { onSelect(type) },
        label = { Text(stringResource(labelRes)) },
    )
}
