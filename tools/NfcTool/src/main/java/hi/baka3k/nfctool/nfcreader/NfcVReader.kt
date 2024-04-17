package hi.baka3k.nfctool.nfcreader

import android.nfc.Tag
import android.nfc.tech.NfcV

import hi.baka3k.nfctool.config.ConfigBuilder

class NfcVReader(tag: Tag) : NFCTagReader(NfcV.get(tag)) {
    override fun getConfig(): ConfigBuilder {
        // TODO: V tags cannot be emulated (yet)
        return ConfigBuilder()
    }
}