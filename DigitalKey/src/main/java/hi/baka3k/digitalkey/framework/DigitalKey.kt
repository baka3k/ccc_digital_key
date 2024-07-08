package hi.baka3k.digitalkey.framework

import hi.baka3k.data.sharedmodel.ext.toRandomByte
import hi.baka3k.digitalkey.utils.toPublicKey
import hi.baka3k.digitalkey.utils.toSecretKey
import hi.baka3k.digitalkey.utils.writePublicKey
import hi.baka3k.digitalkey.utils.writeSecretKey
import hi.baka3k.security.ec.BouncyCastle
import hi.baka3k.security.ec.ECKeyUtils
import hi.baka3k.security.ec.toByteArray
import org.bouncycastle.crypto.EphemeralKeyPair
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import java.io.File

/**
 * 4.2 Digital Key Structure
 * A Digital Key structure is stored in the applet instance and contains a public/private key pair, a
 * private mailbox, a confidential mailbox, and other elements, as shown in Figure 4-2.
 * An owner Digital Key consists of the Digital Key structure only. It does not have any limitations
 * in validity and access rights.
 * A friend Digital Key consists of the Digital Key structure and an entitlements attestation section,
 * which is linked to the friend key by containing the same device public key and by being signed
 * by the owner key.
 * */
class DigitalKey private constructor(
    private val filesDir: File,
    private val cipher: BouncyCastle = BouncyCastle.getInstance()
) {
    val vehicalIdentify = 8.toRandomByte()
    val endPointIdentify = 8.toRandomByte()
    val digitalKeyIdentify = 8.toRandomByte()
    val slotIdentify = 0

    private var vehicleEncSk: ECPrivateKeyParameters? = null
    private var vehicleEncPk: ECPublicKeyParameters? = null

    private var vehicleSigSk: ECPrivateKeyParameters? = null
    private var vehicleSigPk: ECPublicKeyParameters? = null
    val devicePubicKeys = mutableListOf<DevicePubicKey>()

    init {
        val deviceKeys = File(filesDir, "devicekeys")
        if (!deviceKeys.exists()) {
            deviceKeys.mkdirs()
        }
        val devicePubicKey = DevicePubicKey(deviceKeys, cipher)
        devicePubicKeys.add(devicePubicKey)
    }

    fun getVehicleSigPk(): ECPublicKeyParameters? {
        if (vehicleSigPk == null) {
            val vehicleOemSigPkFile = File(filesDir, ECKeyUtils.Vehicle_OEM_Sig_PK)
            vehicleSigPk = vehicleOemSigPkFile.toPublicKey()
        }
        return vehicleSigPk
    }

    fun setVehicleSigPk(encoder: ByteArray) {
        val vehicleOemSigPkFile = File(filesDir, ECKeyUtils.Vehicle_OEM_Sig_PK)
        vehicleOemSigPkFile.writePublicKey(encoder)
        vehicleSigPk = vehicleOemSigPkFile.toPublicKey()
    }

    fun setVehicleEncPk(encoder: ByteArray) {
        val vehicleOemEncPkFile = File(filesDir, ECKeyUtils.Vehicle_OEM_Enc_PK)
        vehicleOemEncPkFile.writePublicKey(encoder)
        vehicleEncPk = vehicleOemEncPkFile.toPublicKey()
    }

    fun getVehicleEncPk(): ECPublicKeyParameters? {
        if (vehicleEncPk == null) {
            val vehicleOemEncPkFile = File(filesDir, ECKeyUtils.Vehicle_OEM_Enc_PK)
            vehicleEncPk = vehicleOemEncPkFile.toPublicKey()
        }
        return vehicleEncPk
    }

    fun loadVehicleKey() {
        loadVehicleEncKey()
        loadVehicleSigKey()
    }

    fun loadVehicleSigKey() {
        val vehicleOemSigPkFile = File(filesDir, ECKeyUtils.Vehicle_OEM_Sig_PK)
        val vehicleOemSigSkFile = File(filesDir, ECKeyUtils.Vehicle_OEM_Sig_SK)
        vehicleSigPk = vehicleOemSigPkFile.toPublicKey()
        vehicleSigSk = vehicleOemSigSkFile.toSecretKey()
    }

    fun loadVehicleEncKey() {
        val vehicleOemEncPkFile = File(filesDir, ECKeyUtils.Vehicle_OEM_Enc_PK)
        val vehicleOemEncSkFile = File(filesDir, ECKeyUtils.Vehicle_OEM_Enc_SK)
        vehicleEncPk = vehicleOemEncPkFile.toPublicKey()
        vehicleEncSk = vehicleOemEncSkFile.toSecretKey()
    }

    companion object {
        private var instance: DigitalKey? = null
        fun getInstance(filesDir: File): DigitalKey {
            return instance ?: synchronized(this) {
                instance ?: DigitalKey(filesDir).also { instance = it }
            }
        }
    }
}

class DevicePubicKey(
    private val filesDir: File, private val cipher: BouncyCastle = BouncyCastle.getInstance()
) {
    val slotIdentify = 0
    val keyFriendName = "Friend_0"
    val deviceEncSk: ECPrivateKeyParameters
    val deviceEncPk: ECPublicKeyParameters

    val deviceSignSk: ECPrivateKeyParameters
    val deviceSignPk: ECPublicKeyParameters

    lateinit var ephemeralKeyPair: EphemeralKeyPair

    init {
        generateEphemeralKey()
        val keyPairEnc = cipher.genECKeyPair()
        deviceEncSk = keyPairEnc.private as ECPrivateKeyParameters
        deviceEncPk = keyPairEnc.public as ECPublicKeyParameters
        saveDeviceEncKey()

        val keyPairSign = cipher.genECKeyPair()
        deviceSignSk = keyPairSign.private as ECPrivateKeyParameters
        deviceSignPk = keyPairSign.public as ECPublicKeyParameters
        saveDeviceSignKey()
        generateEphemeralKey()
    }

    private fun saveDeviceEncKey() {
        val deviceOemEncPkFile = File(filesDir, ECKeyUtils.Devx_Enc_PK)
        val deviceOemEncSkFile = File(filesDir, ECKeyUtils.Devx_Enc_SK)
        deviceOemEncPkFile.writePublicKey(deviceEncPk.toByteArray())
        deviceOemEncSkFile.writeSecretKey(deviceEncSk.toByteArray())
    }

    private fun saveDeviceSignKey() {
        val deviceOemSignPkFile = File(filesDir, ECKeyUtils.Devx_Sig_PK)
        val deviceOemSignSkFile = File(filesDir, ECKeyUtils.Devx_Sig_SK)
        deviceOemSignPkFile.writePublicKey(deviceSignSk.toByteArray())
        deviceOemSignSkFile.writeSecretKey(deviceSignPk.toByteArray())
    }

    fun generateEphemeralKey(): EphemeralKeyPair {
        ephemeralKeyPair = cipher.genEphemeralKeyPair()
        return ephemeralKeyPair//EcDsa.generateEphemeralKeyPair()
    }
}