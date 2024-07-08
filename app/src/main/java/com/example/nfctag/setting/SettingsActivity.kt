package com.example.nfctag.setting

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.nfctag.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()

        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val enableFeaturePref = findPreference<SwitchPreferenceCompat>("fastaccess")
            val config = hi.baka3k.data.preference.Preference.getInstance(requireContext())
            val fashmode: Boolean = config.getFastMode()
            enableFeaturePref?.setDefaultValue(fashmode)
            enableFeaturePref?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    val isFeatureEnabled = newValue as Boolean
                    Log.d("Test", "isFeatureEnabled $isFeatureEnabled")
                    config.setFastMode(isFeatureEnabled)
                    true
                }
        }

    }
}