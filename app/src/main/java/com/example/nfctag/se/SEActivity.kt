package com.example.nfctag.se

import android.os.Bundle
import android.se.omapi.Channel
import android.se.omapi.Reader
import android.se.omapi.SEService
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.nfctag.R
import hi.baka3k.data.sharedmodel.Config
import hi.baka3k.data.sharedmodel.SELECT
import hi.baka3k.nfctool.data.CommandApdu
import hi.baka3k.nfctool.utils.hexStringToByteArray
import hi.baka3k.nfctool.utils.toHexString
import java.util.concurrent.Executors

/**
 * Testing class - do not include in release version
 * */
class SEActivity : AppCompatActivity(), SEService.OnConnectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_se)
    }

    fun testTelephony(view: View) {
        try {
            packageManager.systemAvailableFeatures
            if (packageManager.hasSystemFeature(android.content.Context.TELEPHONY_SERVICE)) {
                val tm =
                    getSystemService(android.content.Context.TELEPHONY_SERVICE) as TelephonyManager
                val resp = tm.iccOpenLogicalChannel(
                    "01020304050607",  // AID
                    0 // p2
                )
                val ch = resp.channel
                if (ch > 0) {
                    val sResp = tm.iccTransmitApduLogicalChannel(
                        ch,
                        0, 1, 2, 3, 4, "HAHA"
                    )
                    tm.iccCloseLogicalChannel(ch)
                }
            } else {
                Log.d(TAG, "#testTelephony() have no TELEPHONY_SERVICE")
            }
        } catch (e: Exception) {
            Log.e(TAG, "#testTelephony() err:$e  !!!!", e)
        }

    }

    private val exe = Executors.newSingleThreadExecutor()
    private lateinit var se: SEService
    fun testSecureElement(view: View) {
        try {
            se = SEService(
                this,  // context
                exe,  // callbacks processor
                this // listener
            )
        } catch (e: Exception) {
//            Log.e(TAG, "#testSecureElement() err:$e  !!!!", e)
        }

    }

    override fun onConnected() {
        Log.d(TAG, "EService.OnConnectedListener() !!!!")
        if (se.isConnected) {
            val rdrs: Array<Reader> = se.readers
            Log.d(TAG, "EService.OnConnectedListener() rdrs size :${rdrs.size}")
            sendCommandSecureElement(rdrs)
        }
    }

    private fun sendCommandSecureElement(rdrs: Array<Reader>) {
        try {
            if (rdrs.isNotEmpty()) {
                rdrs.onEach {
                    if (it.isSecureElementPresent) {
                        val sess = it.openSession()

                        val ch: Channel? = sess.openLogicalChannel(
                            Config.DIGITAL_KEY_FRAMWORK_AID.hexStringToByteArray(), 0x00
                        )
                        if (ch == null) {
                            Log.d(
                                TAG,
                                "#sendCommandSecureElement() openLogicalChannel - FAIL - channel null"
                            )
                        } else {
                            val command = CommandApdu(
                                cla = SELECT.CLA,
                                ins = SELECT.INS,
                                p1 = 0x04,
                                p2 = 0x00,
                                data = Config.DIGITAL_KEY_FRAMWORK_AID.hexStringToByteArray(), // AID_DIGITAL_KEY
                                le = 0x00
                            )
                            val respApdu: ByteArray = ch.transmit(command.toBytes())

                            Log.d(
                                TAG,
                                "#sendCommandSecureElement() response - string :${
                                    String(
                                        respApdu
                                    )
                                }"
                            )
                            Log.d(
                                TAG,
                                "#sendCommandSecureElement() response - hex: ${respApdu.toHexString()}"
                            )
                            ch.close()
                        }
                    } else {
                        Log.d(TAG, "#sendCommandSecureElement() $it is not secure ElementPresent")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "#sendCommandSecureElement() err: $e", e)
        }
    }

    companion object {
        const val TAG = "HAHAHA"
        private val ISD_AID = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00)
    }
}
