// Filename: ColorPickerUtils.kt
package com.lkacz.pola

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Utility methods for handling color picker dialogs and preview box updates.
 */
object ColorPickerUtils {

    /**
     * Applies a background color to the given [picker] View (which is a color preview box).
     */
    fun applyColorPickerBoxColor(context: Context, picker: View, color: Int) {
        val drawable = ContextCompat.getDrawable(context, R.drawable.black_outline)
        if (drawable is GradientDrawable) {
            drawable.setColor(color)
            picker.background = drawable
        }
    }

    /**
     * Opens a color picker dialog with initial [initialColor].
     * The selected color is returned via [onColorSelected].
     */
    fun openColorPicker(
        fragment: Fragment,
        initialColor: Int,
        onColorSelected: (Int) -> Unit
    ) {
        val context = fragment.requireContext()
        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleText = TextView(context).apply {
            text = "Pick a Color"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            textSize = 16f
        }
        dialogLayout.addView(titleText)

        val redRow = newColorRow(context, "R:")
        val seekRed = SeekBar(context).apply { max = 255 }
        redRow.addView(seekRed)
        dialogLayout.addView(redRow)

        val greenRow = newColorRow(context, "G:")
        val seekGreen = SeekBar(context).apply { max = 255 }
        greenRow.addView(seekGreen)
        dialogLayout.addView(greenRow)

        val blueRow = newColorRow(context, "B:")
        val seekBlue = SeekBar(context).apply { max = 255 }
        blueRow.addView(seekBlue)
        dialogLayout.addView(blueRow)

        val colorPreview = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 80)
            ).apply { topMargin = dpToPx(context, 12) }
        }
        dialogLayout.addView(colorPreview)

        val initR = Color.red(initialColor)
        val initG = Color.green(initialColor)
        val initB = Color.blue(initialColor)
        seekRed.progress = initR
        seekGreen.progress = initG
        seekBlue.progress = initB
        colorPreview.setBackgroundColor(Color.rgb(initR, initG, initB))

        val updatePreview = {
            val newC = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
            colorPreview.setBackgroundColor(newC)
        }
        seekRed.setOnSeekBarChangeListener(simpleSeekBarListener { updatePreview() })
        seekGreen.setOnSeekBarChangeListener(simpleSeekBarListener { updatePreview() })
        seekBlue.setOnSeekBarChangeListener(simpleSeekBarListener { updatePreview() })

        AlertDialog.Builder(context)
            .setView(dialogLayout)
            .setPositiveButton("OK") { _, _ ->
                val chosenColor = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
                onColorSelected(chosenColor)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Utility method to create a small row containing a label and a SeekBar.
     * The SeekBar uses a weighted layout param so it expands to fill remaining space.
     */
    private fun newColorRow(context: Context, labelText: String): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val marginTop = dpToPx(context, 8)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = marginTop }
        }
        val label = TextView(context).apply {
            text = labelText
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        row.addView(label)

        // Make the SeekBar fill the rest of the row’s width.
        // We'll insert it at runtime in openColorPicker(...).
        return row
    }

    /**
     * A simplified SeekBar listener that only cares about onProgressChanged.
     */
    private fun simpleSeekBarListener(
        onValueChanged: (Int) -> Unit
    ) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            onValueChanged(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }
}
