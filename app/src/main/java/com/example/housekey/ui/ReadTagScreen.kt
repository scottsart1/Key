package com.example.housekey.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.housekey.R
import com.example.housekey.nfc.TagReading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadTagScreen(
    reading: TagReading?,
    onEnableReader: () -> Unit,
    onDisableReader: () -> Unit,
    onImport: (TagReading) -> Unit,
    onBack: () -> Unit,
) {
    // Reader mode replaces HCE while this screen is visible; restore it on exit.
    DisposableEffect(Unit) {
        onEnableReader()
        onDispose { onDisableReader() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_read_tag)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.Nfc,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.read_hint), style = MaterialTheme.typography.bodyLarge)

            if (reading == null) {
                Text(
                    stringResource(R.string.read_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.read_uid, reading.uid), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.read_tech, reading.techs.joinToString(", ")),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (reading.ndefContent != null) {
                            Text(
                                stringResource(R.string.read_ndef, reading.ndefContent),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            Text(
                                stringResource(R.string.read_cannot_import),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Button(
                    onClick = { onImport(reading) },
                    enabled = reading.importType != null,
                ) {
                    Text(stringResource(R.string.read_import))
                }
            }
        }
    }
}
