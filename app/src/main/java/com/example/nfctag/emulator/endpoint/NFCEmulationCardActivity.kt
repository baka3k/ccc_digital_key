package com.example.nfctag.emulator.endpoint

import android.os.Bundle
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.nfctag.R
import com.example.nfctag.base.BaseActivity


class NFCEmulationCardActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_emulation_card)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<PinCodeFragment>(R.id.fragment_container_view)
            }
        }
    }
}