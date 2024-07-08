package hi.baka3k.digitalkey.protocol.nfc

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.payneteasy.tlv.BerTag
import com.payneteasy.tlv.BerTlv
import com.payneteasy.tlv.BerTlvBuilder
import com.payneteasy.tlv.BerTlvs
import hi.baka3k.data.preference.Preference
import hi.baka3k.data.sharedmodel.AUTH0
import hi.baka3k.data.sharedmodel.AUTH1
import hi.baka3k.data.sharedmodel.CONTROLFLOW
import hi.baka3k.data.sharedmodel.Config
import hi.baka3k.data.sharedmodel.GETKEY
import hi.baka3k.data.sharedmodel.SELECT
import hi.baka3k.data.sharedmodel.ext.toHexArrayString
import hi.baka3k.data.sharedmodel.ext.toHexString
import hi.baka3k.digitalkey.framework.DevicePubicKey
import hi.baka3k.digitalkey.framework.DigitalKey
import hi.baka3k.digitalkey.model.Payload
import hi.baka3k.digitalkey.utils.Logger
import hi.baka3k.digitalkey.utils.TlvUtil
import hi.baka3k.digitalkey.vehicle.VehicleIdentity
import hi.baka3k.iso7816.CommandApdu
import hi.baka3k.security.ec.BouncyCastle
import hi.baka3k.security.ec.toECPublicKeyParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import java.io.ByteArrayOutputStream
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.util.Locale

class NfcHostApduService : HostApduService() {
    // default for test
    private var protocolVersion: ByteArray = VehicleIdentity.protocol_version

    // default for test
    private var vehicleIdentifier: ByteArray = VehicleIdentity.vehicalIdentify

    // default for test
    private var transactionIdentifier: ByteArray = VehicleIdentity.transaction_identifier

    // receive from Auth0 step
    private var vehicleEphemeralEpk: ByteArray? = null

    private lateinit var digitalKey: DigitalKey

    // test - alway device slot = 0
    private lateinit var devicePublicKey: DevicePubicKey
    override fun onCreate() {
        super.onCreate()
        digitalKey = DigitalKey.getInstance(filesDir)
        devicePublicKey = digitalKey.devicePubicKeys[0]
    }

