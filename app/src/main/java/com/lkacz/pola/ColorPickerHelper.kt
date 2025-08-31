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
    fun showColorPickerDialog(
        context: Context,
        initialColor: Int,
        onColorSelected: (Int) -> Unit,
    ) {
        val dialogLayout =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(context, 20))
            }

        val outlineDrawable =
            GradientDrawable().apply {
                setColor(initialColor)
                setStroke(dpToPx(context, 2), Color.BLACK)
                cornerRadius = dpToPx(context, 4).toFloat()
            }
        val previewView =
            View(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(context, 40),
                    )
                background = outlineDrawable
            }
        dialogLayout.addView(previewView)

        var red = Color.red(initialColor)
        var green = Color.green(initialColor)
        var blue = Color.blue(initialColor)

        lateinit var redBar: SeekBar
        lateinit var greenBar: SeekBar
        lateinit var blueBar: SeekBar
        lateinit var redInput: EditText
        lateinit var greenInput: EditText
        lateinit var blueInput: EditText
        lateinit var hexInput: EditText

        fun updatePreview() {
            outlineDrawable.setColor(Color.rgb(red, green, blue))
        }

        val colorGrid =
            createExtendedColorGrid(
                context = context,
                onColorSelected = { gridColor ->
                    red = Color.red(gridColor)
                    green = Color.green(gridColor)
                    blue = Color.blue(gridColor)
                    updatePreview()

                    redBar.progress = red
                    greenBar.progress = green
                    blueBar.progress = blue

                    redInput.setText(red.toString())
                    greenInput.setText(green.toString())
                    blueInput.setText(blue.toString())
                    hexInput.setText(colorToHexString(gridColor))
                },
            )
        dialogLayout.addView(colorGrid)

        fun makeColorSeekBar(
            label: String,
            initialValue: Int,
            onProgressChanged: (Int) -> Unit,
        ): Triple<LinearLayout, SeekBar, EditText> {
            val container =
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val verticalPadding = dpToPx(context, 8)
                    setPadding(0, verticalPadding, 0, verticalPadding)
                    gravity = Gravity.CENTER_VERTICAL
                }

            val labelView =
                TextView(context).apply {
                    text = "$label:"
                    textSize = 14f
                    setPadding(0, 0, dpToPx(context, 8), 0)
                }
            container.addView(labelView)

            val seekBar =
                SeekBar(context).apply {
                    max = 255
                    progress = initialValue
                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f,
                        )
                }
            container.addView(seekBar)

            val valueInput =
                EditText(context).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                            dpToPx(context, 56),
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).also {
                            it.setMargins(dpToPx(context, 8), 0, 0, 0)
                        }
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(initialValue.toString())
                    gravity = Gravity.CENTER
                    textSize = 14f
                }
            container.addView(valueInput)

            // Select all text on focus
            valueInput.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    valueInput.post { valueInput.selectAll() }
                }
            }

            seekBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        sb: SeekBar?,
                        p: Int,
                        fromUser: Boolean,
                    ) {
                        if (fromUser) {
                            valueInput.setText(p.toString())
                        }
                        onProgressChanged(p)
                    }

                    override fun onStartTrackingTouch(sb: SeekBar?) {}

                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                },
            )

            valueInput.addTextChangedListener(
                object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        // Only run logic if user is editing this field
                        if (valueInput.isFocused) {
                            val parsedValue = s?.toString()?.toIntOrNull()
                            if (parsedValue == null) {
                                if (valueInput.text.toString() != seekBar.progress.toString()) {
                                    valueInput.setText(seekBar.progress.toString())
                                }
                                return
                            }
                            val clampedValue = parsedValue.coerceIn(0, 255)
                            if (clampedValue != seekBar.progress) {
                                seekBar.progress = clampedValue
                            }
                            // Update displayed text if user typed a value > 255 or < 0
                            if (parsedValue != clampedValue) {
                                valueInput.setText(clampedValue.toString())
                                valueInput.setSelection(valueInput.text.length)
                            }
                            hexInput.setText(colorToHexString(Color.rgb(red, green, blue)))
                        }
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) {}

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) {}
                },
            )

            return Triple(container, seekBar, valueInput)
        }

        val (redLayout, rSeekBar, rInput) =
            makeColorSeekBar("R", red) { newValue ->
                red = newValue
                updatePreview()
            }
        dialogLayout.addView(redLayout)
        redBar = rSeekBar
        redInput = rInput

        val (greenLayout, gSeekBar, gInput) =
            makeColorSeekBar("G", green) { newValue ->
                green = newValue
                updatePreview()
            }
        dialogLayout.addView(greenLayout)
        greenBar = gSeekBar
        greenInput = gInput

        val (blueLayout, bSeekBar, bInput) =
            makeColorSeekBar("B", blue) { newValue ->
                blue = newValue
                updatePreview()
            }
        dialogLayout.addView(blueLayout)
        blueBar = bSeekBar
        blueInput = bInput

        val hexLayout =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                val verticalPadding = dpToPx(context, 8)
                setPadding(0, verticalPadding, 0, verticalPadding)
                gravity = Gravity.CENTER_VERTICAL
            }
        val hexLabel =
            TextView(context).apply {
                text = "HEX:"
                textSize = 14f
                setPadding(0, 0, dpToPx(context, 8), 0)
            }
        hexLayout.addView(hexLabel)

        var lastValidHex = colorToHexString(Color.rgb(red, green, blue))
        hexInput =
            EditText(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        dpToPx(context, 100),
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).also {
                        it.setMargins(dpToPx(context, 8), 0, 0, 0)
                    }
                inputType = InputType.TYPE_CLASS_TEXT
                gravity = Gravity.CENTER
                textSize = 14f
                setText(lastValidHex)
            }
        // Select all text on focus
        hexInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hexInput.post { hexInput.selectAll() }
            } else {
                val hexString = hexInput.text.toString().trim().uppercase()
                val normalized = if (!hexString.startsWith("#")) "#$hexString" else hexString
                if (normalized.length == 7) {
                    try {
                        val colorValue = Color.parseColor(normalized)
                        red = Color.red(colorValue)
                        green = Color.green(colorValue)
                        blue = Color.blue(colorValue)
                        redBar.progress = red
                        greenBar.progress = green
                        blueBar.progress = blue
                        redInput.setText(red.toString())
                        greenInput.setText(green.toString())
                        blueInput.setText(blue.toString())
                        updatePreview()
                        lastValidHex = normalized
                    } catch (_: Exception) {
                        hexInput.setText(lastValidHex)
                        hexInput.setSelection(hexInput.text.length)
                    }
                } else {
                    hexInput.setText(lastValidHex)
                    hexInput.setSelection(hexInput.text.length)
                }
            }
        }
        hexLayout.addView(hexInput)
        dialogLayout.addView(hexLayout)

        AlertDialog.Builder(context)
            .setTitle("Choose a color")
            .setView(dialogLayout)
            .setPositiveButton("OK") { _, _ ->
                onColorSelected(Color.rgb(red, green, blue))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createExtendedColorGrid(
        context: Context,
        onColorSelected: (Int) -> Unit,
    ): GridLayout {
        val gridLayout =
            GridLayout(context).apply {
                rowCount = 9
                columnCount = 12
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                setPadding(dpToPx(context, 8))
            }
        val cellHeight = dpToPx(context, 28)
        for (row in 1..9) {
            for (col in 0 until 12) {
                val colorInCell =
                    if (col < 11) {
                        val hue = (col * (360f / 11f)) % 360f
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
                        val fraction = (row - 1) / 8f
                        blendColors(Color.WHITE, Color.BLACK, fraction)
                    }
                val colorView =
                    TextView(context).apply {
                        setBackgroundColor(colorInCell)
                        layoutParams =
                            GridLayout.LayoutParams(
                                GridLayout.spec(row - 1),
                                GridLayout.spec(col, 1f),
                            ).apply {
                                width = 0
                                height = cellHeight
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

    private fun blendColors(
        c1: Int,
        c2: Int,
        fraction: Float,
    ): Int {
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

    private fun colorToHexString(color: Int): String {
        return String.format("#%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun dpToPx(
        context: Context,
        dp: Int,
    ): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics,
        ).toInt()
    }

    private fun LinearLayout.setPadding(pixels: Int) {
        setPadding(pixels, pixels, pixels, pixels)
    }
}
