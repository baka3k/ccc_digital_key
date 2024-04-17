package hi.baka3k.data.preference


import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences


class Preference(appContext: Context) {
    private val PREF_FILE_NAME = "encrypted_preferences_test"// only for test
    private val KEY = "secret_key_test" // only for test
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        PREF_FILE_NAME,
        KEY,
        appContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setByPassPinState(state: Boolean) {
        sharedPreferences.edit().putBoolean("pinstate", state).apply()
    }

    fun getByPassPinState(): Boolean {
        return sharedPreferences.getBoolean("pinstate", false)
    }

    fun getFastMode(): Boolean {
        return sharedPreferences.getBoolean("fashmode", false)
    }

    fun setFastMode(fashmode: Boolean) {
        sharedPreferences.edit().putBoolean("fashmode", fashmode).apply()
    }

    fun setUserActiveState(value: Boolean) {
        sharedPreferences.edit().putBoolean("useractive", value).apply()
    }

    fun getUserActiveState(): Boolean {
        return sharedPreferences.getBoolean("useractive", false)
    }

    companion object {
        private var instance: Preference? = null
        fun getInstance(appContext: Context): Preference {
            if (instance == null) {
                instance = Preference(appContext = appContext)
            }
            return instance!!
        }
    }
}