package com.lkacz.pola

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class EndFragment : BaseTouchAwareFragment(5000, 20) {

    private lateinit var logger: Logger

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        logger = Logger.getInstance(requireContext())
        return inflater.inflate(R.layout.fragment_end, container, false)
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
}