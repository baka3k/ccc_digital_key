package com.example.nfctag.setting.transferkey.vehicle

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nfctag.R
import com.example.nfctag.databinding.FragmentTransferKeyBinding
import com.example.nfctag.emulator.endpoint.LogAdapter
import hi.baka3k.nfctool.nfcreader.NFCTagReader
import hi.baka3k.nfctool.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow

class NFCTransferKeyFragment : Fragment() {
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var nfcReaderCallback: NFCKeyTransferCallback
    private var _binding: FragmentTransferKeyBinding? = null
    private val binding get() = _binding!!
    private val logAdapter = LogAdapter()

    //    private uiHandler = Handler(<)
    private val uiHandler = Handler(Looper.getMainLooper())
    private fun initNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext())
        val keyEmulator = KeyEmulator(filesDir = requireContext().filesDir) { message ->
            Logger.d("TransferKeyFragment", "Got message: $message")
            uiHandler.post {
                logAdapter.addLog(message)
                binding.recycleView.scrollToPosition(logAdapter.itemCount - 1)
                if ("Transfer EncPk Success".equals(message) || "Transfer SigPk Success".equals(
                        message
                    )
                ) {
                    binding.status.setImageResource(R.drawable.door_unlock)
                } else {
                    binding.status.setImageResource(R.drawable.door_lock)
                }

            }
        }
        nfcReaderCallback = NFCKeyTransferCallback(keyEmulator)
    }

    override fun onResume() {
        super.onResume()
        enableNFCReaderMode()
    }

    private fun enableNFCReaderMode() {
        if (nfcAdapter.isEnabled) {
            nfcAdapter.enableReaderMode(
                requireActivity(), nfcReaderCallback, READER_FLAGS, null
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
        nfcAdapter.disableReaderMode(requireActivity())
    }

    private fun showNFCSettings() {
        Toast.makeText(requireContext(), getString(R.string.warning_enable_nfc), Toast.LENGTH_SHORT)
            .show()
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initNFC()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recycleView.adapter = logAdapter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_transfer_key, container, false)
        _binding = FragmentTransferKeyBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment NFCTransferKeyFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = NFCTransferKeyFragment()
        private const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_F
    }

}

class NFCKeyTransferCallback(private val keyEmulator: KeyEmulator) :
    NfcAdapter.ReaderCallback {
    private val _mutableStateNfcData: MutableStateFlow<String> = MutableStateFlow("")
    override fun onTagDiscovered(tag: Tag) {
        Logger.i(TAG, "New tag discovered")
        val nfcTagReader = createNFCReader(tag)
        if (nfcTagReader != null) {
            nfcTagReader.connect()
            if (nfcTagReader.isConnected()) {
                _mutableStateNfcData.value = "Start Standard Transaction "
                keyEmulator.start(nfcTagReader)
            } else {
                _mutableStateNfcData.value = "New Tag is not connect"
            }
        }
    }

    private fun createNFCReader(tag: Tag): NFCTagReader? {
        return try {
            NFCTagReader.create(tag)
        } catch (e: UnsupportedOperationException) {
            Logger.i(TAG, "#createNFCReader() error ${e.message}", e)
            null
        }
    }

    companion object {
        private const val TAG = "NFCEmulatorCarCallback"
    }
}