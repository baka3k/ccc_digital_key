package hi.baka3k.emulator.oem.vehicle

import android.util.Log
import com.payneteasy.tlv.BerTag
import com.payneteasy.tlv.BerTlvBuilder
import com.payneteasy.tlv.BerTlvs
import hi.baka3k.data.sharedmodel.AUTH0
import hi.baka3k.data.sharedmodel.AUTH1
import hi.baka3k.data.sharedmodel.CONTROLFLOW
import hi.baka3k.data.sharedmodel.Config
import hi.baka3k.data.sharedmodel.SELECT
import hi.baka3k.emulator.oem.model.Payload
import hi.baka3k.emulator.oem.utils.TlvUtil
import hi.baka3k.emulator.oem.utils.toPublicKey
import hi.baka3k.nfctool.data.CommandApdu
import hi.baka3k.nfctool.data.ResponseAPDU
import hi.baka3k.nfctool.nfcreader.NFCTagReader
import hi.baka3k.nfctool.utils.Logger
import hi.baka3k.nfctool.utils.hexStringToByteArray
import hi.baka3k.nfctool.utils.toHexString
import hi.baka3k.security.ec.BouncyCastle
import hi.baka3k.security.ec.ECKeyUtils
import hi.baka3k.security.ec.toECPublicKeyParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import java.io.File
import java.security.SecureRandom


