package hi.baka3k.nfctool.command

import hi.baka3k.nfctool.nfcreader.NFCTagReader

abstract class PingPong {
    abstract fun execute(
        nfcTagReader: NFCTagReader,
        onReponse: (ByteArray?) -> Unit
    )
}