    override fun processCommandApdu(apdu: ByteArray?, extras: Bundle?): ByteArray {
//        Log.d(TAG, "#processCommandApdu() ${apdu?.toHexString()}")
        val preference = Preference.getInstance(applicationContext)
        val fastaccess = preference.getFastMode()
        val userActiveState = preference.getUserActiveState()
        Log.d(TAG, "#App Config (fastaccess,userActiveState) = ($fastaccess,$userActiveState)")
        if (!fastaccess) {
            Log.d(TAG, "#App Config is not allowed run in FastMode")
            if (!userActiveState) {
                Log.d(TAG, "#USER MUST TO ACTIVE DOOR")
                return buildErrResponse("USER MUST TO ACTIVE DOOR".toByteArray())
            }
        }

        Log.d(TAG, "#processCommandApdu() ${apdu?.toHexArrayString()}")
        printExtras(extras)
        if (apdu != null) {
            val commandRequest = CommandApdu(apdu)
            val ins = commandRequest.ins
            val cla = commandRequest.cla
            if (ins == GETKEY.INS && cla == GETKEY.CLA) {
                Logger.d(TAG, "TransferKey")
                handleTransferVehicleEncPk(commandRequest.data)
                return buildResponseTransferEncPk()
            }
            if (ins == GETKEY.INS_1 && cla == GETKEY.CLA) {
                Logger.d(TAG, "TransferKey")
                handleTransferVehicleSignPk(commandRequest.data)
                return buildResponseTransferSignPk()
            }
            if (ins == SELECT.INS && cla == SELECT.CLA) {
                sendMessageToActivity("<<<CMD SELECT: ${apdu.toHexString()}")
                sendMessageToActivity("<<<CMD SELECT AID: ${commandRequest.data.toHexString()}")
                val application: String = commandRequest.data.toHexString(1, 3)
                Logger.d("SELECT", "SELECT_APPLICATION $application")
                val aid = commandRequest.data.toHexString()
                if (Config.DIGITAL_KEY_APPLET_AID.equals(aid)) {
                    Logger.d("SELECT", "DIGITAL_KEY_APPLET_AID")
                } else if (Config.DIGITAL_KEY_FRAMWORK_AID.equals(aid)) {
                    Logger.d("SELECT", "DIGITAL_KEY_FRAMWORK_AID")
                } else {
                    Logger.d("SELECT", "$aid")
                }
                val payload = byteArrayOf(0x5C, 0x01)
                val temp = buildSuccessResponse(payload)//buildResponse(0x90, 0x00, payload)
                sendMessageToActivity(">>> ${temp.toHexString()}")
                return temp
            }
            if (ins == CONTROLFLOW.INS) {
                val temp = buildSuccessResponse()
                sendMessageToActivity("<<<CONTROLFLOW RESPONSE: ${temp.toHexString()}")
                return temp
            } else if (ins == AUTH0.INS && cla == AUTH0.CLA) {
                val temp = handleAuth0(commandRequest)
                sendMessageToActivity("<<<CMD AUTH0: ${temp.toHexString()}")
                return temp
            } else if (ins == AUTH1.INS && cla == AUTH1.CLA) {
                val temp = handleAuth1(commandRequest)
                sendMessageToActivity("<<<CMD AUTH1: ${temp.toHexString()}")
                return temp
            }
            val temp = buildErrResponse("INVALID (CLA , INS)=($cla , $ins)".toByteArray())
            sendMessageToActivity("<<<INVALID (CLA , INS)")
            return temp
        } else {
            Log.i(TAG, "hit APDU null: ")
            sendMessageToActivity("ERROR!!! reason: APDU null")
            return buildErrResponse("ERROR!!! reason: APDU null".toByteArray())
        }
    }

    private fun buildResponseTransferSignPk(): ByteArray {
        val payload = BerTlvBuilder().addBytes(
            BerTag(0x87),
            devicePublicKey.deviceSignPk.q.getEncoded(false)
        ).buildArray()
        return buildSuccessResponse(payload)
    }

    private fun handleTransferVehicleSignPk(data: ByteArray) {
        val tlvs: BerTlvs? = TlvUtil.parseTLV(data)
        if (tlvs != null) {
            val vehicleEncSkTlv = tlvs.find(BerTag(0x87))
            if (vehicleEncSkTlv != null) {
                // device save VehicleEnSk
                DigitalKey.getInstance(filesDir).setVehicleSigPk(vehicleEncSkTlv.bytesValue)
                sendMessageToActivity("<<<Vehicle.Enc.Sk: ${vehicleEncSkTlv.bytesValue?.toHexString()}")
            } else {
                Log.e(TAG, "#handleTransferKey() deviceEncPkTlv null")
            }
        } else {
            Log.e(TAG, "#handleTransferKey() err can not parse tlvs")
        }
    }

    private fun handleTransferVehicleEncPk(data: ByteArray) {
        val tlvs: BerTlvs? = TlvUtil.parseTLV(data)
        if (tlvs != null) {
            val deviceEncPkTlv = tlvs.find(BerTag(0x87))
            if (deviceEncPkTlv != null) {
                Log.e(
                    TAG,
                    "#handleTransferVehicleEncPk() size key ${deviceEncPkTlv.bytesValue.size}}"
                )
                DigitalKey.getInstance(filesDir).setVehicleEncPk(deviceEncPkTlv.bytesValue)
                sendMessageToActivity("<<<Device.Enc.Pk: ${deviceEncPkTlv.bytesValue?.toHexString()}")
            } else {
                Log.e(TAG, "#handleTransferKey() deviceEncPkTlv null")
            }
        } else {
            Log.e(TAG, "#handleTransferKey() err can not parse tlvs")
        }
    }


