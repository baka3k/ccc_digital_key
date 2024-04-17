package hi.baka3k.nfctool.nfcreader

import android.nfc.NfcAdapter.ReaderCallback
import android.nfc.Tag
import android.util.Log
import hi.baka3k.nfctool.command.PingPong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NfcReaderCallback(
    private val pingPong: PingPong
) : ReaderCallback {
    private val _mutableStateNfcData: MutableStateFlow<String> = MutableStateFlow("")
    val nfcData: StateFlow<String> = _mutableStateNfcData

    override fun onTagDiscovered(tag: Tag) {
        Log.i(TAG, "New tag discovered")
        _mutableStateNfcData.value = "New Tag discovered"
        val nfcTagReader = createNFCReader(tag)
        if (nfcTagReader != null) {
            nfcTagReader.connect()
            if (nfcTagReader.isConnected()) {
                _mutableStateNfcData.value = "New Tag Connected"
                pingPong.execute(nfcTagReader = nfcTagReader) {
                    _mutableStateNfcData.value = if (it == null) {
                        ""
                    } else {
                        String(it)
                    }
                }
            } else {
                _mutableStateNfcData.value = "New Tag is not connect"
            }
        }
    }

    private fun createNFCReader(tag: Tag): NFCTagReader? {
        return try {
            NFCTagReader.create(tag)
        } catch (e: UnsupportedOperationException) {
            _mutableStateNfcData.value = "New Tag is Support"
            Log.i(TAG, "#createNFCReader() error ${e.message}", e)
            null
        }
    }

    companion object {
        private const val TAG = "Card NfcReaderCallback"
    }
}