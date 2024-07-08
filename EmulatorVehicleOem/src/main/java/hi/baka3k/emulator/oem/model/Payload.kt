package hi.baka3k.emulator.oem.model

import com.payneteasy.tlv.BerTag
import com.payneteasy.tlv.BerTlvBuilder
import hi.baka3k.emulator.oem.vehicle.VehicleIdentity
import org.bouncycastle.crypto.params.ECPublicKeyParameters

object Payload {
    private fun payload9E(
        endPointPK: ECPublicKeyParameters,
        vehiclePk: ECPublicKeyParameters
    ): ByteArray {
        return payload9E(endPointPK = endPointPK, vehiclePk = vehiclePk)
    }

    fun payload9E(
        endPointPK: ECPublicKeyParameters,
        vehiclePk: ECPublicKeyParameters,
        b0x4D: ByteArray = VehicleIdentity.vehicalIdentify,
        b0x4C: ByteArray = VehicleIdentity.transaction_identifier,
        b0x93: String = "415D9569",
    ): ByteArray {
        return BerTlvBuilder()
            .addBytes(BerTag(0x4D), b0x4D)
            .addBytes(BerTag(0x86), extractPkX(endPointPK))
            .addBytes(BerTag(0x87), extractPkX(vehiclePk))
            .addBytes(BerTag(0x4C), b0x4C) // transaction Identifier
            .addHex(BerTag(0x93), b0x93)//usage = 415D9569h
            .buildArray()
    }

    private fun extractPkX(publicKey: ECPublicKeyParameters): ByteArray {
        return publicKey.q.affineXCoord.encoded
    }

    private fun extractPkX(keyAsByteArray: ByteArray): ByteArray {
        if (keyAsByteArray.size > 32) {
            return keyAsByteArray.copyOfRange(0, 32)
        }
        return keyAsByteArray
    }
}