package hi.baka3k.data.sharedmodel.ext

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
fun String.hexStringToByteArray(): ByteArray {
    val result = ByteArray(length / 2)
    for (i in indices step 2) {
        val firstIndex = HEX_CHARS.indexOf(this[i])
        val secondIndex = HEX_CHARS.indexOf(this[i + 1])

        val octet = firstIndex.shl(4).or(secondIndex)
        result[i.shr(1)] = octet.toByte()
    }
    return result
}