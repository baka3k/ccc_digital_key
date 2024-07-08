package com.example.nfctag.setting.transferkey.vehicle

import com.payneteasy.tlv.BerTag
import com.payneteasy.tlv.BerTlvBuilder
import com.payneteasy.tlv.BerTlvs
import hi.baka3k.data.sharedmodel.Config
import hi.baka3k.data.sharedmodel.GETKEY
import hi.baka3k.data.sharedmodel.SELECT
import hi.baka3k.digitalkey.utils.TlvUtil
import hi.baka3k.digitalkey.utils.writePublicKey
import hi.baka3k.emulator.oem.vehicle.VehicleIdentity
import hi.baka3k.nfctool.data.CommandApdu
import hi.baka3k.nfctool.data.ResponseAPDU
import hi.baka3k.nfctool.nfcreader.NFCTagReader
import hi.baka3k.nfctool.utils.Logger
import hi.baka3k.nfctool.utils.hexStringToByteArray
import hi.baka3k.nfctool.utils.toHexString
import hi.baka3k.security.ec.ECKeyUtils
import hi.baka3k.security.ec.toByteArray
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import java.io.File

class KeyEmulator(private val filesDir: File, private val callback: (String) -> Unit) {
    private lateinit var nfcTagReader: NFCTagReader
    fun start(tagreader: NFCTagReader) {
        nfcTagReader = tagreader
        if (select()) {
            if (transferEncPk()) {
                callback("Transfer EncPk Success")
                if (transferSignPk()) {
                    callback("Transfer SigPk Success")
                }
            } else {
                callback("Transfer Pk Fail")
            }
        } else {
            Logger.e("TAG", "#Can not select AID")
            callback("Can not select AID")
        }
    }

    private fun transferSignPk(): Boolean {
        val payload = BerTlvBuilder()
            .addBytes(BerTag(0x87), getVehicleSigSk().toByteArray())
            .buildArray()
        val getPublicKeyCommand = CommandApdu(
            cla = GETKEY.CLA, // o 84 - in test we used 80
            ins = GETKEY.INS_1,
            p1 = 0x00,
            p2 = 0x00,
            data = payload, // test - return version sample
            le = 0x00
        )
        val byteArray = sendCommandToMobile(command = getPublicKeyCommand.toBytes())
        val response = ResponseAPDU(byteArray)
        callback(">>>vehicle.Sign.Pk: ${getVehicleSigSk().toByteArray().toHexString()}")
        callback(">>>${getPublicKeyCommand.toBytes().toHexString()}")
        callback("<<<${byteArray.toHexString()}")
        if (response.getSW1() == 0x90 && response.getSW2() == 0x00) {
            val dataIncomming = response.getData()
            val tlvs: BerTlvs? = parseTLV(dataIncomming)
            return if (tlvs != null) {
                val deviceSigPkTlv = tlvs.find(BerTag(0x87))
                if (deviceSigPkTlv != null && deviceSigPkTlv.bytesValue != null) {
                    // vehicle save deviceEnPk
                    saveDeviceSignPk(deviceSigPkTlv.bytesValue, filesDir)
                    true
                } else {
                    Logger.e(TAG, "#transferKey() deviceEncPkTlv null")
                    true // ignore for test
                }
            } else {
                Logger.e(TAG, "#transferKey() BerTlvs cannot parse data")
                true // ignore for test
            }
        }
        return true
    }

    private fun transferEncPk(): Boolean {
        val payload = BerTlvBuilder()
            .addBytes(BerTag(0x87), getVehicleEncPk().toByteArray())
            .buildArray()
        val getPublicKeyCommand = CommandApdu(
            cla = GETKEY.CLA, // o 84 - in test we used 80
            ins = GETKEY.INS,
            p1 = 0x00,
            p2 = 0x00,
            data = payload, // test - return version sample
            le = 0x00
        )


        val byteArray = sendCommandToMobile(command = getPublicKeyCommand.toBytes())
        callback(">>>vehicle.Enc.Pk: ${getVehicleEncPk().toByteArray().toHexString()}")
        callback(">>>${getPublicKeyCommand.toBytes().toHexString()}")
        callback("<<<${byteArray.toHexString()}")
        val response = ResponseAPDU(byteArray)
        if (response.getSW1() == 0x90 && response.getSW2() == 0x00) {
            val dataIncomming = response.getData()
            val tlvs: BerTlvs? = parseTLV(dataIncomming)
            if (tlvs != null) {
                val deviceEncPkTlv = tlvs.find(BerTag(0x87))
                if (deviceEncPkTlv != null && deviceEncPkTlv.bytesValue != null) {
                    // vehicle save deviceEnPk
                    saveDeviceEncPk(deviceEncPkTlv.bytesValue, filesDir)
                    callback("<<<device.Enc.Pk: ${deviceEncPkTlv.bytesValue.toHexString()}")
                } else {
                    Logger.e(TAG, "#transferKey() deviceEncPkTlv null")
                }
            } else {
                Logger.e(TAG, "#transferKey() BerTlvs cannot parse data")
            }
        }
        return true
    }

    private fun select(): Boolean {
        val command = CommandApdu(
            cla = SELECT.CLA,
            ins = SELECT.INS,
            p1 = 0x04,
            p2 = 0x00,
//            data = Config.DIGITAL_KEY_FRAMWORK_AID.hexStringToByteArray(), // AID_DIGITAL_KEY
            data = Config.DIGITAL_KEY_APPLET_AID.hexStringToByteArray(), // AID_DIGITAL_KEY
            le = 0x00
        )
        val mobileResponse = sendCommandToMobile(command.toBytes())
        val response = ResponseAPDU(mobileResponse)
        val sw1 = response.getSW1()
        val sw2 = response.getSW2()
        Logger.d("SELECT", "(sw1,sw2) = (${response.getSW1()},${response.getSW2()})")
        return sw1 == 0x90 && sw2 == 0x00
    }


    /** *******************************************************************************************
     * SUPPORT FUNCTION
     * *******************************************************************************************/
    private fun getVehicleEncPk(): ECPublicKeyParameters {
        return VehicleIdentity.cryptoBoxKeyPair.public as ECPublicKeyParameters
    }

    private fun getVehicleSigSk(): ECPublicKeyParameters {
        return VehicleIdentity.signKeyPair.public as ECPublicKeyParameters
    }


    private fun sendCommandToMobile(command: ByteArray): ByteArray {
        return nfcTagReader.transceive(command) ?: byteArrayOf(0x67, 0x00)
    }

    private fun parseTLV(decryptData: ByteArray): BerTlvs? {
        return TlvUtil.parseTLV(decryptData)
    }

    private fun saveDeviceSignPk(bytesValue: ByteArray, filesDir: File) {
        val fileKey = File(filesDir, ECKeyUtils.Devx_Sig_PK)
        fileKey.writePublicKey(bytesValue)
    }

    //    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun saveDeviceEncPk(bytesValue: ByteArray, filesDir: File) {
        val fileKey = File(filesDir, ECKeyUtils.Devx_Enc_PK)
        fileKey.writePublicKey(bytesValue)
    }

    companion object {
        private const val TAG = "KeyEmulator"
    }
}