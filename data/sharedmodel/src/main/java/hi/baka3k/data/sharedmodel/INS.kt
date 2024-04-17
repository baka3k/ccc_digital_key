package hi.baka3k.data.sharedmodel

import hi.baka3k.data.sharedmodel.ext.hexStringToByteArray


/**
 * Application Function Code
 * define Application code to execute
 * */
object INS {
    const val INS_SELECT_APPLICATION = 0x5A //
    const val INS_CMD_SELECT = 0xA4
    const val INS_SPAKE2_REQUEST = 0x30
    const val INS_SPAKE2_VERITY = 0x32
    const val INS_WRITE_DATA = 0xD4
    const val INS_GET_DATA = 0xCA
    const val INS_GET_RESPONSE = 0xC0
    const val INS_OP_CONTROL_FLOW = 0x3C

    const val INS_GET_PUBLIC_KEY = 0x04
    const val INS_AUTHENTICATE = 0x11
    const val INS_AUTHENTICATE_0 = 0x10
    const val INS_AUTHENTICATE_1 = 0x11
    const val INS_GET_CARD_INFO = 0x14
}

object SELECT {
    const val CLA = 0x00 //
    const val INS = 0xA4
}

object AUTH0 {
    const val CLA = 0x80
    const val INS = 0x80
}

object AUTH1 {
    const val CLA = 0x80
    const val INS = 0x81
}

object CONTROLFLOW {
    const val INS = 0x3C
    const val CLA = 0x80
    const val P1_FAIL = 0x00
    const val P1_SUCCESS = 0x01
    const val P1_AP = 0x40
    const val P2 = 0x00 // test
}
object GETKEY {
    const val CLA = 0x88
    const val INS = 0x88
    const val INS_1 = 0x89
}
object SUCCESS {
    const val SW1 = 0x90
    const val SW2 = 0x00
}

object ResponseStatus {
    val INVALID = "6400".hexStringToByteArray()
    val NOT_AVAILABLE = "6402".hexStringToByteArray()
    val DATA_NOT_FOUND = "6A88".hexStringToByteArray()
    val COMMAND_OUT_OF_SEQUENCE = "6985".hexStringToByteArray()
}
