package hi.baka3k.data.sharedmodel

/** Card commands we support.
 * Define Command/function to execute
 * **/
object CLA {
    const val CLA_OPERATION_OK = 0x00
    const val CLA_STATUS_OK = 0x91

    const val CLA_READ_DATA = 0x00
    const val CLA_WRITE_DATA = 0x02
    const val CLA_LOCK_DATA = 0x04
    const val CLA_DECODE_DATA = 0x08
    const val CLA_CREATE_CHARATER_CONFIRM_1 = 0x80
    const val CLA_CREATE_CHARATER_CONFIRM_2 = 0x84
    const val CLA_CREATE_CA = 0x81
    const val CLA_EXECUTE_TRANSACTION = 0x82
}