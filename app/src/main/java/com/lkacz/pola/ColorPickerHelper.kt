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
     * plus an 11x9 color grid (since top & bottom rows are removed) transitioning
     * from white -> hue -> black.
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

        // The color grid omitting the top & bottom rows
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
     * Creates an 11-column grid of color swatches, but omits row=0 and row=10,
     * so only rows 1..9 are displayed (9 rows total).
     *
     * - row in [1..4] blends from white -> hue
     * - row = 5 is the hue at full brightness
     * - row in [6..9] blends from hue -> black (up to 80% black at row=9)
     */
    private fun createExtendedColorGrid(
        context: Context,
        onColorSelected: (Int) -> Unit
    ): GridLayout {
        // We display 9 rows visually (since we skip row=0 & row=10), with 11 columns
        val gridLayout = GridLayout(context).apply {
            rowCount = 9      // We'll fill in 9 rows
            columnCount = 11  // Keep 11 columns
            setPadding(dpToPx(context, 8))
        }
        val cellSize = dpToPx(context, 20)

        // Loop from row=1..9 (inclusive), skipping row=0 and row=10
        for (row in 1 until 10) {
            for (col in 0 until 11) {
                // Hue from 0..360 (depending on column)
                val hue = (col * (360f / 11f)) % 360f
                // The full hue (saturation=1, brightness=1)
                val fullColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))

                // Determine the blend:
                //   - rows 1..4 => blend from white to hue (fraction row/5)
                //   - row=5 => pure hue
                //   - rows 6..9 => blend from hue to black (fraction (row-5)/5)
                val colorInCell = when {
                    row < 5 -> {
                        val fraction = row / 5f
                        blendColors(Color.WHITE, fullColor, fraction)
                    }
                    row == 5 -> {
                        fullColor
                    }
                    else -> {
                        val fraction = (row - 5) / 5f
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
