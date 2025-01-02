// Filename: ColorPickerHelper.kt
package com.lkacz.pola

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.setPadding

object ColorPickerHelper {

    /**
     * Shows a dialog with three SeekBars for selecting R, G, B values of a color,
     * plus a 16x16 color grid that transitions from white -> full color -> black in each column.
     * The [onColorSelected] callback returns the chosen color upon "OK".
     */
    fun showColorPickerDialog(
        context: Context,
        initialColor: Int,
        onColorSelected: (Int) -> Unit
    ) {
        // Root container (vertical)
        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(context, 20))
        }

        // Text preview to show the selected color
        val previewText = TextView(context).apply {
            text = "Color Preview"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(initialColor)
            setPadding(dpToPx(context, 16))
        }
        dialogLayout.addView(previewText)

        // Internal tracking of color channels
        var red = Color.red(initialColor)
        var green = Color.green(initialColor)
        var blue = Color.blue(initialColor)

        // The 16x16 grid for quick color selection (now effectively 16x14 displayed)
        val colorGrid = createExtendedColorGrid(
            context = context,
            onColorSelected = { gridColor ->
                red = Color.red(gridColor)
                green = Color.green(gridColor)
                blue = Color.blue(gridColor)
                previewText.setBackgroundColor(gridColor)
            }
        )
        dialogLayout.addView(colorGrid)

        // Sliders for Red, Green, Blue
        val redBar = makeColorSeekBar(context, "R", red) { newValue ->
            red = newValue
            previewText.setBackgroundColor(Color.rgb(red, green, blue))
        }
        val greenBar = makeColorSeekBar(context, "G", green) { newValue ->
            green = newValue
            previewText.setBackgroundColor(Color.rgb(red, green, blue))
        }
        val blueBar = makeColorSeekBar(context, "B", blue) { newValue ->
            blue = newValue
            previewText.setBackgroundColor(Color.rgb(red, green, blue))
        }

        // Add them to the layout
        dialogLayout.addView(redBar)
        dialogLayout.addView(greenBar)
        dialogLayout.addView(blueBar)

        // Show the alert dialog
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
     * Creates a color grid that *skips the first and last row*. So you effectively
     * see rows 1..14 out of the 16 total, giving 14 rows and 16 columns.
     * Each column represents a hue (0-360) at full saturation.
     * The top portion blends toward white, the bottom portion blends toward black.
     */
    private fun createExtendedColorGrid(
        context: Context,
        onColorSelected: (Int) -> Unit
    ): GridLayout {
        // We declare 14 rows (instead of 16) because we skip row=0 and row=15
        val gridLayout = GridLayout(context).apply {
            rowCount = 14
            columnCount = 16
            setPadding(dpToPx(context, 8))
        }
        val cellSize = dpToPx(context, 20)

        // Skip row=0 and row=15
        for (row in 1 until 15) {
            for (col in 0 until 16) {
                // Hue from 0..360
                val hue = (col * (360f / 16f)) % 360f
                // The “middle color” is full hue, full saturation, full brightness
                val fullColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))

                // Decide if we are in top half (blend with white) or bottom half (blend with black)
                val colorInCell = when {
                    row < 8 -> {
                        // Top half: row=1 to 7 => fraction = row/7
                        val fraction = row / 7f
                        blendColors(Color.WHITE, fullColor, fraction)
                    }
                    else -> {
                        // Bottom half: row=8 to 14 => fraction=(row-8)/7
                        val fraction = (row - 8) / 7f
                        blendColors(fullColor, Color.BLACK, fraction)
                    }
                }

                val colorView = TextView(context).apply {
                    setBackgroundColor(colorInCell)
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = cellSize
                        height = cellSize
                    }
                    setOnClickListener {
                        onColorSelected(colorInCell)
                    }
                }
                gridLayout.addView(colorView)
            }
        }

        return gridLayout
    }

    /**
     * Blends two colors (c1, c2) in RGB space by [fraction].
     * fraction = 0.0 gives c1, fraction = 1.0 gives c2.
     */
    private fun blendColors(c1: Int, c2: Int, fraction: Float): Int {
        val r1 = Color.red(c1)
        val g1 = Color.green(c1)
        val b1 = Color.blue(c1)

        val r2 = Color.red(c2)
        val g2 = Color.green(c2)
        val b2 = Color.blue(c2)

        val rOut = (r1 + fraction * (r2 - r1)).toInt().coerceIn(0, 255)
        val gOut = (g1 + fraction * (g2 - g1)).toInt().coerceIn(0, 255)
        val bOut = (b1 + fraction * (b2 - b1)).toInt().coerceIn(0, 255)

        return Color.rgb(rOut, gOut, bOut)
    }

    /**
     * Creates a labeled horizontal layout with a text label and a SeekBar for a color channel.
     */
    private fun makeColorSeekBar(
        context: Context,
        label: String,
        initialValue: Int,
        onProgressChanged: (Int) -> Unit
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val verticalPadding = dpToPx(context, 8)
            setPadding(0, verticalPadding, 0, verticalPadding)
            gravity = Gravity.CENTER_VERTICAL
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
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * Overload for easier setting uniform padding if needed.
     */
    private fun LinearLayout.setPadding(pixels: Int) {
        setPadding(pixels, pixels, pixels, pixels)
    }
}
