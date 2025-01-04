// Filename: EndFragment.kt
package com.lkacz.pola

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView

class EndFragment : BaseTouchAwareFragment(5000, 20) {

    private lateinit var logger: Logger

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        logger = Logger.getInstance(requireContext())

        // Create a simple layout in code:
        val rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            gravity = Gravity.CENTER
        }

        val endMessageTextView = TextView(requireContext()).apply {
            text = "Completed"
            textSize = 24f
            gravity = Gravity.CENTER
        }
        rootLayout.addView(endMessageTextView)

        return rootLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Delay backup slightly after loading the view
        Handler(Looper.getMainLooper()).postDelayed({ logger.backupLogFile() }, 500)
    }

    /**
     * Overriding the method to close the activity once the threshold is reached.
     */
    override fun onTouchThresholdReached() {
        activity?.finish()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }
}
