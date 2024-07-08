package com.example.nfctag.setting.transferkey.vehicle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nfctag.databinding.FragmentVehicleOEMTransferKeyBinding
import com.example.nfctag.emulator.endpoint.LogAdapter
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import hi.baka3k.digitalkey.utils.writePublicKey
import hi.baka3k.emulator.oem.vehicle.VehicleIdentity

import hi.baka3k.nfctool.utils.Logger
import hi.baka3k.nfctool.utils.hexStringToByteArray
import hi.baka3k.nfctool.utils.toHexString
import hi.baka3k.security.ec.ECKeyUtils
import hi.baka3k.security.ec.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import java.io.File


/**
 * A simple [Fragment] subclass.
 * Use the [VehicleOEMTransferKeyFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class VehicleOEMTransferKeyFragment : Fragment() {
    private val logAdapter = LogAdapter()
    private var _binding: FragmentVehicleOEMTransferKeyBinding? = null
    private val binding get() = _binding!!
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val valueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            coroutineScope.launch {
                Logger.d(TAG, "Value is: $dataSnapshot")
                val tag = VehicleIdentity.digitalKeyIdentifer
                val device_Enc_Pk = dataSnapshot.child(tag).child("device_Enc_Pk").value as? String
                val device_Sign_Pk = dataSnapshot.child(tag).child("device_Sign_Pk").value as? String
                Logger.d(TAG, "device_Enc_Pk is: $device_Enc_Pk")
                Logger.d(TAG, "device_En_Sk is: $device_Sign_Pk")
                if (device_Enc_Pk != null) {
                    saveDeviceEncPk(device_Enc_Pk.hexStringToByteArray())
                }
                if (device_Sign_Pk != null) {
                    saveDeviceSignPk(device_Sign_Pk.hexStringToByteArray())
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Sync Key Success", Toast.LENGTH_SHORT).show()
                    logAdapter.addLog("<<<device.Enc.pk: $device_Enc_Pk")
                    logAdapter.addLog("<<<device.Enc.sk: $device_Sign_Pk")
                }
            }

        }

        override fun onCancelled(error: DatabaseError) {
            Logger.w(TAG, "Failed to read value.", error.toException())
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(requireContext().applicationContext)
        initVehicleKey()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVehicleOEMTransferKeyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        databaseReference().addValueEventListener(valueEventListener)
        // Write a message to the database
        binding.recycleView.adapter = logAdapter
        binding.transferKeyButton.setOnClickListener {
            sendVehicleKeyToOEMServer()
        }
        sendVehicleKeyToOEMServer()
    }

    private fun sendVehicleKeyToOEMServer() {
        coroutineScope.launch {
            val myRef = databaseReference()
            val tag = VehicleIdentity.digitalKeyIdentifer
            val key = myRef.child(tag)
            key.child("vehicle_Enc_Pk").setValue(getVehicleEncPk().toByteArray().toHexString())
            key.child("vehicle_Sig_Pk").setValue(getVehicleSigSk().toByteArray().toHexString())
            withContext(Dispatchers.Main) {
                logAdapter.addLog(
                    ">>>vehi cle.Enc.Pk: ${
                        getVehicleEncPk().toByteArray().toHexString()
                    }"
                )
                logAdapter.addLog(
                    ">>>vehicle.Sig.Pk: ${
                        getVehicleSigSk().toByteArray().toHexString()
                    }"
                )
            }
        }
    }


    private fun databaseReference(): DatabaseReference {
        val database = Firebase.database
        return database.getReference("data")
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

    private fun initVehicleKey() {
//        KeyUtils.saveVehicleEncPk(
//            VehicleIdentity.cryptoBoxKeyPair.publicKey.asBytes,
//            requireContext().filesDir
//        )
//        KeyUtils.saveVehicleEncSk(
//            VehicleIdentity.cryptoBoxKeyPair.secretKey.asBytes,
//            requireContext().filesDir
//        )
    }

    override fun onDestroyView() {
        databaseReference().removeEventListener(valueEventListener)
        super.onDestroyView()
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }

    private fun saveDeviceSignPk(bytesValue: ByteArray) {
        val fileKey = File(requireContext().filesDir, ECKeyUtils.Devx_Sig_PK)
        fileKey.writePublicKey(bytesValue)
    }

    private fun saveDeviceEncPk(bytesValue: ByteArray) {
        val fileKey = File(requireContext().filesDir, ECKeyUtils.Devx_Enc_PK)
        fileKey.writePublicKey(bytesValue)
    }

    companion object {
        const val TAG = "VehicleOEMTransferKeyFragment"

        @JvmStatic
        fun newInstance() = VehicleOEMTransferKeyFragment()
    }
}