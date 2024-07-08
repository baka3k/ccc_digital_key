package com.example.nfctag

import android.app.Application
import hi.baka3k.data.preference.Preference
import hi.baka3k.digitalkey.framework.DigitalKey

class EmulatorApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        DigitalKey.getInstance(filesDir)
        Preference.getInstance(applicationContext)
        Preference.getInstance(applicationContext).setUserActiveState(false)
    }

    override fun onTerminate() {
        Preference.getInstance(applicationContext).setByPassPinState(false)
        super.onTerminate()
    }
}