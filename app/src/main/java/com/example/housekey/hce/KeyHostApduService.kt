package com.example.housekey.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Android entry point for Host Card Emulation. Delegates the actual APDU logic to
 * the pure [ApduProcessor], and reloads the active credential from
 * [EmulationStore] at the start of each interaction (on the application SELECT)
 * so that changes made in the UI take effect on the very next tap.
 */
class KeyHostApduService : HostApduService() {

    private lateinit var store: EmulationStore
    private lateinit var processor: ApduProcessor

    override fun onCreate() {
        super.onCreate()
        store = EmulationStore(this)
        processor = ApduProcessor(store.load())
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val cmd = commandApdu ?: return ApduProcessor.SW_WRONG_LENGTH
        if (ApduProcessor.isSelectByName(cmd)) {
            processor.setActive(store.load())
        }
        return processor.process(cmd)
    }

    override fun onDeactivated(reason: Int) {
        processor.reset()
    }
}
