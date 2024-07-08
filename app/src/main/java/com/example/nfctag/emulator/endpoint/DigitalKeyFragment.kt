package com.example.nfctag.emulator.endpoint

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nfctag.R
import com.example.nfctag.databinding.FragmentDigitalKeyBinding
import hi.baka3k.data.preference.Preference
import hi.baka3k.digitalkey.protocol.nfc.NfcHostApduService
import org.xmlpull.v1.XmlPullParser


class DigitalKeyFragment : Fragment() {
    private lateinit var cardEmulation: CardEmulation
    private var _binding: FragmentDigitalKeyBinding? = null
    private val binding get() = _binding!!
    private val logAdapter = LogAdapter()

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("data") ?: "empty data"
            Log.d("receiver", "Got message: $message")
            logAdapter.addLog(message)
            binding.recycleView.scrollToPosition(logAdapter.itemCount - 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cardEmulation = CardEmulation.getInstance(NfcAdapter.getDefaultAdapter(requireContext()))
        checkForceDefaultServiceAid()
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mMessageReceiver, IntentFilter("nfc-event"))
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mMessageReceiver)
        super.onDestroy()
    }

    private var currentState = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkNFCCardEmulation()
        binding.recycleView.layoutManager = LinearLayoutManager(context)
        binding.recycleView.adapter = logAdapter
        currentState = R.drawable.bg_btn_lock


        binding.btnDoor.setImageResource(R.drawable.bg_btn_lock)
        binding.buttonShowUILayout.setOnClickListener {
            binding.logLayout.visibility = View.GONE
            binding.keyLayout.visibility = View.VISIBLE
        }
        binding.keyView.setOnClickListener {
            binding.logLayout.visibility = View.VISIBLE
            binding.keyLayout.visibility = View.GONE
        }
        val buttonClickAnimation = AlphaAnimation(1f, 0.7f)
        binding.btnDoor.setOnClickListener {
            it.startAnimation(buttonClickAnimation)
            if (currentState == R.drawable.bg_btg_unlock) {
                Preference.getInstance(requireContext().applicationContext)
                    .setUserActiveState(false)
                currentState = R.drawable.bg_btn_lock
            } else {
                Preference.getInstance(requireContext().applicationContext).setUserActiveState(true)
                currentState = R.drawable.bg_btg_unlock
            }
            binding.btnDoor.setImageResource(currentState)
        }


        binding.switchCompat.setOnCheckedChangeListener { buttonView, isChecked ->
            Log.d("test","isChecked $isChecked")
            Preference.getInstance(requireContext().applicationContext).setFastMode(isChecked)
            if (isChecked) {
                binding.switchCompat.text = getString(R.string.do_not_need_pin_to_access)
            } else {
                binding.switchCompat.text = getString(R.string.always_ask_pin_to_access)
            }
        }
        val pinState = Preference.getInstance(requireContext().applicationContext).getByPassPinState()
        binding.switchCompat.isChecked = pinState
        binding.clearLogButton.setOnClickListener {
            logAdapter.clearData()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDigitalKeyBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onResume() {
        setPreferServiceAid()
        super.onResume()
    }

    /**
     * SUPPORT FUNCTION
     * */
    private fun checkForceDefaultServiceAid() {
        val aid = parseAid(resources.getXml(R.xml.device_aptu_service))
        val activity = requireActivity()
        if (!cardEmulation.isDefaultServiceForAid(
                ComponentName(
                    activity,
                    NfcHostApduService::class.java
                ), aid
            )
        ) {
            Log.d(
                "NFCEmulationCardActivity",
                "This application is NOT the preferred service for aid $aid"
            )

        } else {
            Log.d(
                "NFCEmulationCardActivity",
                "This application is the preferred service for aid  $aid"
            )
        }
    }

    private fun checkNFCCardEmulation() {
        if (activity != null) {
            val pm = requireActivity().packageManager
            if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
                Log.i("MainActivity", "Missing HCE functionality.")
            } else {
//                inforEmulation.text = "GOOD! NFC Emulator Card is running!!!"
//                inforEmulation.setBackgroundResource(R.color.green)
            }
        }
    }

    private fun setPreferServiceAid() {
        val activity = requireActivity()
        cardEmulation.setPreferredService(
            activity, ComponentName(
                activity,
                NfcHostApduService::class.java
            )
        )
    }

    private fun parseAid(xmlResource: XmlResourceParser): String {
        try {
            xmlResource.use { parser ->
                var eventType: Int
                do {
                    eventType = parser.next()
                    if (eventType == XmlPullParser.START_TAG) {
                        if (parser.name == "aid-filter") {
                            for (i in 0 until parser.attributeCount) {
                                if (parser.getAttributeName(i) == "name") {
                                    return parser.getAttributeValue(i)
                                }
                            }
                        }
                    }
                } while (eventType != XmlPullParser.END_DOCUMENT)
                throw IllegalArgumentException("No aid-filter found")
            }
        } catch (e: Exception) {
            throw IllegalArgumentException(e)
        }
    }
}