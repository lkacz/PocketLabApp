// Filename: ColorPickerHelper.kt
package com.lkacz.pola

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.setPadding

object ColorPickerHelper {

    /**
     * Shows a dialog with three SeekBars for selecting R, G, B values of a color,
     * plus an extended grid that omits the top & bottom row for the hue-based columns,
     * and adds one extra column (col=11) transitioning from white (top) to black (bottom).
     * When a grid cell is tapped, the preview and the sliders are updated accordingly.
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

        // A simple View to preview the selected color, with a black stroke.
        // Note: We keep a reference to the GradientDrawable so we can update its fill color more directly.
        val outlineDrawable = GradientDrawable().apply {
            setColor(initialColor)
            setStroke(dpToPx(context, 2), Color.BLACK)
            cornerRadius = dpToPx(context, 4).toFloat()
        }
        val previewView = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 40)
            )
            background = outlineDrawable
        }
        dialogLayout.addView(previewView)

        // Internal tracking of color channels
        var red = Color.red(initialColor)
        var green = Color.green(initialColor)
        var blue = Color.blue(initialColor)

        // We define references for the SeekBars and EditTexts so we can update them later
        lateinit var redBar: SeekBar
        lateinit var greenBar: SeekBar
        lateinit var blueBar: SeekBar
        lateinit var redInput: EditText
        lateinit var greenInput: EditText
        lateinit var blueInput: EditText

        // Function to update the preview color
        fun updatePreview() {
            outlineDrawable.setColor(Color.rgb(red, green, blue))
        }

        // The color grid omitting the top & bottom rows, plus an extra grayscale column
        // We'll pass a callback that updates red/green/blue and also updates the SeekBars and EditTexts
        val colorGrid = createExtendedColorGrid(
            context = context,
            onColorSelected = { gridColor ->
                red = Color.red(gridColor)
                green = Color.green(gridColor)
                blue = Color.blue(gridColor)

                // Refresh preview
                updatePreview()

                // Update the SeekBars with the new RGB values
                redBar.progress = red
                greenBar.progress = green
                blueBar.progress = blue

                // Update the numeric fields as well
                redInput.setText(red.toString())
                greenInput.setText(green.toString())
                blueInput.setText(blue.toString())
            }
        )
        dialogLayout.addView(colorGrid)

        // Helper to build each color row: label, seekBar, numeric box
        fun makeColorSeekBar(
            label: String,
            initialValue: Int,
            onProgressChanged: (Int) -> Unit
        ): Triple<LinearLayout, SeekBar, EditText> {

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
            }
            container.addView(seekBar)

            // Numeric input box so user can directly type values
            val valueInput = EditText(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(context, 56),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.setMargins(dpToPx(context, 8), 0, 0, 0)
                }
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(initialValue.toString())
                gravity = Gravity.CENTER
                textSize = 14f
            }
            container.addView(valueInput)

            // Listeners: keep slider & text in sync
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) {
                        // Update the text field only if user changed slider
                        valueInput.setText(p.toString())
                    }
                    onProgressChanged(p)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            // When user edits the numeric value, update the slider and color preview
            valueInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val parsedValue = s?.toString()?.toIntOrNull()

                    // Graceful handling if user clears the box or enters invalid text
                    if (parsedValue == null) {
                        // Optionally set to 0 or revert to current slider value
                        // For clarity, let's revert to the current slider value:
                        if (valueInput.text.toString() != seekBar.progress.toString()) {
                            valueInput.setText(seekBar.progress.toString())
                        }
                        return
                    }

                    val clampedValue = parsedValue.coerceIn(0, 255)
                    if (clampedValue != seekBar.progress) {
                        seekBar.progress = clampedValue
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            return Triple(container, seekBar, valueInput)
        }

        // Create the container + SeekBar for each color
        val (redLayout, rSeekBar, rInput) = makeColorSeekBar("R", red) { newValue ->
            red = newValue
            updatePreview()
        }
        dialogLayout.addView(redLayout)
        redBar = rSeekBar
        redInput = rInput

        val (greenLayout, gSeekBar, gInput) = makeColorSeekBar("G", green) { newValue ->
            green = newValue
            updatePreview()
        }
        dialogLayout.addView(greenLayout)
        greenBar = gSeekBar
        greenInput = gInput

        val (blueLayout, bSeekBar, bInput) = makeColorSeekBar("B", blue) { newValue ->
            blue = newValue
            updatePreview()
        }
        dialogLayout.addView(blueLayout)
        blueBar = bSeekBar
        blueInput = bInput

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
     * Creates a grid with:
     *  - rows=9 visible (skipping row=0 and row=10),
     *  - columns=12 total, where columns 0..10 show hue transitions,
     *    and column=11 is a pure white->black gradient.
     *
     * For the hue columns:
     *  - row in [1..4] blends from white -> hue
     *  - row=5 is the hue at full brightness
     *  - row in [6..9] blends from hue -> black
     *
     * The last column (col=11) is a grayscale gradient from white at row=1 to black at row=9.
     */
    private fun createExtendedColorGrid(
        context: Context,
        onColorSelected: (Int) -> Unit
    ): GridLayout {
        val gridLayout = GridLayout(context).apply {
            // We display 9 rows visually (rows=1..9)
            rowCount = 9
            // We now have 12 columns: 11 hue columns + 1 grayscale column
            columnCount = 12
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(context, 8))
        }

        // Fixed cell height for each row
        val cellHeight = dpToPx(context, 28)

        // Loop from row=1..9 (inclusive), skipping row=0 and row=10
        for (row in 1..9) {
            for (col in 0 until 12) {
                val colorInCell = if (col < 11) {
                    // Hue-based columns 0..10
                    val hue = (col * (360f / 11f)) % 360f
                    // The full hue (saturation=1, brightness=1)
                    val fullColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                    when {
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
                } else {
                    // col == 11 => grayscale gradient from white (row=1) to black (row=9)
                    val fraction = (row - 1) / 8f
                    blendColors(Color.WHITE, Color.BLACK, fraction)
                }

                val colorView = TextView(context).apply {
                    setBackgroundColor(colorInCell)
                    layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(row - 1),
                        GridLayout.spec(col, 1f) // Weighted column
                    ).apply {
                        width = 0    // let weight handle the width distribution
                        height = cellHeight
                    }
                    // On click, notify that this color was chosen
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
