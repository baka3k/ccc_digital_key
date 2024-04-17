package hi.baka3k.data.sharedmodel.ext

import java.math.BigInteger
import java.security.SecureRandom
import java.util.Base64

val random = SecureRandom()
fun Int.toRandomByte(): ByteArray {
    val byteArray = ByteArray(this)
    random.nextBytes(byteArray)
    return byteArray
}

/**
 * Converts the byte array to HEX string.
 *
 * @param buffer
 * the buffer.
 * @return the HEX string.
 */
fun ByteArray.toHexString(): String {
    return toHexString(0, this.size)
}

/**
 * Converts the byte array to HEX string.
 *
 * @param buffer
 * the buffer.
 * @return the HEX string.
 */
fun ByteArray.toHexString(offset: Int, length: Int): String {
    val sb = StringBuilder()
    for (i in offset until (offset + length)) {
        val b = this[i]
        val octet = b.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        sb.append(HEX_CHARS[firstIndex])
        sb.append(HEX_CHARS[secondIndex])
    }
    return sb.toString()
}

fun ByteArray.toHexArrayString(
    separator: CharSequence = ",",
    prefix: CharSequence = "{",
    postfix: CharSequence = "}"
) =
    this.joinToString(separator, prefix, postfix) {
        String.format("0x%02X", it)
    }

fun ByteArray.toBigInteger(signum: Int = 1): BigInteger {
    return BigInteger(signum, this)
}

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

fun ByteArray.toPublicKeyString(): String {
    val base64PubKey = Base64.getEncoder().encodeToString(this)
    return "-----BEGIN PUBLIC KEY-----\n" + base64PubKey.replace(
        "(.{64})".toRegex(),
        "$1\n"
    ) + "\n-----END PUBLIC KEY-----\n"
}

fun ByteArray.toSecretKeyString(): String {
    val base64PubKey = Base64.getEncoder().encodeToString(this)
    return "-----BEGIN PRIVATE KEY-----\n" + base64PubKey.replace(
        "(.{64})".toRegex(),
        "$1\n"
    ) + "\n-----END PRIVATE KEY-----\n"
}