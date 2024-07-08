package com.example.nfctag

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.nfctag.emulator.endpoint.NFCEmulationCardActivity
import com.example.nfctag.emulator.vehicle.EmulatorCarActivity
import com.example.nfctag.setting.SettingsActivity
import com.example.nfctag.setting.TransferKeyActivity
import com.example.nfctag.setting.transferkey.device.DeviceSyncKeyActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun openNFCReader(view: View) {
//        val intent = Intent(this, NFCReaderActivity::class.java)
//        if (view.id == R.id.nfcReaderFelica) {
//            intent.putExtra("type", NfcReaderType.NFC_A)
//        } else {
//            intent.putExtra("type", NfcReaderType.ISO_DEP)
//        }
        val intent = Intent(this, EmulatorCarActivity::class.java)
        startActivity(intent)
    }

    fun openNFCEmulation(view: View) {
        val intent = Intent(this, NFCEmulationCardActivity::class.java)
        startActivity(intent)
    }

    fun openNFCFelicaEmulation(view: View) {

    }

    fun openNFCFelicaReader(view: View) {

    }

    fun openTransferKeyActivity(view: View) {
        val intent = Intent(this, TransferKeyActivity::class.java)
        startActivity(intent)
//        test()
    }

    fun openSettingActivity(view: View) {
        //FFE3E3
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun test() {
//        val hexByteArray = "3337453030343631463831434135384135464635303737453038373434303744364643454330313543433132353835323142333442423830313632353342303844443738464539443132354344423542434437464442434237463138334431344536364339383543443046313833413930323134383145334339393732464433434341343142463033433735324341453333433532463735444139323731393631383946354439374441363041303844394538303432353537354342303931313334304534363839374434333338344533313939384531393931463032323631393243313745314332453041384533393845373245464537324433333546444342393232364131343544334135434538343539413439324446463439414332354131424446323437".hexStringToByteArray()
//        KeyUtils.saveDeviceEncPk(MobileIdentity.cryptoBoxKeyPair.publicKey.asBytes,filesDir)
//        val deviceEncPk = KeyUtils.getDeviceEncPk(filesDir)
//  val
//        KeyUtils.saveVehicleEncPk(VehicleIdentity.cryptoBoxKeyPair.publicKey.asBytes,filesDir)
//        val vehicleEncPk = KeyUtils.getVehicleEncPk(filesDir)
//        val data = "Xin chao cac ban".toByteArray()
//
//        val encryptedData = EcDsa.encrypt(data, vehicleEncPk!!, MobileIdentity.cryptoBoxKeyPair.secretKey)
//        Log.d("test","encrypted:${encryptedData.toHexString()}")
//
//        val decryptData = EcDsa.decrypt(encryptedData,deviceEncPk!!,VehicleIdentity.cryptoBoxKeyPair.secretKey)
//        Log.d("test","decryptData:${String(decryptData)}")
//        val hexData =
//            "3746333444354233343132374142304443384646423235444343454434444438353937314145434230464346464642413130423433394631373834454336393544313944374445323334424439333632353638463443434233434543383935343431453336363244444642323030384445334239383835423039353032314130444337393335324233423141363146333641354442364146443931373243463644304246343639463843334237353730333146334231364146323332393546453537354139413246454335394643363634323441363132303835393131384639383633393538444539373231353636354131343346343331323833304643363836424335374435424245423645383737383844363742383044374546434230374541".hexStringToByteArray()
//        val s_pk = "6AD4B41E33741440ADB5093C541F7C7CE9D8B44E11195CEFEDD9F821BB66E91C"
//        val s_sk = "F626D3B70131B6867113361F063AC1EEAE594885CA2E5D97B88DE5CAED86B99E"
//        val pk = Key.fromHexString(s_pk)
//        val sk = Key.fromHexString(s_sk)
//        val sodium = EcDsa.lazySodium
//        val bobKp: KeyPair = sodium.cryptoBoxKeypair()
//
//        val message = "A super secret message xin chao cac ban".toByteArray()
//        val cipherText = ByteArray(message.size + Box.SEALBYTES)
//        sodium.cryptoBoxSeal(cipherText, message, message.size.toLong(), bobKp.publicKey.asBytes)
//
//        Log.d("test","cipherText ${cipherText.toHexString()}")
//        val messageLength = cipherText.size - Box.SEALBYTES
//        Log.d("test","message.size ${message.size} /messageLength: $messageLength")
//        val decrypted = ByteArray(message.size)
//
//        val res: Boolean = sodium.cryptoBoxSealOpen(
//            decrypted,
//            cipherText,
//            cipherText.size.toLong(),
//            bobKp.publicKey.asBytes,
//            bobKp.secretKey.asBytes
//        )
//        Log.d("test","decrypted ${decrypted.toHexString()}")
//        Log.d("test","decrypted ${String(decrypted)}")
//        Log.d("test","res $res")
//        val bouncyCastle = BouncyCastle.getInstance()
//        val keyPair = bouncyCastle.genECKeyPair()
//        val dataInput = "Xin chao cac ban".toByteArray() //as ECPrivateKeyParameters
//        val signatureData = bouncyCastle.sign(input =  dataInput , privateKey = keyPair.private as ECPrivateKeyParameters)
//        val verify = bouncyCastle.verify(originalData = dataInput, signature = signatureData, publicKey = keyPair.public as ECPublicKeyParameters)
//
    }

    fun syncKeyButtonOnClicked(view: View) {
        val intent = Intent(this, DeviceSyncKeyActivity::class.java)
        startActivity(intent)
    }
}