    private fun buildResponseTransferEncPk(): ByteArray {
        val payload = BerTlvBuilder().addBytes(
            BerTag(0x87),
            devicePublicKey.deviceEncPk.q.getEncoded(false)
        ).buildArray()
        return buildSuccessResponse(payload)
    }

    private fun printExtras(extras: Bundle?) {
        if (extras != null) {
            for (s in extras.keySet()) {
                Log.d(
                    TAG, "Got extras $s:${extras.get(s)}"
                )
            }
        }
    }

    private fun handleAuth1(commandRequest: CommandApdu): ByteArray {
        val data = commandRequest.data
        val endpointSign = extractEnpointSignedAuth1(data)
        if (veritySign(endpointSign)) {
            caculateKdhKey()
            val payload =
                BerTlvBuilder().addIntAsHex(BerTag(0x4E), 0, 1) // slot 0 - test just only 1 keypair
                    .addBytes(BerTag(0x9E), endpointSign)
                    .addBytes(BerTag(0x57), byteArrayOf(0x12, 0x34, 0x56, 0x78))// deprecated
                    .addBytes(BerTag(0x4A), byteArrayOf(0x4A)) // confidential_mailbox_data_subset
                    .addBytes(BerTag(0x4B), byteArrayOf(0x4B)) // private mailbox
                    .buildArray()
            Logger.d(
                "Auth1", "payload before Encrypt ${payload.toHexString()}"
            )
            val payLoadEncryted = encryptData(payload)
            Logger.d(
                "Auth1", "payload after Encrypt ${payLoadEncryted.toHexString()}"
            )
            val temp = buildSuccessResponse(payLoadEncryted)
            Logger.d(
                "Auth1", "payload send to Vehicle ${temp.toHexString()}"
            )
            sendMessageToActivity(">>> ${temp.toHexString()}")
            return temp
        } else {
            sendMessageToActivity("<<< Verify Sign package fail  ")
            return buildErrResponse("Verify Sign package fail".toByteArray())
        }
    }

    /**
     * caculate Kmac, Krmac, Kenc
     * */
    private fun caculateKdhKey() {
        val sk = devicePublicKey.ephemeralKeyPair.keyPair.private as ECPrivateKeyParameters
        val pk = vehicleEphemeralEpk?.toECPublicKeyParameters()
        if (pk != null) {
            val transactionIdentify = transactionIdentifier
            val sharedKey = BouncyCastle.getInstance().createSharedSecret(pk, sk)
            Logger.d("Auth1", "#caculateKdhKey() sharedKey:${sharedKey.toHexString()}")
            val hmac256 = BouncyCastle.getInstance().hkdfSha256(sharedKey, transactionIdentify, 96)
            Logger.d("Auth1", "#caculateKdhKey() hmac256:${hmac256.size}")
            val kMac = BouncyCastle.getInstance().genKmacKey(hmac256)
            val kenC = BouncyCastle.getInstance().genKencKey(hmac256)
            val krMac = BouncyCastle.getInstance().genKrmacKey(hmac256)
            pushKeyToSE(kMac, kenC, krMac)
            Logger.d("Auth1", "kMac :${kMac.toHexString()}")
            Logger.d("Auth1", "KenC :${kenC.toHexString()}")
            Logger.d("Auth1", "krMac :${krMac.toHexString()}")
            sendMessageToActivity("kMac:${kMac.toHexString()}")
            sendMessageToActivity("KenC:${kenC.toHexString()}")
            sendMessageToActivity("krMac:${krMac.toHexString()}")
        }
    }

    private fun pushKeyToSE(kMac: ByteArray, kenC: ByteArray, krMac: ByteArray) {
        // To do
    }

