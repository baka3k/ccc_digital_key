package hi.baka3k.nfctool.nfcreader

import android.nfc.Tag
import android.nfc.tech.NfcA

import hi.baka3k.nfctool.config.ConfigBuilder
import hi.baka3k.nfctool.config.OptionType

class NfcAReader(tag: Tag) : NFCTagReader(NfcA.get(tag)) {
    override fun getConfig(): ConfigBuilder {
        val builder = ConfigBuilder()
        val readerA = reader as NfcA
        builder.add(OptionType.LA_NFCID1, readerA.tag.id)
        builder.add(OptionType.LA_SEL_INFO, readerA.sak.toByte())
        builder.add(OptionType.LA_BIT_FRAME_SDD, readerA.atqa[0])
        builder.add(OptionType.LA_PLATFORM_CONFIG, readerA.atqa[1])
        return builder
    }
}