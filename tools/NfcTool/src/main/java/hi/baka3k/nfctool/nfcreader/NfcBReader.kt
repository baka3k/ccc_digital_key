package hi.baka3k.nfctool.nfcreader

import android.nfc.Tag
import android.nfc.tech.NfcB

import hi.baka3k.nfctool.config.ConfigBuilder
import hi.baka3k.nfctool.config.OptionType
class NfcBReader(tag: Tag) : NFCTagReader(NfcB.get(tag)) {
    override fun getConfig(): ConfigBuilder {
        val builder = ConfigBuilder()
        val readerB = reader as NfcB

        builder.add(OptionType.LB_NFCID0, readerB.tag.id)
        builder.add(OptionType.LB_APPLICATION_DATA, readerB.applicationData)
        builder.add(OptionType.LB_SFGI, readerB.protocolInfo[0])
        builder.add(OptionType.LB_SENSB_INFO, readerB.protocolInfo[1])
        builder.add(OptionType.LB_ADC_FO, readerB.protocolInfo[2])

        return builder
    }
}