package hi.baka3k.nfctool.data

/**
 * A response APDU as defined in ISO/IEC 7816-4. It consists of a conditional
 * body and a two byte trailer.
 * https://en.wikipedia.org/wiki/Smart_card_application_protocol_data_unit
 * */
class ResponseAPDU(apdu: ByteArray) {
    private val apdu: ByteArray

    init {
        check(apdu)
        this.apdu = apdu.clone()
    }

    private fun check(apdu: ByteArray) {
        require(apdu.size >= 2) { "apdu must be at least 2 bytes long" }
    }

    /**
     * Returns the number of data bytes in the response body (Nr) or 0 if this
     * APDU has no body. This call is equivalent to
     * `getData().length`.
     *
     * @return the number of data bytes in the response body or 0 if this APDU
     * has no body.
     */
    fun getNr(): Int {
        return apdu.size - 2
    }

    /**
     * Returns a copy of the data bytes in the response body. If this APDU as
     * no body, this method returns a byte array with a length of zero.
     *
     * @return a copy of the data bytes in the response body or the empty
     * byte array if this APDU has no body.
     */
    fun getData(): ByteArray {
        val data = ByteArray(apdu.size - 2)
        copyArray(apdu, 0, data, 0, data.size)
        return data
    }

    /**
     * Returns the value of the status byte SW1 as a value between 0 and 255.
     *
     * @return the value of the status byte SW1 as a value between 0 and 255.
     */
    fun getSW1(): Int {
        return apdu[apdu.size - 2].toInt() and 0xff
    }

    /**
     * Returns the value of the status byte SW2 as a value between 0 and 255.
     *
     * @return the value of the status byte SW2 as a value between 0 and 255.
     */
    fun getSW2(): Int {
        return apdu[apdu.size - 1].toInt() and 0xff
    }

    /**
     * Returns the value of the status bytes SW1 and SW2 as a single
     * status word SW.
     * It is defined as
     * `(getSW1() << 8) | getSW2()`.
     *
     * @return the value of the status word SW.
     */
    private fun getSW(): Int {
        return getSW1() shl 8 or getSW2()
    }

    /**
     * Returns a copy of the bytes in this APDU.
     *
     * @return a copy of the bytes in this APDU.
     */
    fun getBytes(): ByteArray {
        return apdu.clone()
    }

    /**
     * Returns a string representation of this response APDU.
     *
     * @return a String representation of this response APDU.
     */
    override fun toString(): String {
        return ("ResponseAPDU: " + apdu.size + " bytes, SW="
                + Integer.toHexString(getSW()))
    }

    /**
     * Compares the specified object with this response APDU for equality.
     * Returns true if the given object is also a ResponseAPDU and its bytes are
     * identical to the bytes in this ResponseAPDU.
     *
     * @param obj the object to be compared for equality with this response APDU
     * @return true if the specified object is equal to this response APDU
     */
    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj !is ResponseAPDU) {
            return false
        }
        val other: ResponseAPDU = obj
        return apdu.contentEquals(other.apdu)
    }

    /**
     * Returns the hash code value for this response APDU.
     *
     * @return the hash code value for this response APDU.
     */
    override fun hashCode(): Int {
        return apdu.contentHashCode()
    }
//
//    @Throws(IOException::class, ClassNotFoundException::class)
//    private fun readObject(objectInputStream: ObjectInputStream) {
//        apdu = objectInputStream.readUnshared() as ByteArray
//        check(apdu)
//    }

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