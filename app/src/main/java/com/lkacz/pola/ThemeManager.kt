package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class ThemeManager(private val context: Context) {
    companion object {
        private const val THEME_PREF = "ThemePref"
        private const val IS_DARK_THEME = "IsDarkTheme"
    }

    private val sharedPref: SharedPreferences = context.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE)

    fun toggleTheme() {
        val isDarkTheme = sharedPref.getBoolean(IS_DARK_THEME, false)
        sharedPref.edit().putBoolean(IS_DARK_THEME, !isDarkTheme).apply()
        applyTheme()
    }

    fun applyTheme() {
        val isDarkTheme = sharedPref.getBoolean(IS_DARK_THEME, false)
        if (isDarkTheme) {
            context.setTheme(R.style.AppTheme_Dark)
        } else {
            context.setTheme(R.style.AppTheme_Light)
        }
    }
}
