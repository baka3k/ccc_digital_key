package hi.baka3k.nfctool.nfcreader

import android.nfc.Tag
import android.nfc.tech.NfcF

import hi.baka3k.nfctool.config.ConfigBuilder
import hi.baka3k.nfctool.config.OptionType

class NfcFReader(tag: Tag) : NFCTagReader(NfcF.get(tag)) {
    override fun getConfig(): ConfigBuilder {
        val builder = ConfigBuilder()
        val readerF = reader as NfcF

        // join systemcode and nfcid2
        val t3t_identifier_1 = ByteArray(10)
        System.arraycopy(readerF.systemCode, 0, t3t_identifier_1, 0, 2)
        System.arraycopy(readerF.tag.id, 0, t3t_identifier_1, 2, 8)
        // set bit at index 1 to indicate activation of t3t_identifier_1
        val t3t_flags = byteArrayOf(1, 0)

        builder.add(OptionType.LF_T3T_IDENTIFIERS_1, t3t_identifier_1)
        builder.add(OptionType.LF_T3T_FLAGS, t3t_flags)
        builder.add(OptionType.LF_T3T_PMM, readerF.manufacturer)

        return builder
    }
}