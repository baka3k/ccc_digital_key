package com.example.nfctag.setting.transferkey.device

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nfctag.databinding.ActivityDeviceSyncKeyBinding
import com.example.nfctag.emulator.endpoint.LogAdapter
import com.example.nfctag.setting.transferkey.vehicle.VehicleOEMTransferKeyFragment
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import hi.baka3k.digitalkey.framework.DigitalKey
import hi.baka3k.emulator.oem.vehicle.VehicleIdentity
import hi.baka3k.nfctool.utils.hexStringToByteArray
import hi.baka3k.nfctool.utils.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceSyncKeyActivity : AppCompatActivity() {
    private var _binding: ActivityDeviceSyncKeyBinding? = null
    private val binding get() = _binding!!
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logAdapter = LogAdapter()
    private val valueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            Log.d(VehicleOEMTransferKeyFragment.TAG, "Value is: $dataSnapshot")
            coroutineScope.launch {
                val tag = VehicleIdentity.digitalKeyIdentifer
                val vehicleEncPk = dataSnapshot.child(tag).child("vehicle_Enc_Pk").value as? String
                val vehicleSigPk = dataSnapshot.child(tag).child("vehicle_Sig_Pk").value as? String
                Log.d(VehicleOEMTransferKeyFragment.TAG, "vehicle_Sig_pk is: $vehicleSigPk")
                Log.d(VehicleOEMTransferKeyFragment.TAG, "vehicle_Enc_pk is: $vehicleEncPk")
                if (vehicleEncPk != null) {
                    DigitalKey.getInstance(filesDir)
                        .setVehicleEncPk(vehicleEncPk.hexStringToByteArray())
//                    KeyUtils.saveVehicleEncPk(vehicle_Enc_pk.hexStringToByteArray(), filesDir)
                }
                if (vehicleSigPk != null) {
                    DigitalKey.getInstance(filesDir)
                        .setVehicleSigPk(vehicleSigPk.hexStringToByteArray())
//                    KeyUtils.saveDeviceEncSk(vehicle_Enc_sk.hexStringToByteArray(), filesDir)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Sync Key Success", Toast.LENGTH_SHORT)
                        .show()
                    logAdapter.addLog("<<<vehicle.Enc.pk: $vehicleEncPk")
                    logAdapter.addLog("<<<vehicle.Sign.sk: $vehicleSigPk")
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.w(VehicleOEMTransferKeyFragment.TAG, "Failed to read value.", error.toException())
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(applicationContext)
        _binding = ActivityDeviceSyncKeyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        databaseReference().addValueEventListener(valueEventListener)
        binding.btnSynkey.setOnClickListener {
            coroutineScope.launch {
                databaseReference().removeEventListener(valueEventListener)
                databaseReference().addValueEventListener(valueEventListener)

                val myRef = databaseReference()
                val tag = VehicleIdentity.digitalKeyIdentifer
                val key = myRef.child(tag)
                val devicePubicKey = DigitalKey.getInstance(filesDir).devicePubicKeys[0]
                key.child("device_Enc_Pk").setValue(
                    devicePubicKey.deviceEncPk.q.getEncoded(false).toHexString()

                )
                key.child("device_Sign_Pk").setValue(
                    devicePubicKey.deviceSignPk.q.getEncoded(false).toHexString()
                )
            }
        }
        binding.recycleView.adapter = logAdapter
    }

    override fun onDestroy() {
        databaseReference().removeEventListener(valueEventListener)
        coroutineScope.cancel()
        super.onDestroy()
    }


    /** *******************************************************************************************
     * SUPPORT FUNCTION
     * *******************************************************************************************/

    private fun databaseReference(): DatabaseReference {
        val database = Firebase.database
        return database.getReference("data")
    }
}