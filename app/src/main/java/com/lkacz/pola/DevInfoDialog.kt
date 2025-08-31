package com.lkacz.pola

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class DevInfoDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val info = buildString {
            appendLine("App Version: ${BuildConfig.APP_VERSION} (${BuildConfig.APP_VERSION_CODE})")
            appendLine("SDK Int: ${Build.VERSION.SDK_INT}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("FeatureFlags.NEW_FEATURE_ONE=${FeatureFlags.NEW_FEATURE_ONE}")
            appendLine("FeatureFlags.NEW_FEATURE_TWO=${FeatureFlags.NEW_FEATURE_TWO}")
        }
        return AlertDialog.Builder(requireContext())
            .setTitle("Developer Info")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .create()
    }
}