class VehicleEmulator(
    private val filesDir: File, private val callback: (String) -> Unit
) {
    private lateinit var nfcTagReader: NFCTagReader

    private val vehicalIdentityValue = VehicleIdentity.vehicalIdentify
    private val random = SecureRandom()
    private var transactionIdentifier = VehicleIdentity.transaction_identifier
    private var ephemeralDevicePk: ByteArray? = null
    private var cryptogram = BouncyCastle.TRANSFORMATION // default
    fun start(tagreader: NFCTagReader) {
        nfcTagReader = tagreader
        callback("START TRANSACTION")
        if (selectAPDU()) {
            callback("SELECT AID SUCCESS")
            if (auth0()) {
                callback("AUTH0 SUCCESS")
                if (auth1()) {
                    callback("AUTH1 SUCCESS")
                    if (exchange()) {
                        callback("EXCHANGE SUCCESS")
                        if (flowControl()) {
                            callback("CONTROL FLOW SUCCESS")
                        } else {
                            callback("ERROR: CONTROL FLOW FAIL")
                            logd("ERROR: CONTROL FLOW command FAIL")
                        }
                    } else {
                        callback("ERROR: EXCHANGE FAIL")
                        logd("ERROR: EXCHANGE FAIL")
                    }
                } else {
                    callback("ERROR: AUTH1 FAIL")
                    logd("ERROR: AUTH1 FAIL")
                }
            } else {
                callback("ERROR: AUTH0 FAIL")
                logd("ERROR: AUTH0 FAIL")
            }
        } else {
            callback("ERROR: COULD NOT SELECT APDU")
            logd("ERROR: COULD NOT SELECT APDU")
        }
    }

    /**
     * 15.3.2.16 CONTROL FLOW command
     * This command allows the vehicle to indicate the final success or failure of the transaction or to signal application-specific codes.
     * The P2 field is used for vehicle error codes, domain specific codes, or timing information.
     * The vehicle error codes are hints provided by the vehicle to the device in case of a transaction failure. The decision to send such error codes is vehicle implementation-specific.
     * command: CLA3 3C [Table 15-42] [Table 15-43] response: 90 00
     * */
    private fun flowControl(): Boolean {
        val flowControlCommand = CommandApdu(
            cla = CONTROLFLOW.CLA, // o 84 - in test we used 80
            ins = CONTROLFLOW.INS,
            p1 = CONTROLFLOW.P1_SUCCESS,
            p2 = CONTROLFLOW.P2,
            le = 0x00
        )
        callback(">>>${flowControlCommand.toBytes().toHexString()}")
        val byteArray = sendCommandToMobile(command = flowControlCommand.toBytes())
        callback("<<<${byteArray.toHexString()}")
        val response = ResponseAPDU(byteArray)
        caculateKdhKey()
        return response.getSW1() == 0x90 && response.getSW2() == 0x00
    }
    private fun caculateKdhKey() {
        val sk = VehicleIdentity.ephemeralKeyPair.keyPair.private as ECPrivateKeyParameters
        val pk = ephemeralDevicePk?.toECPublicKeyParameters()
        if (pk != null) {
            val transactionIdentify = transactionIdentifier
            val sharedKey = BouncyCastle.getInstance().createSharedSecret(pk, sk)
            Logger.d("Auth1", "#caculateKdhKey() sharedKey:${sharedKey.toHexString()}")
            val hmac256 = BouncyCastle.getInstance().hkdfSha256(sharedKey, transactionIdentify, 96)
            Logger.d("Auth1", "#caculateKdhKey() hmac256:${hmac256.size}")
            val kMac = BouncyCastle.getInstance().genKmacKey(hmac256)
            val kenC = BouncyCastle.getInstance().genKencKey(hmac256)
            val krMac = BouncyCastle.getInstance().genKrmacKey(hmac256)
            Logger.d("Auth1", "kMac :${kMac.toHexString()}")
            Logger.d("Auth1", "KenC :${kenC.toHexString()}")
            Logger.d("Auth1", "krMac :${krMac.toHexString()}")
            callback("kMac:${kMac.toHexString()}")
            callback("KenC:${kenC.toHexString()}")
            callback("krMac:${krMac.toHexString()}")
        }
    }

    private fun exchange(): Boolean {
        // optional - return true for test
        return true
    }

    private fun buildPayloadAuth1(): ByteArray {
        val payloadOriginnal9E = payload9EOriginal()
        Log.d("Auth1", "PayloadZinWillBeSigned:${payloadOriginnal9E.toHexString()}")
//        val payLoad9ESigned = EcDsa.signData(payloadOriginnal9E, VehicleIdentity.signKeyPair.secretKey)
        val payLoad9ESigned = BouncyCastle.getInstance().sign(
            payloadOriginnal9E,
            VehicleIdentity.signKeyPair.private as ECPrivateKeyParameters
        )
        return BerTlvBuilder()
            .addBytes(BerTag(0x9E), payLoad9ESigned).buildArray()
    }

    /**
     * 15.3.2.10 AUTH1 command
     * This command allows mutual authentication and establishment of a secure channel between the vehicle and device.
     * This command may be used to optionally pre-compute a ranging key. If this option is implemented,
     * this command shall produce 80 bytes of keying material and the computed ranging key shall subsequently be
     * made available upon reception of the CREATE RANGING KEY command. Otherwise, this command shall produce 48 bytes of keying material only
     * command: CLA3 81 00 00 Lc [Table 15-32] 00 response: [encrypted_payload] [mac] 9000
     * The CLA3 is as defined in Table 15-3
     * */
    private fun auth1(): Boolean {
//        val endPointPK = KeyUtils.getEphemeralDevicePk(filesDir)
//        val endPointPK = ephemeralDevicePk!!.encodedPublicKeyToECPublicKeyParameters()
        Logger.e("Auth1", "Start command aut1")
        val payloadSendToDevice = buildPayloadAuth1()
        Logger.e("Auth1", "Signed payload and sent $payloadSendToDevice")
        val auth1Command = CommandApdu(
            cla = AUTH1.CLA,
            ins = AUTH1.INS,
            p1 = 0x00,
            p2 = 0x00,
            data = payloadSendToDevice,
            le = 0x00
        )
        callback(">>>${auth1Command.toBytes().toHexString()}")
        val mobileResponse = sendCommandToMobile(command = auth1Command.toBytes())
        val response = ResponseAPDU(mobileResponse)
        callback("<<<${mobileResponse.toHexString()}")
        Logger.d(
            "Auth1", "auth1 response (SW1,SW2) = (${response.getSW1()},${response.getSW2()})"
        )
//            if (response.getSW1() == 0x90 && response.getSW2() == 0x00) {
        val encryptedDataIncomming = response.getData()
        val sw1 = response.getSW1()
        val sw2 = response.getSW2()
        Logger.d("Auth1", "BEGIN DECRYPT data = ${encryptedDataIncomming.toHexString()}")
        if (sw1 == 0x90 && sw2 == 0x00) {
            Logger.d("Auth1", "BEGIN DECRYPT Start Decrypt")
            val (decryptData, success) = decryptData(encryptedDataIncomming)
            Logger.d("Auth1", "DECRYPT status $success")
            Logger.d("Auth1", "auth1 response DECRYPTED = ${decryptData.toHexString()}")
            val tlvs: BerTlvs? = parseTLV(decryptData)
            Logger.d("Auth1", "auth1 BerTlvs tlvs = $tlvs")
            if (tlvs != null) {
                Logger.d("Auth1", "DECRYPT status ok to parse data")
            }
//                return success
        } else {
            Logger.d(
                "Auth1",
                "DECRYPT status error (sw1,sw2) =($sw1,$sw2) / (${0x90},${0x00})"
            )
        }//
        //return false
        return true //bypass all only for test
//        val response = sendCommandToMobile(command = auth1Command.getBytes())
    }

    private fun payload9EOriginal(): ByteArray {
        val b0x4D = vehicalIdentityValue
        val b0x4C = transactionIdentifier
        val b0x86 = ephemeralDevicePk!!.toECPublicKeyParameters()
        val b0x87 = VehicleIdentity.ephemeralKeyPair.keyPair.public as ECPublicKeyParameters
        return Payload.payload9E(
            endPointPK = b0x86,
            vehiclePk = b0x87,
            b0x4C = b0x4C,
            b0x4D = b0x4D
        )
    }
    //cla: 0x80
    //ins: 0x5C
    //p1: 0x00
    //p2: 0x00
    //data: [0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F]

    /**
     * 15.3.2.9 AUTH0 command
     * This command allows the vehicle to initiate the authentication procedure.
     * In case a fast transaction is requested by the vehicle, a cryptogram is returned,
     * allowing the vehicle to tentatively proceed with a fast transaction.
     * command: CLA3 80 P1 P2 Lc [Table 15-29] 00 response: [Table 15-30] 9000
     * The CLA3 is as defined in Table 15-3.
     * */
    private fun auth0(): Boolean {
        transactionIdentifier = VehicleIdentity.transaction_identifier// getRandom()
        val payload = BerTlvBuilder()
            .addBytes(BerTag(0x5C), VehicleIdentity.protocol_version)
            .addBytes(
                BerTag(0x87),
                //prepended by 04h
                VehicleIdentity.ephemeralKeyPair.encodedPublicKey
            )
            .addBytes(BerTag(0x4C), transactionIdentifier)
            .addBytes(BerTag(0x4D), vehicalIdentityValue)
            .buildArray()
        val auth0Command = CommandApdu(
            cla = AUTH0.CLA, // o 84 - in test we used 80
            ins = AUTH0.INS,
            p1 = 0x00,
            p2 = 0x00,
            data = payload, // test - return version sample
            le = 0x00
        )

        val byteArray = sendCommandToMobile(command = auth0Command.toBytes())
        callback(">>>${auth0Command.toBytes().toHexString()}")
        val response = ResponseAPDU(byteArray)
        callback(">>>${byteArray.toHexString()}")
        if (response.getSW1() == 0x90 && response.getSW2() == 0x00) {
            val dataIncomming = response.getData()
            val tlvs: BerTlvs? = parseTLV(dataIncomming)
            if (tlvs != null) {
                val mobilePKTlv = tlvs.find(BerTag(0x86))
                val cryptogramTlv = tlvs.find(BerTag(0x9D))
                if (cryptogramTlv != null) {
                    val text = cryptogramTlv.textValue
                    if (text.isNullOrEmpty()) {
                        cryptogram = text
                    }
                }
                Logger.d("Auth0", "vehicle_emulator: mobilePKTlv :$mobilePKTlv")
                Logger.d("Auth0", "vehicle_emulator: cryptogramTlv :${cryptogramTlv?.textValue}")

                if (mobilePKTlv.bytesValue != null) {
                    ephemeralDevicePk = mobilePKTlv.bytesValue
                } else {
                    Logger.e(
                        "Auth0",
                        "Can not save Ephemeral Device Pk - mobilePKTlv.bytesValue null"
                    )
                }

            } else {
                Logger.e("Auth0", "#auth0() tlvs null cannot parse data")
            }
        }
        return true
    }

    private fun parseTLV(decryptData: ByteArray): BerTlvs? {
        return TlvUtil.parseTLV(decryptData)
    }


    private fun selectAPDU(): Boolean {
//        val command = vehicalCommand.selectAIDCmd()
        val command = CommandApdu(
            cla = SELECT.CLA,
            ins = SELECT.INS,
            p1 = 0x04,
            p2 = 0x00,
            data = Config.DIGITAL_KEY_FRAMWORK_AID.hexStringToByteArray(), // AID_DIGITAL_KEY
            le = 0x00
        )
        callback(">>>${command.toBytes().toHexString()}")
        val mobileResponse = sendCommandToMobile(command.toBytes())
        val response = ResponseAPDU(mobileResponse)
        callback("<<${response.getBytes().toHexString()}")
        val sw1 = response.getSW1()
        val sw2 = response.getSW2()
        Logger.d("SELECT", "(sw1,sw2) = (${response.getSW1()},${response.getSW2()})")
        return sw1 == 0x90 && sw2 == 0x00
    }

    /** *******************************************************************************************
     * SUPPORT FUNCTION
     * *******************************************************************************************/
    private fun sendCommandToMobile(command: ByteArray): ByteArray {
        return nfcTagReader.transceive(command) ?: byteArrayOf(0x67, 0x00)
    }

    private fun logd(content: String) {
        Log.d(TAG, content)
    }

    fun loge(content: String, exception: Exception) {
        Log.e(TAG, content, exception)
    }

    private fun getRandom(): ByteArray {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes
    }


    private fun decryptData(payload: ByteArray): Pair<ByteArray, Boolean> {
        val endPointPublicKey = getEndpointPKFromFile()
        val vehicleSecretKey = VehicleIdentity.cryptoBoxKeyPair.private as? ECPrivateKeyParameters
        return if (endPointPublicKey != null && vehicleSecretKey != null) {
            try {
                val decryptData =
                    BouncyCastle.getInstance().decrypt(payload, endPointPublicKey, vehicleSecretKey)
                callback("DECRYPT DATA SUCCESS")
                Pair(decryptData, true)
            } catch (e: Exception) {
                callback("DECRYPT DATA Fail - by pass only for test")
                Logger.e("Auth1", "decryptData error ${e.message}", e)
                Pair(payload, false)
            }
        } else {
            Logger.e("Auth1", "getDeviceEncPk null - can not decrypt data")
            Pair(payload, false)
        }
    }

    private fun getEndpointPKFromFile(): ECPublicKeyParameters? {
        val fileKey = File(filesDir, ECKeyUtils.Devx_Enc_PK)
        return fileKey.toPublicKey()
    }

    companion object {
        const val TAG = "VehicleEmulator"
    }
}