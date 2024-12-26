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

    /**
     * Applies a Material3 light or dark theme dynamically.
     * For completeness, ensure your styles.xml includes the corresponding
     * Theme.Material3.Light.* and Theme.Material3.Dark.* definitions.
     */
    fun applyTheme() {
        val isDarkTheme = sharedPref.getBoolean(IS_DARK_THEME, false)
        if (isDarkTheme) {
            // You could reference a custom dark Material3 theme if defined
            context.setTheme(R.style.Theme_Material3_Dark)
        } else {
            context.setTheme(R.style.Theme_Material3_Light)
        }
    }
}
