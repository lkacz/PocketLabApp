// Filename: CompletionFragment.kt
package com.lkacz.pola

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class CompletionFragment : Fragment() {
    private lateinit var sharedPref: android.content.SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        sharedPref = requireContext().getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)

        val rootLayout =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(32), dpToPx(32), dpToPx(32), dpToPx(32))
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                setBackgroundColor(ColorManager.getScreenBackgroundColor(requireContext()))
            }

        // Success icon/emoji
        val iconTextView =
            TextView(requireContext()).apply {
                text = "✓"
                textSize = 72f
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                gravity = Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin = dpToPx(24)
                    }
            }
        rootLayout.addView(iconTextView)

        // Title
        val titleTextView =
            TextView(requireContext()).apply {
                text = "Protocol Complete"
                textSize = 32f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ColorManager.getHeaderTextColor(requireContext()))
                gravity = Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin = dpToPx(16)
                    }
            }
        rootLayout.addView(titleTextView)

        // Message
        val messageTextView =
            TextView(requireContext()).apply {
                text = "Thank you for completing this session.\n\nYour responses have been saved."
                textSize = 18f
                setTextColor(ColorManager.getBodyTextColor(requireContext()))
                gravity = Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin = dpToPx(48)
                    }
            }
        rootLayout.addView(messageTextView)

        // Close button
        val closeButton =
            MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonStyle).apply {
                text = "Close App"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                setOnClickListener {
                    clearProtocolProgress()
                    requireActivity().finish()
                }
            }
        rootLayout.addView(closeButton)

        return rootLayout
    }

    private fun clearProtocolProgress() {
        sharedPref
            .edit()
            .remove(Prefs.KEY_PROTOCOL_PROGRESS_INDEX)
            .putBoolean(Prefs.KEY_PROTOCOL_IN_PROGRESS, false)
            .apply()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    companion object {
        @JvmStatic
        fun newInstance() = CompletionFragment()
    }
}
