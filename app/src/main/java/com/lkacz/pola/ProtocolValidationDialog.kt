package com.lkacz.pola

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class ProtocolValidationDialog : DialogFragment() {

    private val recognizedCommands = setOf(
        "BRANCH_SCALE",
        "INSTRUCTION",
        "TIMER",
        "TAP_INSTRUCTION",
        "SCALE",
        "MULTISCALE",
        "RANDOMIZED_MULTISCALE",
        "BRANCH_SCALE",
        "INPUTFIELD",
        "CUSTOM_HTML",
        "LABEL",
        "GOTO",
        "TRANSITIONS",
        "TIMER_SOUND",
        "HEADER_SIZE",
        "BODY_SIZE",
        "BUTTON_SIZE",
        "ITEM_SIZE",
        "RESPONSE_SIZE",
        "LOG",
        "END"
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ScrollView(requireContext()).apply {
            val textView = TextView(requireContext()).apply {
                textSize = 16f
                val content = getProtocolContent()
                text = highlightProtocolContent(content)
                setPadding(32, 32, 32, 32)
            }
            addView(textView)
        }
    }

    private fun highlightProtocolContent(fileContent: String): Spanned {
        val lines = fileContent.split("\n")
        val builder = SpannableStringBuilder()

        for (i in lines.indices) {
            val line = lines[i].trimEnd()
            val startIndex = builder.length

            if (line.startsWith("//")) {
                builder.append(line)
                builder.setSpan(
                    ForegroundColorSpan(Color.GRAY),
                    startIndex,
                    startIndex + line.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else if (line.isBlank()) {
                builder.append(line)
            } else {
                val semicolonIndex = line.indexOf(";")
                val firstSegment = if (semicolonIndex >= 0) {
                    line.substring(0, semicolonIndex).uppercase()
                } else {
                    line.uppercase()
                }

                if (recognizedCommands.contains(firstSegment)) {
                    builder.append(line)
                    builder.setSpan(
                        ForegroundColorSpan(Color.GREEN),
                        startIndex,
                        startIndex + firstSegment.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.setSpan(
                        StyleSpan(Typeface.BOLD),
                        startIndex,
                        startIndex + firstSegment.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    builder.append(line)
                    builder.setSpan(
                        ForegroundColorSpan(Color.RED),
                        startIndex,
                        startIndex + line.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.setSpan(
                        StyleSpan(Typeface.BOLD),
                        startIndex,
                        startIndex + line.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            if (i < lines.size - 1) builder.append("\n")
        }
        return builder
    }

    private fun getProtocolContent(): String {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", 0)
        val mode = prefs.getString("CURRENT_MODE", null)
        val customUriString = prefs.getString("PROTOCOL_URI", null)

        if (!customUriString.isNullOrEmpty()) {
            val uri = android.net.Uri.parse(customUriString)
            return ProtocolReader().readFileContent(requireContext(), uri)
        }

        return if (mode == "tutorial") {
            ProtocolReader().readFromAssets(requireContext(), "tutorial_protocol.txt")
        } else {
            ProtocolReader().readFromAssets(requireContext(), "demo_protocol.txt")
        }
    }
}
