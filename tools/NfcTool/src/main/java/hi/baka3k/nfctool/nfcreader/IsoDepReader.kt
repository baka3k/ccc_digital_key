package hi.baka3k.nfctool.nfcreader

import android.nfc.Tag
import android.nfc.tech.IsoDep
import hi.baka3k.nfctool.config.ConfigBuilder
import hi.baka3k.nfctool.config.OptionType
import hi.baka3k.nfctool.config.Technologies

class IsoDepReader(tag: Tag, underlying: String) : NFCTagReader(IsoDep.get(tag)) {
    private val mUnderlying: NFCTagReader

    init {
        (reader as IsoDep).timeout = 5000
        // determine underlying technology
        mUnderlying = if (underlying.equals(Technologies.A)) {
            NfcAReader(tag)
        } else {
            NfcBReader(tag)
        }
    }

    override fun getConfig(): ConfigBuilder {
        val builder = ConfigBuilder()
        val readerIsoDep = reader as IsoDep
        if (mUnderlying is NfcAReader)
            builder.add(OptionType.LA_HIST_BY, readerIsoDep.historicalBytes)
        else
            builder.add(OptionType.LB_H_INFO_RSP, readerIsoDep.hiLayerResponse)
        return builder
    }
}