package hi.baka3k.emulator.oem.vehicle

import hi.baka3k.nfctool.utils.toHexString
import hi.baka3k.nfctool.utils.toRandomByte
import hi.baka3k.security.ec.BouncyCastle

object VehicleIdentity {
    // ID vehicle - in simulation project I choose random value,
    val protocol_version = byteArrayOf(0x01, 0x00)
    // transaction id - default for test
    val transaction_identifier = 16.toRandomByte()
    // ID vehicle - in simulation project I choose random hash,
    val vehicalIdentify = 8.toRandomByte()

    // original key store in Vehical, in simulation project I choose random hash,
    val ephemeralKeyPair = BouncyCastle.getInstance().genEphemeralKeyPair()// EcDsa.generateEphemeralKeyPair()
    val cryptoBoxKeyPair = BouncyCastle.getInstance().genECKeyPair() // EcDsa.generateCryptoBoxKeyPair()
    val signKeyPair = BouncyCastle.getInstance().genECKeyPair()
    val digitalKeyIdentifer = byteArrayOf(0x02, 0x06, 0x01, 0x09, 0x08, 0x06).toHexString()
}
