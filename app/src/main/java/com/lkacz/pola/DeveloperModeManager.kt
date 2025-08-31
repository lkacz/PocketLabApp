package com.lkacz.pola

import android.content.Context

object DeveloperModeManager {
    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE).getBoolean(Prefs.KEY_DEVELOPER_MODE, false)

    fun enable(context: Context) {
        context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE).edit().putBoolean(Prefs.KEY_DEVELOPER_MODE, true).apply()
    }

    fun disable(context: Context) {
        context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE).edit().putBoolean(Prefs.KEY_DEVELOPER_MODE, false).apply()
    }
}