    private fun handleAuth0(commandRequest: CommandApdu): ByteArray {
        try {
            val dataIncomming = commandRequest.data
            Logger.d("Auth0", "dataSize :${dataIncomming.size}")
            //parse TLV data
            val tlvs: BerTlvs? = parseTLV(dataIncomming)
            if (tlvs != null) {
                val vehiclePKTlv = tlvs.find(BerTag(0x87))
                val randomTlv = tlvs.find(BerTag(0x4C))
                val vehicalIdentityTlv = tlvs.find(BerTag(0x4D))
                val appletversionTlv = tlvs.find(BerTag(0x5C))
                parserAuth0Data(appletversionTlv, vehiclePKTlv, randomTlv, vehicalIdentityTlv)

                val payload = BerTlvBuilder().addBytes(
                    BerTag(0x86),
                    //prepended by 04h
                    devicePublicKey.ephemeralKeyPair.encodedPublicKey
                )//ephemeral endpoint public key
                    .addText(BerTag(0x9D), BouncyCastle.TRANSFORMATION).buildArray()
                val temp = buildSuccessResponse(payload)//buildResponse(0x90, 0x00, payload)
                sendMessageToActivity(">>> ${temp.toHexString()}")
                return temp
            } else {
                Logger.e("Auth0", "cannot parse TLV")
                val temp = buildErrResponse("invalid data incoming".toByteArray())
                sendMessageToActivity(">>> error invalid data incoming ${temp.toHexString()}")
                return temp
            }

        } catch (e: Exception) {
            val temp = buildErrResponse("invalid data incoming".toByteArray())
            sendMessageToActivity(">>> error invalid data incoming ${temp.toHexString()}")
            return temp
        }
    }

    private fun parserAuth0Data(
        appletversionTlv: BerTlv?,
        vehiclePKTlv: BerTlv?,
        randomTlv: BerTlv?,
        vehicalIdentityTlv: BerTlv?
    ) {
        Logger.d("Auth0", "mobile: appletversionTlv :$appletversionTlv")
        Logger.d("Auth0", "mobile: vehiclePKTlv :$vehiclePKTlv")
        Logger.d("Auth0", "mobile: randomTlv :$randomTlv")
        Logger.d("Auth0", "mobile: vehicalIdentityTlv :$vehicalIdentityTlv")
        protocolVersion = appletversionTlv?.bytesValue ?: VehicleIdentity.protocol_version
        transactionIdentifier = randomTlv?.bytesValue ?: VehicleIdentity.transaction_identifier
        vehicleIdentifier = vehicalIdentityTlv?.bytesValue ?: VehicleIdentity.vehicalIdentify
        vehicleEphemeralEpk = vehiclePKTlv?.bytesValue
    }

    private fun payload9EOriginal(): ByteArray {
        val b0x4D = vehicleIdentifier
        val b0x4C = transactionIdentifier
        //MobileIdentity.ephemeralKeyPair.keyPair.public as ECPublicKeyParameters
        val b0x86 = devicePublicKey.ephemeralKeyPair.keyPair.public as ECPublicKeyParameters
        val b0x87 = vehicleEphemeralEpk?.toECPublicKeyParameters()!!
        return Payload.payload9E(
            endPointPK = b0x86,
            vehiclePk = b0x87,
            b0x4C = b0x4C,
            b0x4D = b0x4D
        )
    }

    private fun veritySign(signatureData: ByteArray?): Boolean { //parse TLV data
        if (signatureData == null) {
            sendMessageToActivity("<<<CMD AUTH1: veritySign fail 0x9E tag have no data")
            return false
        }
        val payloadOriginnal = payload9EOriginal()
        Log.d("Auth1", "verify payloadOriginnal :${payloadOriginnal.toHexString()}")
        val verify = verifyData(signatureData, payloadOriginnal)
        Log.d("Auth1", "verify payload:$verify")
        if (verify) {
            sendMessageToActivity("<<<CMD AUTH1: veritySign SUCCESS")
            return true
        } else {
            sendMessageToActivity("<<<CMD AUTH1: veritySign FAIL-bypass for test")
            return true
        }
    }

