package com.example.nfctag.emulator.vehicle

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nfctag.R
import com.example.nfctag.databinding.ActivityEmulatorCarBinding
import hi.baka3k.emulator.oem.vehicle.NFCEmulatorCarCallback
import hi.baka3k.emulator.oem.vehicle.VehicleEmulator

import hi.baka3k.nfctool.config.NfcReaderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class EmulatorCarActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmulatorCarBinding
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var nfcReaderCallback: NFCEmulatorCarCallback
    private val vehicleLogAdapter = VehicleLogAdapter()
    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmulatorCarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recycleView.layoutManager = LinearLayoutManager(this)
        binding.recycleView.adapter = vehicleLogAdapter
        initCardReader()
        observeCardReaderFlow()
        initNfc()
        binding.imageView.setOnClickListener {
            binding.vehicleLayout.visibility = View.GONE
            binding.logLayout.visibility = View.VISIBLE
        }
    }

    private fun initCardReader() {
        val nfcType = intent.getIntExtra("type", NfcReaderType.ISO_DEP)
//        binding.inforEmulation.text = "NFCReader Screen - ISO_DEP reader "
        val vehicleEmulator = VehicleEmulator(filesDir) { message ->
            Log.d("receiver", "Got message: $message")
            runOnUiThread {
                vehicleLogAdapter.addLog(message)
                binding.recycleView.scrollToPosition(vehicleLogAdapter.itemCount - 1)
                if ("CONTROL FLOW SUCCESS".equals(message)) {
                    openDoor()
                } else {
                    lockDoor()
                }
            }
        }
        nfcReaderCallback = NFCEmulatorCarCallback(vehicleEmulator)
    }

    private fun lockDoor() {
        binding.lock1.setImageResource(R.drawable.door_lock)
        binding.lock2.setImageResource(R.drawable.door_lock)
        binding.lock3.setImageResource(R.drawable.door_lock)
        binding.lock4.setImageResource(R.drawable.door_lock)
        binding.imageView.setImageResource(R.drawable.car_lock)
        binding.lock1.clearAnimation()
        binding.lock2.clearAnimation()
        binding.lock3.clearAnimation()
        binding.lock4.clearAnimation()
    }

    private fun openDoor() {
        binding.lock1.setImageResource(R.drawable.door_unlock)
        binding.lock2.setImageResource(R.drawable.door_unlock)
        binding.lock3.setImageResource(R.drawable.door_unlock)
        binding.lock4.setImageResource(R.drawable.door_unlock)
        binding.imageView.setImageResource(R.drawable.car_open)
        val animation = AnimationUtils.loadAnimation(applicationContext, R.anim.blink)
        binding.lock1.startAnimation(animation)
        binding.lock2.startAnimation(animation)
        binding.lock3.startAnimation(animation)
        binding.lock4.startAnimation(animation)
    }

    private fun observeCardReaderFlow() {
        coroutineScope.launch {
            nfcReaderCallback.nfcData.collect { nfcData ->
                Log.d(TAG, "#onDataReceived() $nfcData")
                val mess = nfcData.ifEmpty {
                    "NFCData is empty - waiting!!!"
                }
//                binding.inforEmulation.text = mess
            }
        }
    }

    private fun initNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    override fun onResume() {
        super.onResume()
        enableNFCReaderMode()
    }

    private fun enableNFCReaderMode() {
        if (nfcAdapter.isEnabled) {
            nfcAdapter.enableReaderMode(
                this, nfcReaderCallback, READER_FLAGS, null
            )
        } else {
            showNFCSettings()
        }
    }

    override fun onPause() {
        disableNFCReaderMode()
        super.onPause()
    }

    private fun disableNFCReaderMode() {
        nfcAdapter.disableReaderMode(this)
    }

    private fun showNFCSettings() {
        Toast.makeText(this, getString(R.string.warning_enable_nfc), Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startActivity(intent)
    }

    fun buttonClearLogClicked(view: View) {
        vehicleLogAdapter.clear()
        lockDoor()
    }

    companion object {
        private const val TAG = "NFCActivity"
        private const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_F
    }

    fun buttonShowViewClicked(view: View) {
        binding.vehicleLayout.visibility = View.VISIBLE
        binding.logLayout.visibility = View.GONE
    }

}