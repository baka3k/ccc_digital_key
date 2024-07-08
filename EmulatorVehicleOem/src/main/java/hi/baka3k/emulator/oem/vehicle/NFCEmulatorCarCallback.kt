package hi.baka3k.emulator.oem.vehicle

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.util.Log
import hi.baka3k.nfctool.nfcreader.NFCTagReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NFCEmulatorCarCallback(private val vehicleEmulator: VehicleEmulator) :
    NfcAdapter.ReaderCallback {
    private val _mutableStateNfcData: MutableStateFlow<String> = MutableStateFlow("")
    val nfcData: StateFlow<String> = _mutableStateNfcData
    override fun onTagDiscovered(tag: Tag) {
        Log.i(TAG, "New tag discovered")
        val nfcTagReader = createNFCReader(tag)
        if (nfcTagReader != null) {
            nfcTagReader.connect()
            if (nfcTagReader.isConnected()) {
                _mutableStateNfcData.value = "Start Standard Transaction "
                vehicleEmulator.start(nfcTagReader)
            } else {
                _mutableStateNfcData.value = "New Tag is not connect"
            }
        }
    }

    private fun createNFCReader(tag: Tag): NFCTagReader? {
        return try {
            NFCTagReader.create(tag)
        } catch (e: UnsupportedOperationException) {
            Log.i(TAG, "#createNFCReader() error ${e.message}", e)
            null
        }
    }

    companion object {
        private const val TAG = "NFCEmulatorCarCallback"
    }
}