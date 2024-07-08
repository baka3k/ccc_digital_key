package hi.baka3k.digitalkey.utils

import android.util.Log

object Logger {
    fun e(tag: String, errormess: String, exception: Throwable) {
        Log.e(tag, errormess, exception)
    }

    fun e(tag: String, errormess: String) {
        Log.e(tag, errormess)
    }

    fun e(errormess: String, exception: Throwable) {
        Log.e(TAG, errormess, exception)
    }

    fun e(errormess: String) {
        Log.e(TAG, errormess)
    }


    fun d(tag: String, content: String) {
        Log.d(tag, content)
    }

    fun d(content: String) {
        Log.d(TAG, content)
    }

    fun w(tag: String, s: String, toException: Throwable) {
        Log.w(TAG, s, toException)
    }
    fun w(tag: String, s: String) {
        Log.w(TAG, s)
    }
    fun i(tag: String, s: String, e: Throwable) {
        Log.i(TAG, s, e)
    }

    fun i(tag: String, s: String) {
        Log.i(TAG, s)
    }

    private const val TAG = "NFC Digital key emulator"
}