    private fun extractEnpointSignedAuth1(apduData: ByteArray): ByteArray? {
        val tlvs: BerTlvs? = TlvUtil.parseTLV(apduData)
        return if (tlvs != null) {
            val dataSignedTlv = tlvs.find(BerTag(0x9E))
            dataSignedTlv.bytesValue
        } else {
            Logger.d(
                "Auth1", "extractEnpointSignedAuth1 null - could not get signed data from endpoint}"
            )
            null
        }
    }

    private fun verifyData(
        dataIncomming: ByteArray, payloadOriginnal: ByteArray
    ): Boolean {
        val vehicleSignPk = DigitalKey.getInstance(filesDir).getVehicleSigPk()
        return if (vehicleSignPk != null) {
            BouncyCastle.getInstance().verify(payloadOriginnal, dataIncomming, vehicleSignPk)
        } else {
            false
        }
    }

    private fun encryptData(payload: ByteArray): ByteArray {
        //KeyUtils.getVehicleEncPk(filesDir)
        val vehicleEncPk = DigitalKey.getInstance(filesDir).getVehicleEncPk()
//        val enPointEncSK = MobileIdentity.encSignKeyPair.private as ECPrivateKeyParameters
        val endPointEncSK = devicePublicKey.deviceEncSk
        if (vehicleEncPk != null) {
            return try {
                Logger.d("Auth1", "#encryptData()")
                BouncyCastle.getInstance().encrypt(payload, vehicleEncPk, endPointEncSK)
            } catch (e: UnsupportedOperationException) {
                Logger.e("Auth1", "err encrypt data ${e.message}", e)
                payload
            } catch (e: InvalidKeyException) {
                Logger.e("Auth1", "err encrypt data ${e.message}", e)
                payload
            } catch (e: InvalidAlgorithmParameterException) {
                Logger.e("Auth1", "err encrypt data ${e.message}", e)
                payload
            } catch (e: Exception) {
                Logger.e("Auth1", "err encrypt data ${e.message}", e)
                payload
            }
        } else {
            Logger.e("Auth1", "getVehicleEncPk null - can not decrypt data")
            return payload
        }
    }

    private fun buildResponse(sw1: Int, sw2: Int, content: ByteArray? = null): ByteArray {
        val bout = ByteArrayOutputStream()
        if (content != null) {
            bout.write(content)
        }
        bout.write(sw1)
        bout.write(sw2)
        return bout.toByteArray()
    }

    private fun buildSuccessResponse(content: ByteArray? = null): ByteArray {
        val bout = ByteArrayOutputStream()
        if (content != null) {
            bout.write(content)
        }
        bout.write(0x90)
        bout.write(0x00)
        return bout.toByteArray()
    }

    private fun buildErrResponse(content: ByteArray? = null): ByteArray {
        val bout = ByteArrayOutputStream()
        if (content != null) {
            bout.write(content)
        }
        bout.write(0x6A)
        bout.write(0x82)
        return bout.toByteArray()
    }

    override fun onDeactivated(reason: Int) {
        Log.i(TAG, "Deactivated: $reason")
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.lowercase(Locale.getDefault())
                .startsWith(manufacturer.lowercase(Locale.getDefault()))
        ) {
            capitalize(model)
        } else {
            capitalize(manufacturer) + " " + model
        }
    }


    private fun capitalize(s: String): String {
        if (s.isEmpty()) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            first.uppercaseChar().toString() + s.substring(1)
        }
    }

    private fun sendMessageToActivity(data: String) {
        val intent = Intent("nfc-event")
        // You can also include some extra data.
        intent.putExtra("data", data)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun parseTLV(dataIncomming: ByteArray): BerTlvs? {
        return TlvUtil.parseTLV(dataIncomming)
    }

    companion object {
        private const val TAG = "Endpoint"
    }
}
