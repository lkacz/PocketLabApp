package com.lkacz.pola

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment

/** Simple runtime feature flag toggle dialog (debug use). */
class FeatureFlagsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply { setTitle("Feature Flags") }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        FeatureFlags.load(requireContext())
        val ctx = requireContext()
        val root =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 32, 48, 32)
            }

        fun addFlag(
            label: String,
            get: () -> Boolean,
            set: (Boolean) -> Unit,
        ) {
            val cb =
                CheckBox(ctx).apply {
                    text = label
                    isChecked = get()
                    setOnCheckedChangeListener { _, isChecked ->
                        set(isChecked)
                    }
                }
            root.addView(cb)
        }

        addFlag("NEW_FEATURE_ONE", { FeatureFlags.newFeatureOne }, { FeatureFlags.newFeatureOne = it })
        addFlag("NEW_FEATURE_TWO", { FeatureFlags.newFeatureTwo }, { FeatureFlags.newFeatureTwo = it })

        val btnSave =
            Button(ctx).apply {
                text = "Save & Close"
                setOnClickListener {
                    FeatureFlags.persist(ctx)
                    dismiss()
                }
            }
        root.addView(btnSave)
        return root
    }
}
