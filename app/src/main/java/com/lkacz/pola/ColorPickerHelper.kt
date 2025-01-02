// Filename: ColorPickerHelper.kt
package com.lkacz.pola

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red

/**
 * A helper class to show a color picker dialog (with three sliders for R, G, B).
 * Factored out from AppearanceCustomizationDialog for better modularity.
 */
object ColorPickerHelper {

    /**
     * Shows a color picker dialog that allows users to adjust the R, G, B components.
     * Calls [onColorSelected] once the user confirms the color choice.
     */
    fun showColorPickerDialog(
        context: Context,
        initialColor: Int,
        onColorSelected: (Int) -> Unit
    ) {
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
            setTypeface(typeface, Typeface.BOLD)
            textSize = 16f
        }
        dialogLayout.addView(titleText)

        // R row
        val redRow = createColorRow(context, "R:")
        val seekRed = SeekBar(context).apply { max = 255 }
        redRow.addView(seekRed)
        dialogLayout.addView(redRow)

        // G row
        val greenRow = createColorRow(context, "G:")
        val seekGreen = SeekBar(context).apply { max = 255 }
        greenRow.addView(seekGreen)
        dialogLayout.addView(greenRow)

        // B row
        val blueRow = createColorRow(context, "B:")
        val seekBlue = SeekBar(context).apply { max = 255 }
        blueRow.addView(seekBlue)
        dialogLayout.addView(blueRow)

        // Color preview
        val colorPreview = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 80)
            ).apply { topMargin = dpToPx(context, 12) }
        }
        dialogLayout.addView(colorPreview)

        // Initialize
        val initR = initialColor.red
        val initG = initialColor.green
        val initB = initialColor.blue
        seekRed.progress = initR
        seekGreen.progress = initG
        seekBlue.progress = initB
        colorPreview.setBackgroundColor(Color.rgb(initR, initG, initB))

        val updatePreview = {
            val newC = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
            colorPreview.setBackgroundColor(newC)
        }

        // Listeners for preview
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
     * Creates a simple row (horizontal layout) for the color label + SeekBar.
     */
    private fun createColorRow(context: Context, labelText: String): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(context, 8) }
            val label = TextView(context).apply {
                text = labelText
                setTypeface(typeface, Typeface.BOLD)
            }
            addView(label)
        }
    }

    /**
     * A simple utility for SeekBar change listener that only cares about onProgressChanged.
     */
    private fun simpleSeekBarListener(onValueChanged: (Int) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onValueChanged(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

    private fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
