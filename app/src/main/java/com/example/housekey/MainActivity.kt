package com.example.housekey

import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.housekey.nfc.NfcReader
import com.example.housekey.nfc.TagReading
import com.example.housekey.ui.AppRoot
import com.example.housekey.ui.KeyViewModel
import com.example.housekey.ui.theme.HouseKeyTheme

class MainActivity : ComponentActivity() {

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(this) }
    private val viewModel: KeyViewModel by viewModels()

    /** Set while the read-tag screen is visible; receives parsed tag readings. */
    private var onTagRead: ((TagReading) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HouseKeyTheme {
                AppRoot(
                    viewModel = viewModel,
                    enableReader = { callback -> enableReaderMode(callback) },
                    disableReader = { disableReaderMode() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCapabilities()
    }

    override fun onPause() {
        super.onPause()
        // Ensure reader mode never lingers into card-emulation use.
        disableReaderMode()
    }

    private fun enableReaderMode(onRead: (TagReading) -> Unit) {
        onTagRead = onRead
        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
        nfcAdapter?.enableReaderMode(
            this,
            { tag ->
                val result = NfcReader.read(tag)
                runOnUiThread { onTagRead?.invoke(result) }
            },
            flags,
            null,
        )
    }

    private fun disableReaderMode() {
        onTagRead = null
        nfcAdapter?.disableReaderMode(this)
    }
}
