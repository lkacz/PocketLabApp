// Filename: ColorPickerHelper.kt
package com.lkacz.pola

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

object ColorPickerHelper {

    /**
     * Shows a dialog with three SeekBars for selecting R, G, B values of a color.
     * The [onColorSelected] callback returns the chosen color upon "OK".
     */
    fun showColorPickerDialog(
        context: Context,
        initialColor: Int,
        onColorSelected: (Int) -> Unit
    ) {
        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(context, 20), dpToPx(context, 20), dpToPx(context, 20), dpToPx(context, 20))
        }

        val previewText = TextView(context).apply {
            text = "Color Preview"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(initialColor)
            setPadding(dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16))
        }
        dialogLayout.addView(previewText)

        var red = (initialColor shr 16) and 0xFF
        var green = (initialColor shr 8) and 0xFF
        var blue = (initialColor) and 0xFF

        // Red SeekBar
        val redBar = makeColorSeekBar(context, "R", red) { newValue ->
            red = newValue
            previewText.setBackgroundColor(Color.rgb(red, green, blue))
        }
        dialogLayout.addView(redBar)

        // Green SeekBar
        val greenBar = makeColorSeekBar(context, "G", green) { newValue ->
            green = newValue
            previewText.setBackgroundColor(Color.rgb(red, green, blue))
        }
        dialogLayout.addView(greenBar)

        // Blue SeekBar
        val blueBar = makeColorSeekBar(context, "B", blue) { newValue ->
            blue = newValue
            previewText.setBackgroundColor(Color.rgb(red, green, blue))
        }
        dialogLayout.addView(blueBar)

        AlertDialog.Builder(context)
            .setTitle("Choose a color")
            .setView(dialogLayout)
            .setPositiveButton("OK") { _, _ ->
                onColorSelected(Color.rgb(red, green, blue))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Creates a labeled horizontal layout with a text label and a SeekBar for color channel.
     */
    private fun makeColorSeekBar(
        context: Context,
        label: String,
        initialValue: Int,
        onProgressChanged: (Int) -> Unit
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(context, 8), 0, dpToPx(context, 8))
        }

        val labelView = TextView(context).apply {
            text = "$label:"
            textSize = 14f
            setPadding(0, 0, dpToPx(context, 8), 0)
        }
        container.addView(labelView)

        val seekBar = SeekBar(context).apply {
            max = 255
            progress = initialValue
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    onProgressChanged(p)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        container.addView(seekBar)

        return container
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }
}
