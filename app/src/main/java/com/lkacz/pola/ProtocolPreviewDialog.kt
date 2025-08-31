package com.lkacz.pola

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import java.io.BufferedReader

/**
 * Debug/feature-flagged dialog that shows the manipulated protocol text
 * using the same path as actual app usage (ProtocolManager + ProtocolTransformer).
 */
class ProtocolPreviewDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireContext()
        val pm = ProtocolManager(ctx)
        // Read currently selected or default asset protocol
        pm.readOriginalProtocol(null)
        val br: BufferedReader = pm.getManipulatedProtocol()
        val content = buildString {
            br.useLines { seq -> seq.forEach { appendLine(it) } }
        }.ifBlank { "<empty protocol>" }

        val scroll = ScrollView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val tv = TextView(ctx).apply {
            text = content
            setPadding(32, 32, 32, 32)
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        scroll.addView(tv)

        return AlertDialog.Builder(ctx)
            .setTitle("Protocol Preview")
            .setView(scroll)
            .setPositiveButton("Close", null)
            .create()
    }

    companion object {
        fun newInstance() = ProtocolPreviewDialog()
    }
}