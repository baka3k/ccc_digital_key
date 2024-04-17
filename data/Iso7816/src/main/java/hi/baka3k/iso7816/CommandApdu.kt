package hi.baka3k.iso7816


/**
 * ISO/IEC 7816-4 Organization
 * https://en.wikipedia.org/wiki/Smart_card_application_protocol_data_unit
 * */
class CommandApdu {
    var cla = 0x00
    var ins = 0x00
    var p1 = 0x00
    var p2 = 0x00
    private var lc = 0x00
    private var mData = ByteArray(0)
    private var mLe = 0x00
    private var mLeUsed = false

    constructor(cla: Int, ins: Int, p1: Int, p2: Int) {
        this.cla = cla
        this.ins = ins
        this.p1 = p1
        this.p2 = p2
    }

    constructor()
    constructor(cla: Int, ins: Int, p1: Int, p2: Int, data: ByteArray) {
        this.cla = cla
        this.ins = ins
        lc = data.size
        this.p1 = p1
        this.p2 = p2
        mData = data.clone()
    }

    constructor(cla: Int, ins: Int, p1: Int, p2: Int, data: ByteArray, le: Int) {
        this.cla = cla
        this.ins = ins
        lc = data.size
        this.p1 = p1
        this.p2 = p2
        mData = data.clone()
        mLe = le
        mLeUsed = true
    }

    constructor(cla: Int, ins: Int, p1: Int, p2: Int, le: Int) {
        this.cla = cla
        this.ins = ins
        this.p1 = p1
        this.p2 = p2
        mLe = le
        mLeUsed = true
    }

    var data: ByteArray
        get() = mData
        /** Sets Data field of the APDU  */
        set(data) {
            lc = data.size
            mData = data
        }
    var le: Int
        get() = mLe
        /** Sets the Le field of the command  */
        set(le) {
            mLe = le
            mLeUsed = true
        }

    /** Returns the APDU in byte[] format  */
    fun toBytes(): ByteArray {
        var length = 4 // CLA, INS, P1, P2
        if (mData.isNotEmpty()) {
            length += 1 // LC
            length += mData.size // DATA
        }
        if (mLeUsed) {
            length += 1 // LE
        }
        val apdu = ByteArray(length)
        var index = 0
        apdu[index] = cla.toByte()
        index++
        apdu[index] = ins.toByte()
        index++
        apdu[index] = p1.toByte()
        index++
        apdu[index] = p2.toByte()
        index++
        if (mData.isNotEmpty()) {
            apdu[index] = lc.toByte()
            index++
            System.arraycopy(mData, 0, apdu, index, mData.size)
            index += mData.size
        }
        if (mLeUsed) {
            apdu[index] = (apdu[index] + mLe.toByte()).toByte()

        }
        return apdu
    }

    /** Clones the APDU  */
    fun clone(): CommandApdu {
        val apdu = CommandApdu()
        apdu.cla = cla
        apdu.ins = ins
        apdu.p1 = p1
        apdu.p2 = p2
        apdu.lc = lc
        apdu.mData = ByteArray(mData.size)
        System.arraycopy(mData, 0, apdu.mData, 0, mData.size)
        apdu.mLe = mLe
        apdu.mLeUsed = mLeUsed
        return apdu
    }

    companion object {
        /** Returns true if both the headers are same  */
        fun compareHeaders(header1: ByteArray, mask: ByteArray, header2: ByteArray): Boolean {
            if (header1.size < 4 || header2.size < 4) {
                return false
            }
            val compHeader = ByteArray(4)
            compHeader[0] = (header1[0].toInt() and mask[0].toInt()).toByte()
            compHeader[1] = (header1[1].toInt() and mask[1].toInt()).toByte()
            compHeader[2] = (header1[2].toInt() and mask[2].toInt()).toByte()
            compHeader[3] = (header1[3].toInt() and mask[3].toInt()).toByte()
            return compHeader[0] == header2[0] && compHeader[1] == header2[1] && compHeader[2] == header2[2] && compHeader[3] == header2[3]
        }
    }

    /**
     * Constructs a CommandAPDU from a byte array containing the complete
     * APDU contents (header and body).
     *
     *
     * Note that the apdu bytes are copied to protect against
     * subsequent modification.
     *
     * @param apdu the complete command APDU
     *
     * @throws NullPointerException if apdu is null
     * @throws IllegalArgumentException if apdu does not contain a valid
     * command APDU
     */
    constructor(apdu: ByteArray) {
        parse(apdu.clone())
    }

    /**
     * Command APDU encoding options:
     *
     * case 1:  |CLA|INS|P1 |P2 |                                 len = 4
     * case 2s: |CLA|INS|P1 |P2 |LE |                             len = 5
     * case 3s: |CLA|INS|P1 |P2 |LC |...BODY...|                  len = 6..260
     * case 4s: |CLA|INS|P1 |P2 |LC |...BODY...|LE |              len = 7..261
     * case 2e: |CLA|INS|P1 |P2 |00 |LE1|LE2|                     len = 7
     * case 3e: |CLA|INS|P1 |P2 |00 |LC1|LC2|...BODY...|          len = 8..65542
     * case 4e: |CLA|INS|P1 |P2 |00 |LC1|LC2|...BODY...|LE1|LE2|  len =10..65544
     *
     * LE, LE1, LE2 may be 0x00.
     * LC must not be 0x00 and LC1|LC2 must not be 0x00|0x00
     */
    private fun parse(apdu: ByteArray) {
        if (apdu.size < 4) {
            throw IllegalArgumentException("apdu must be at least 4 bytes long")
        }
        cla = apdu[0].toInt() and 0xff
        ins = apdu[1].toInt() and 0xff
        p1 = apdu[2].toInt() and 0xff
        p2 = apdu[3].toInt() and 0xff
        var nc = 0
        var dataOffset = 0
        if (apdu.size == 4) {
            val data = ByteArray(4)
            copyArray(apdu, dataOffset, data, 0, nc)
            this.mData = data
        }
        val l1: Int = apdu[4].toInt() and 0xff
        var l2 = apdu[4].toInt() and 0xff
        if (apdu.size == 5) {
            val data = ByteArray(4)
            copyArray(apdu, dataOffset, data, 0, nc)
            this.mData = data
        }
        if (apdu.size > 5) {
            if (l1 != 0) {
                when (apdu.size) {
                    4 + 1 + l1 -> {
                        nc = l1
                        dataOffset = 5
                    }

                    4 + 2 + l1 -> {
                        // case 4s
                        nc = l1
                        dataOffset = 5
                        l2 = apdu[apdu.size - 1].toInt() and 0xff
                    }
                }
            }
            l2 = apdu[5].toInt() and 0xff shl 8 or (apdu[6].toInt() and 0xff)
            if (apdu.size == 4 + 3 + l2) {
                // case 3e
                nc = l2
                dataOffset = 7
            } else if (apdu.size == 4 + 5 + l2) {
                // case 4e
                nc = l2
                dataOffset = 7
            }
        }
        val data = ByteArray(nc)
        copyArray(apdu, dataOffset, data, 0, nc)
        cla = apdu[0].toInt() and 0xff
        ins = apdu[1].toInt() and 0xff
        p1 = apdu[2].toInt() and 0xff
        p2 = apdu[3].toInt() and 0xff
        lc = data.size
        this.mData = data
    }

    private fun copyArray(
        src: ByteArray,
        srcPos: Int,
        dest: ByteArray,
        destPos: Int,
        length: Int
    ) {
        System.arraycopy(src, srcPos, dest, destPos, length)
    }
}
