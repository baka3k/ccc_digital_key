package hi.baka3k.digitalkey.utils

import android.util.Log
import com.payneteasy.tlv.BerTlvParser
import com.payneteasy.tlv.BerTlvs

object TlvUtil {
    private val tlvParser = BerTlvParser()
    fun parseTLV(data: ByteArray): BerTlvs? {
        return try {
            tlvParser.parse(data, 0, data.size)
        } catch (e: IllegalStateException) {
            Log.e("TLVParser", "#parseTLV() error:${e.message}", e)
            null
        } catch (e: Exception) {
            // BerTlvParser has no document about exception can be throw - so generic exception seems to be best options
            Log.e("TLVParser", "#parseTLV() error:${e.message}", e)
            null
        }
    }

}