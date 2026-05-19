package com.smarter.video

import android.content.Context

object PasswordManager {

    private const val PREF = "secure_pref"
    private const val KEY = "verified"

    fun setVerified(context: Context, verified: Boolean) {

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY, verified)
            .apply()
    }

    fun isVerified(context: Context): Boolean {

        return context.getSharedPreferences(
            PREF,
            Context.MODE_PRIVATE
        ).getBoolean(KEY, false)
    }

    fun clear(context: Context) {
        setVerified(context, false)
    }
}
