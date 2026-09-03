package com.example.housekey.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.housekey.R

/** A compact warning row with an icon, used for capability/setup problems. */
@Composable
fun WarningBanner(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Shows every capability/setup warning relevant to the current device state. */
@Composable
fun CapabilityWarnings(capabilities: Capabilities, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            !capabilities.nfcSupported -> WarningBanner(stringResource(R.string.warn_no_nfc))
            !capabilities.hceSupported -> WarningBanner(stringResource(R.string.warn_no_hce))
            !capabilities.nfcEnabled -> WarningBanner(stringResource(R.string.warn_nfc_disabled))
        }
        if (capabilities.nfcSupported && capabilities.hceSupported && !capabilities.defaultForNdef) {
            WarningBanner(stringResource(R.string.warn_not_default))
        }
    }
}

/** Prominent card showing whether a key is being emulated, with a Stop action. */
@Composable
fun StatusCard(
    activeKeyName: String?,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = activeKeyName != null
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val icon: ImageVector = Icons.Filled.Contactless
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (active) {
                        stringResource(R.string.status_active_format, activeKeyName!!)
                    } else {
                        stringResource(R.string.status_inactive)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (active) {
                    Text(
                        text = stringResource(R.string.status_tap_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (active) {
                OutlinedButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.action_stop),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}
