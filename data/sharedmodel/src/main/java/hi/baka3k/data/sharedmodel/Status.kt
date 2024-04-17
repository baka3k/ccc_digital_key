package hi.baka3k.data.sharedmodel

import hi.baka3k.data.sharedmodel.ext.hexStringToByteArray


object Status {
    //6700h wrong length
//6A80h Incorrect parameters in command payload
//6A82h file not found
//6B00h wrong P1 or P2
//6C00h wrong Le
//6D00h wrong INS code
//6E00h wrong CLA code
//9000h command successfully executed
    val SUCCESS = "9000".hexStringToByteArray()
    val NOT_FOUND = "6A82".hexStringToByteArray()
}