package hi.baka3k.nfctool.config

import hi.baka3k.nfctool.utils.toHexString

class ConfigOption(private val ID: OptionType, private val data: ByteArray) {
    constructor(ID: OptionType, data: Byte) : this(ID, byteArrayOf(data))

    fun size(): Int {
        return data.size
    }

    fun push(data: ByteArray, offset: Int) {
        data[offset + 0] = ID.getID()
        data[offset + 1] = data.size.toByte()
        System.arraycopy(data, 0, data, offset + 2, data.size)
    }

    override fun toString(): String {
        val result = StringBuilder()
        result.append("Type: ")
        result.append(ID.toString())
        if (data.size > 1) {
            result.append(" (")
            result.append(data.size)
            result.append(")")
        }
        result.append(", Value: 0x")
        result.append(data.toHexString())
        return result.toString()
    }
}