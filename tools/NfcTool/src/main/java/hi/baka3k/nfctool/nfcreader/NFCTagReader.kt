package hi.baka3k.nfctool.nfcreader

import android.nfc.Tag
import android.nfc.tech.TagTechnology
import android.text.TextUtils
import android.util.Log

import hi.baka3k.nfctool.config.ConfigBuilder
import hi.baka3k.nfctool.config.Technologies
import java.io.IOException
import java.lang.reflect.Method


abstract class NFCTagReader(val reader: TagTechnology) {
    abstract fun getConfig(): ConfigBuilder
    open fun isConnected(): Boolean {
        return reader.isConnected
    }

    open fun transceive(command: ByteArray?): ByteArray? {
        return try {
            // there is no common interface for TagTechnology...
            val transceive: Method = reader.javaClass.getMethod(
                "transceive", ByteArray::class.java
            )
            transceive.invoke(reader, command) as ByteArray
        } catch (e: Exception) {
            Log.e(TAG, "#transceive() err  ${e.message}", e)
            null
        }
    }

    open fun connect() {
        try {
            reader.connect()
        } catch (e: IOException) {
            Log.e(TAG, "#connect() err  ${e.message}", e)
        }
    }

    open fun close() {
        try {
            reader.close()
        } catch (e: IOException) {
            Log.e(TAG, "#close() err  ${e.message}", e)
        }
    }

    companion object {
        const val TAG = "NFCTagReader"

        @Throws(UnsupportedOperationException::class)
        fun create(tag: Tag): NFCTagReader {
            val technologies = tag.techList.toList()
            if (technologies.contains(Technologies.IsoDep)) {
                // an IsoDep tag can be backed by either NfcA or NfcB technology
                if (technologies.contains(Technologies.A)) {
                    return IsoDepReader(tag, Technologies.A)
                } else if (technologies.contains(Technologies.B)) {
                    return IsoDepReader(tag, Technologies.B)
                } else {
                    Log.e(
                        TAG,
                        "Unknown tag technology backing IsoDep ${
                            TextUtils.join(
                                ", ",
                                technologies
                            )
                        }"
                    )
                }
            }
            for (tech in technologies) {
                when (tech) {
                    Technologies.A -> return NfcAReader(tag)
                    Technologies.B -> return NfcBReader(tag)
                    Technologies.F -> return NfcFReader(tag)
                    Technologies.V -> return NfcVReader(tag)
                }
            }
            throw UnsupportedOperationException("Unknown Tag type")
        }
    }
}