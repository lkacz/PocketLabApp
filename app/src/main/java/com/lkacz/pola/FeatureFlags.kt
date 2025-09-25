package com.lkacz.pola

object FeatureFlags {
    // Backing fields (default values act as fallbacks if prefs not yet written)
    private const val PREFS_NAME = "ProtocolPrefs"
    private const val KEY_FF_ONE = "ff_new_feature_one"
    private const val KEY_FF_TWO = "ff_new_feature_two"

    var newFeatureOne: Boolean = true
    var newFeatureTwo: Boolean = true

    fun load(context: android.content.Context) {
        val p = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        if (p.contains(KEY_FF_ONE)) newFeatureOne = p.getBoolean(KEY_FF_ONE, newFeatureOne)
        if (p.contains(KEY_FF_TWO)) newFeatureTwo = p.getBoolean(KEY_FF_TWO, newFeatureTwo)
    }

    fun persist(context: android.content.Context) {
        val p = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        p.edit()
            .putBoolean(KEY_FF_ONE, newFeatureOne)
            .putBoolean(KEY_FF_TWO, newFeatureTwo)
            .apply()
    }
}
