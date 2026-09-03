package com.example.housekey.hce

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import com.example.housekey.util.Hex

/**
 * Thin wrapper over the platform NFC/HCE APIs: reports capability/state and
 * registers or clears the dynamic AIDs used by raw-APDU keys.
 */
class HceManager(context: Context) {

    private val appContext = context.applicationContext
    private val component = ComponentName(appContext, KeyHostApduService::class.java)
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(appContext)
    private val cardEmulation: CardEmulation? =
        nfcAdapter?.let { CardEmulation.getInstance(it) }

    fun isNfcSupported(): Boolean = nfcAdapter != null

    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    fun isHceSupported(): Boolean =
        appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)

    /** Registers a dynamic AID (category "other") so the OS routes it to us. */
    fun registerRawAid(aid: ByteArray): Boolean = runCatching {
        cardEmulation?.registerAidsForService(
            component,
            CardEmulation.CATEGORY_OTHER,
            listOf(Hex.encode(aid)),
        ) ?: false
    }.getOrDefault(false)

    /** Removes any dynamically registered AIDs, leaving only the static NDEF AID. */
    fun unregisterRawAids(): Boolean = runCatching {
        cardEmulation?.removeAidsForService(component, CardEmulation.CATEGORY_OTHER) ?: false
    }.getOrDefault(false)

    /**
     * Whether this app would win routing for the NDEF Tag AID. When false, another
     * app may intercept reads and the user should set HouseKey as default in NFC
     * settings. Returns true if the state cannot be determined.
     */
    fun isDefaultForNdef(): Boolean = runCatching {
        cardEmulation?.isDefaultServiceForAid(component, NDEF_AID_HEX) ?: true
    }.getOrDefault(true)

    private companion object {
        const val NDEF_AID_HEX = "D2760000850101"
    }
}
