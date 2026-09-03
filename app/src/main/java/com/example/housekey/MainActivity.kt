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

    /** Callback that receives parsed tag readings while the read screen is open. */
    private var onTagRead: ((TagReading) -> Unit)? = null

    /**
     * Whether the read screen currently wants reader mode. Reader mode is bound to
     * the foreground: the OS tears it down on pause, so we re-arm it on resume as
     * long as the screen still wants it. Without this, backgrounding while on the
     * read screen would silently leave reader mode off.
     */
    private var readerRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HouseKeyTheme {
                AppRoot(
                    viewModel = viewModel,
                    enableReader = { callback -> requestReaderMode(callback) },
                    disableReader = { releaseReaderMode() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCapabilities()
        if (readerRequested) startReaderHardware()
    }

    override fun onPause() {
        super.onPause()
        // Stop the hardware but keep the request, so onResume can re-arm it.
        stopReaderHardware()
    }

    /** Called by the read screen when it appears; arms reader mode. */
    private fun requestReaderMode(onRead: (TagReading) -> Unit) {
        onTagRead = onRead
        readerRequested = true
        startReaderHardware()
    }

    /** Called by the read screen when it leaves; fully disarms reader mode. */
    private fun releaseReaderMode() {
        readerRequested = false
        onTagRead = null
        stopReaderHardware()
    }

    private fun startReaderHardware() {
        if (onTagRead == null) return
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

    private fun stopReaderHardware() {
        nfcAdapter?.disableReaderMode(this)
    }
}
