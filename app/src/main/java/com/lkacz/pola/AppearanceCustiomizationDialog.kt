// Filename: AppearanceCustomizationDialog.kt
package com.lkacz.pola

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.fragment.app.DialogFragment

/**
 * Dialog for customizing font sizes, UI colors, transitions, button spacing/padding, etc.
 * Revised to remove references to external managers (FontSizeManager, ColorManager, etc.).
 * Instead, it uses local variables and placeholders for storing/loading.
 */
class AppearanceCustomizationDialog : DialogFragment() {

    // Preview elements
    private lateinit var previewContainer: LinearLayout
    private lateinit var previewHeaderTextView: TextView
    private lateinit var previewBodyTextView: TextView
    private lateinit var previewButton: Button
    private lateinit var previewItemTextView: TextView
    private lateinit var previewResponseButton: Button
    private lateinit var previewContinueButton: Button

    // Sliders for font sizes
    private lateinit var sliderHeader: SeekBar
    private lateinit var tvHeaderSizeValue: TextView
    private lateinit var sliderBody: SeekBar
    private lateinit var tvBodySizeValue: TextView
    private lateinit var sliderButton: SeekBar
    private lateinit var tvButtonSizeValue: TextView
    private lateinit var sliderItem: SeekBar
    private lateinit var tvItemSizeValue: TextView
    private lateinit var sliderResponse: SeekBar
    private lateinit var tvResponseSizeValue: TextView

    // Newly placed in XML for consistency
    private lateinit var sliderContinue: SeekBar
    private lateinit var tvContinueSizeValue: TextView

    // Sliders for response margin/padding
    private lateinit var sliderResponseMargin: SeekBar
    private lateinit var tvResponseMarginValue: TextView
    private lateinit var sliderResponsePaddingH: SeekBar
    private lateinit var tvResponsePaddingHValue: TextView
    private lateinit var sliderResponsePaddingV: SeekBar
    private lateinit var tvResponsePaddingVValue: TextView

    // Sliders for continue button padding
    private lateinit var sliderContinuePaddingH: SeekBar
    private lateinit var tvContinuePaddingHValue: TextView
    private lateinit var sliderContinuePaddingV: SeekBar
    private lateinit var tvContinuePaddingVValue: TextView

    // Color pickers
    private lateinit var headerColorPicker: View
    private lateinit var bodyColorPicker: View
    private lateinit var buttonTextColorPicker: View
    private lateinit var buttonBackgroundColorPicker: View
    private lateinit var itemColorPicker: View
    private lateinit var responseColorPicker: View
    private lateinit var screenBackgroundColorPicker: View

    // If also added in XML:
    private lateinit var continueTextColorPicker: View
    private lateinit var continueBackgroundColorPicker: View

    // Extra preview container for multiple response buttons
    private lateinit var previewPaddingButtonContainer: LinearLayout
    private lateinit var previewPaddingButton1: Button
    private lateinit var previewPaddingButton2: Button

    // In-memory default values (replace with your own load/store logic)
    private var headerTextSize = 24f
    private var bodyTextSize = 16f
    private var buttonTextSize = 16f
    private var itemTextSize = 16f
    private var responseTextSize = 16f
    private var continueTextSize = 18f

    private var headerTextColor = Color.BLACK
    private var bodyTextColor = Color.DKGRAY
    private var buttonTextColor = Color.WHITE
    private var buttonBackgroundColor = Color.BLUE
    private var itemTextColor = Color.BLUE
    private var responseTextColor = Color.RED
    private var screenBgColor = Color.WHITE
    private var continueTextColor = Color.WHITE
    private var continueBackgroundColor = Color.parseColor("#008577")

    // Example transition mode
    private var currentTransitionMode = "off"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_appearance_customization, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Previews (static elements in layout)
        previewContainer = view.findViewById(R.id.previewContainer)
        previewHeaderTextView = view.findViewById(R.id.previewHeaderTextView)
        previewBodyTextView = view.findViewById(R.id.previewBodyTextView)
        previewButton = view.findViewById(R.id.previewButton)
        previewItemTextView = view.findViewById(R.id.previewItemTextView)
        previewResponseButton = view.findViewById(R.id.previewResponseButton)

        // Dynamically add a 'continue' preview button
        previewContinueButton = Button(requireContext()).apply {
            text = "Continue Button"
        }
        previewContainer.addView(previewContinueButton)

        // Sliders from XML
        sliderHeader = view.findViewById(R.id.sliderHeaderSize)
        tvHeaderSizeValue = view.findViewById(R.id.tvHeaderSizeValue)
        sliderBody = view.findViewById(R.id.sliderBodySize)
        tvBodySizeValue = view.findViewById(R.id.tvBodySizeValue)
        sliderButton = view.findViewById(R.id.sliderButtonSize)
        tvButtonSizeValue = view.findViewById(R.id.tvButtonSizeValue)
        sliderItem = view.findViewById(R.id.sliderItemSize)
        tvItemSizeValue = view.findViewById(R.id.tvItemSizeValue)
        sliderResponse = view.findViewById(R.id.sliderResponseSize)
        tvResponseSizeValue = view.findViewById(R.id.tvResponseSizeValue)
        sliderContinue = view.findViewById(R.id.sliderContinue)
        tvContinueSizeValue = view.findViewById(R.id.tvContinueSizeValue)

        // Response margin/padding
        sliderResponseMargin = view.findViewById(R.id.sliderResponseMargin)
        tvResponseMarginValue = view.findViewById(R.id.tvResponseMarginValue)
        sliderResponsePaddingH = view.findViewById(R.id.sliderResponsePaddingH)
        tvResponsePaddingHValue = view.findViewById(R.id.tvResponsePaddingHValue)
        sliderResponsePaddingV = view.findViewById(R.id.sliderResponsePaddingV)
        tvResponsePaddingVValue = view.findViewById(R.id.tvResponsePaddingVValue)

        // Continue button padding
        sliderContinuePaddingH = view.findViewById(R.id.sliderContinuePaddingH)
        tvContinuePaddingHValue = view.findViewById(R.id.tvContinuePaddingHValue)
        sliderContinuePaddingV = view.findViewById(R.id.sliderContinuePaddingV)
        tvContinuePaddingVValue = view.findViewById(R.id.tvContinuePaddingVValue)

        // Color picker views
        headerColorPicker = view.findViewById(R.id.headerColorPicker)
        bodyColorPicker = view.findViewById(R.id.bodyColorPicker)
        buttonTextColorPicker = view.findViewById(R.id.buttonTextColorPicker)
        buttonBackgroundColorPicker = view.findViewById(R.id.buttonBackgroundColorPicker)
        itemColorPicker = view.findViewById(R.id.itemColorPicker)
        responseColorPicker = view.findViewById(R.id.responseColorPicker)
        screenBackgroundColorPicker = view.findViewById(R.id.screenBackgroundColorPicker)
        continueTextColorPicker = view.findViewById(R.id.continueTextColorPicker)
        continueBackgroundColorPicker = view.findViewById(R.id.continueBackgroundColorPicker)

        // Container with two sample response buttons
        previewPaddingButtonContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
        }
        previewContainer.addView(previewPaddingButtonContainer)
        previewPaddingButton1 = Button(requireContext()).apply { text = "Response 1" }
        previewPaddingButton2 = Button(requireContext()).apply { text = "Response 2" }
        previewPaddingButtonContainer.addView(previewPaddingButton1)
        previewPaddingButtonContainer.addView(previewPaddingButton2)

        // -------------------------------
        // Load “stored” sizes (placeholder logic)
        // -------------------------------
        // In a real app, you’d fetch from SharedPreferences or database.
        // Here we simply use local fields that may be changed before the dialog shows.

        sliderHeader.progress = headerTextSize.toInt().coerceIn(8, 100)
        sliderBody.progress = bodyTextSize.toInt().coerceIn(8, 100)
        sliderButton.progress = buttonTextSize.toInt().coerceIn(8, 100)
        sliderItem.progress = itemTextSize.toInt().coerceIn(8, 100)
        sliderResponse.progress = responseTextSize.toInt().coerceIn(8, 100)
        sliderContinue.progress = continueTextSize.toInt().coerceIn(8, 100)

        tvHeaderSizeValue.text = sliderHeader.progress.toString()
        tvBodySizeValue.text = sliderBody.progress.toString()
        tvButtonSizeValue.text = sliderButton.progress.toString()
        tvItemSizeValue.text = sliderItem.progress.toString()
        tvResponseSizeValue.text = sliderResponse.progress.toString()
        tvContinueSizeValue.text = sliderContinue.progress.toString()

        // Apply to previews
        previewHeaderTextView.textSize = sliderHeader.progress.toFloat()
        previewBodyTextView.textSize = sliderBody.progress.toFloat()
        previewButton.textSize = sliderButton.progress.toFloat()
        previewItemTextView.textSize = sliderItem.progress.toFloat()
        previewResponseButton.textSize = sliderResponse.progress.toFloat()
        previewPaddingButton1.textSize = sliderResponse.progress.toFloat()
        previewPaddingButton2.textSize = sliderResponse.progress.toFloat()
        previewContinueButton.textSize = sliderContinue.progress.toFloat()

        // -------------------------------
        // Load “stored” colors (placeholder logic)
        // -------------------------------
        // In a real app, you’d fetch from SharedPreferences or database.
        // Here we simply use local fields defined above.

        // Apply colors to previews
        previewHeaderTextView.setTextColor(headerTextColor)
        previewBodyTextView.setTextColor(bodyTextColor)
        previewButton.setTextColor(buttonTextColor)
        previewButton.setBackgroundColor(buttonBackgroundColor)
        previewItemTextView.setTextColor(itemTextColor)
        previewResponseButton.setTextColor(responseTextColor)
        previewResponseButton.setBackgroundColor(buttonBackgroundColor)
        previewContinueButton.setTextColor(continueTextColor)
        previewContinueButton.setBackgroundColor(continueBackgroundColor)
        previewContainer.setBackgroundColor(screenBgColor)

        // Update color picker backgrounds
        applyColorPickerBoxColor(headerColorPicker, headerTextColor)
        applyColorPickerBoxColor(bodyColorPicker, bodyTextColor)
        applyColorPickerBoxColor(buttonTextColorPicker, buttonTextColor)
        applyColorPickerBoxColor(buttonBackgroundColorPicker, buttonBackgroundColor)
        applyColorPickerBoxColor(itemColorPicker, itemTextColor)
        applyColorPickerBoxColor(responseColorPicker, responseTextColor)
        applyColorPickerBoxColor(screenBackgroundColorPicker, screenBgColor)
        applyColorPickerBoxColor(continueTextColorPicker, continueTextColor)
        applyColorPickerBoxColor(continueBackgroundColorPicker, continueBackgroundColor)

        // -------------------------------
        // Load “stored” spacing (placeholder logic)
        // -------------------------------
        sliderResponseMargin.max = 100
        sliderResponseMargin.progress = 0
        tvResponseMarginValue.text = "0"
        applyResponseButtonMargin(0)

        sliderResponsePaddingH.max = 100
        sliderResponsePaddingH.progress = 0
        tvResponsePaddingHValue.text = "0"

        sliderResponsePaddingV.max = 100
        sliderResponsePaddingV.progress = 0
        tvResponsePaddingVValue.text = "0"
        applyResponseButtonPadding()

        sliderContinuePaddingH.max = 100
        sliderContinuePaddingH.progress = 0
        tvContinuePaddingHValue.text = "0"

        sliderContinuePaddingV.max = 100
        sliderContinuePaddingV.progress = 0
        tvContinuePaddingVValue.text = "0"
        applyContinueButtonPadding()

        // -------------------------------
        // Handle slider changes
        // -------------------------------
        sliderHeader.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            headerTextSize = size.toFloat()  // Placeholder “store”
            previewHeaderTextView.textSize = size.toFloat()
            tvHeaderSizeValue.text = size.toString()
        })
        sliderBody.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            bodyTextSize = size.toFloat()  // Placeholder “store”
            previewBodyTextView.textSize = size.toFloat()
            tvBodySizeValue.text = size.toString()
        })
        sliderButton.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            buttonTextSize = size.toFloat()  // Placeholder “store”
            previewButton.textSize = size.toFloat()
            tvButtonSizeValue.text = size.toString()
        })
        sliderItem.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            itemTextSize = size.toFloat()  // Placeholder “store”
            previewItemTextView.textSize = size.toFloat()
            tvItemSizeValue.text = size.toString()
        })
        sliderResponse.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            responseTextSize = size.toFloat()  // Placeholder “store”
            previewResponseButton.textSize = size.toFloat()
            previewPaddingButton1.textSize = size.toFloat()
            previewPaddingButton2.textSize = size.toFloat()
            tvResponseSizeValue.text = size.toString()
        })
        sliderContinue.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            continueTextSize = size.toFloat()  // Placeholder “store”
            previewContinueButton.textSize = size.toFloat()
            tvContinueSizeValue.text = size.toString()
        })

        // Response margin/padding
        sliderResponseMargin.setOnSeekBarChangeListener(simpleSeekBarListener {
            val marginDp = it.coerceIn(0, 100)
            tvResponseMarginValue.text = marginDp.toString()
            applyResponseButtonMargin(marginDp)
        })
        sliderResponsePaddingH.setOnSeekBarChangeListener(simpleSeekBarListener {
            val ph = it.coerceIn(0, 100)
            tvResponsePaddingHValue.text = ph.toString()
            applyResponseButtonPadding()
        })
        sliderResponsePaddingV.setOnSeekBarChangeListener(simpleSeekBarListener {
            val pv = it.coerceIn(0, 100)
            tvResponsePaddingVValue.text = pv.toString()
            applyResponseButtonPadding()
        })

        // Continue button padding
        sliderContinuePaddingH.setOnSeekBarChangeListener(simpleSeekBarListener {
            val ph = it.coerceIn(0, 100)
            tvContinuePaddingHValue.text = ph.toString()
            applyContinueButtonPadding()
        })
        sliderContinuePaddingV.setOnSeekBarChangeListener(simpleSeekBarListener {
            val pv = it.coerceIn(0, 100)
            tvContinuePaddingVValue.text = pv.toString()
            applyContinueButtonPadding()
        })

        // -------------------------------
        // Color pickers
        // -------------------------------
        headerColorPicker.setOnClickListener {
            openColorPicker(headerTextColor) { chosenColor ->
                headerTextColor = chosenColor
                applyColorPickerBoxColor(headerColorPicker, chosenColor)
                previewHeaderTextView.setTextColor(chosenColor)
            }
        }
        bodyColorPicker.setOnClickListener {
            openColorPicker(bodyTextColor) { chosenColor ->
                bodyTextColor = chosenColor
                applyColorPickerBoxColor(bodyColorPicker, chosenColor)
                previewBodyTextView.setTextColor(chosenColor)
            }
        }
        buttonTextColorPicker.setOnClickListener {
            openColorPicker(buttonTextColor) { chosenColor ->
                buttonTextColor = chosenColor
                applyColorPickerBoxColor(buttonTextColorPicker, chosenColor)
                previewButton.setTextColor(chosenColor)
            }
        }
        buttonBackgroundColorPicker.setOnClickListener {
            openColorPicker(buttonBackgroundColor) { chosenColor ->
                buttonBackgroundColor = chosenColor
                applyColorPickerBoxColor(buttonBackgroundColorPicker, chosenColor)
                // Also recolor the response preview
                previewButton.setBackgroundColor(chosenColor)
                previewResponseButton.setBackgroundColor(chosenColor)
                previewPaddingButton1.setBackgroundColor(chosenColor)
                previewPaddingButton2.setBackgroundColor(chosenColor)
            }
        }
        itemColorPicker.setOnClickListener {
            openColorPicker(itemTextColor) { chosenColor ->
                itemTextColor = chosenColor
                applyColorPickerBoxColor(itemColorPicker, chosenColor)
                previewItemTextView.setTextColor(chosenColor)
            }
        }
        responseColorPicker.setOnClickListener {
            openColorPicker(responseTextColor) { chosenColor ->
                responseTextColor = chosenColor
                applyColorPickerBoxColor(responseColorPicker, chosenColor)
                previewResponseButton.setTextColor(chosenColor)
                previewPaddingButton1.setTextColor(chosenColor)
                previewPaddingButton2.setTextColor(chosenColor)
            }
        }
        screenBackgroundColorPicker.setOnClickListener {
            openColorPicker(screenBgColor) { chosenColor ->
                screenBgColor = chosenColor
                applyColorPickerBoxColor(screenBackgroundColorPicker, chosenColor)
                previewContainer.setBackgroundColor(chosenColor)
            }
        }
        continueTextColorPicker.setOnClickListener {
            openColorPicker(continueTextColor) { chosenColor ->
                continueTextColor = chosenColor
                applyColorPickerBoxColor(continueTextColorPicker, chosenColor)
                previewContinueButton.setTextColor(chosenColor)
            }
        }
        continueBackgroundColorPicker.setOnClickListener {
            openColorPicker(continueBackgroundColor) { chosenColor ->
                continueBackgroundColor = chosenColor
                applyColorPickerBoxColor(continueBackgroundColorPicker, chosenColor)
                previewContinueButton.setBackgroundColor(chosenColor)
            }
        }

        // -------------------------------
        // Spinner for transitions (placeholder logic)
        // -------------------------------
        val transitionsSpinner = view.findViewById<Spinner>(R.id.spinnerTransitions)
        val transitionOptions = listOf("No transition", "Slide to left")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, transitionOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        transitionsSpinner.adapter = adapter

        // Example: pick first item if “off”, second if “slide”
        transitionsSpinner.setSelection(
            if (currentTransitionMode == "off") 0 else 1,
            false
        )
        transitionsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                currentTransitionMode = if (position == 0) "off" else "slide"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // OK / Cancel
        val okButton = view.findViewById<Button>(R.id.btnOk)
        okButton.setOnClickListener {
            // In a real app, you'd persist everything to SharedPreferences or a database here
            dismiss()
        }
        val cancelButton = view.findViewById<Button>(R.id.btnCancel)
        cancelButton.setOnClickListener { dismiss() }

        // "Defaults" Button
        val buttonsLayout = cancelButton.parent as? LinearLayout
        val defaultsButton = Button(requireContext()).apply {
            id = View.generateViewId()
            text = "Defaults"
            setOnClickListener { restoreAllDefaults() }
        }
        buttonsLayout?.addView(defaultsButton, buttonsLayout.indexOfChild(cancelButton))
    }

    /**
     * Applies a margin in dp to the two "response" preview buttons.
     */
    private fun applyResponseButtonMargin(marginDp: Int) {
        val scale = resources.displayMetrics.density
        val marginPx = (marginDp * scale + 0.5f).toInt()
        fun updateButtonMargin(button: Button) {
            val params = button.layoutParams as LinearLayout.LayoutParams
            params.setMargins(marginPx, marginPx, marginPx, marginPx)
            button.layoutParams = params
        }
        updateButtonMargin(previewPaddingButton1)
        updateButtonMargin(previewPaddingButton2)
    }

    /**
     * Applies the stored horizontal/vertical padding to the "response" preview buttons.
     * For now, just uses the slider values (placeholder).
     */
    private fun applyResponseButtonPadding() {
        val scale = resources.displayMetrics.density
        val horizontalPx = (sliderResponsePaddingH.progress * scale + 0.5f).toInt()
        val verticalPx = (sliderResponsePaddingV.progress * scale + 0.5f).toInt()
        fun updateButtonPadding(button: Button) {
            button.setPadding(horizontalPx, verticalPx, horizontalPx, verticalPx)
        }
        updateButtonPadding(previewPaddingButton1)
        updateButtonPadding(previewPaddingButton2)
        previewResponseButton.setPadding(horizontalPx, verticalPx, horizontalPx, verticalPx)
    }

    /**
     * Applies the stored horizontal/vertical padding to the "continue" preview button.
     */
    private fun applyContinueButtonPadding() {
        val scale = resources.displayMetrics.density
        val horizontalPx = (sliderContinuePaddingH.progress * scale + 0.5f).toInt()
        val verticalPx = (sliderContinuePaddingV.progress * scale + 0.5f).toInt()
        previewContinueButton.setPadding(horizontalPx, verticalPx, horizontalPx, verticalPx)
    }

    /**
     * Updates the background of a color picker “box” to show the chosen color.
     */
    private fun applyColorPickerBoxColor(picker: View, color: Int) {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.black_outline)
        if (drawable is GradientDrawable) {
            drawable.setColor(color)
            picker.background = drawable
        }
    }

    /**
     * Displays a simple RGB color picker, with OK/Cancel.
     */
    private fun openColorPicker(initialColor: Int, onColorSelected: (Int) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.color_picker_dialog, null)
        val seekRed = dialogView.findViewById<SeekBar>(R.id.seekRed)
        val seekGreen = dialogView.findViewById<SeekBar>(R.id.seekGreen)
        val seekBlue = dialogView.findViewById<SeekBar>(R.id.seekBlue)
        val colorPreview = dialogView.findViewById<View>(R.id.colorPreview)

        val initR = initialColor.red
        val initG = initialColor.green
        val initB = initialColor.blue

        seekRed.progress = initR
        seekGreen.progress = initG
        seekBlue.progress = initB
        colorPreview.setBackgroundColor(Color.rgb(initR, initG, initB))

        val updatePreview = {
            val newColor = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
            colorPreview.setBackgroundColor(newColor)
        }

        seekRed.setOnSeekBarChangeListener(simpleSeekBarListener { updatePreview() })
        seekGreen.setOnSeekBarChangeListener(simpleSeekBarListener { updatePreview() })
        seekBlue.setOnSeekBarChangeListener(simpleSeekBarListener { updatePreview() })

        AlertDialog.Builder(requireContext())
            .setTitle("Pick a Color")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val chosenColor = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
                onColorSelected(chosenColor)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Restores everything to defaults (in-memory). Replace with real logic if needed.
     */
    private fun restoreAllDefaults() {
        // Default text sizes
        val defaultHeaderSize = 24f
        val defaultBodySize = 16f
        val defaultButtonSize = 16f
        val defaultItemSize = 16f
        val defaultResponseSize = 16f
        val defaultContinueSize = 18f

        // Default colors
        val defaultHeaderColor = Color.parseColor("#37474F")
        val defaultBodyColor = Color.parseColor("#616161")
        val defaultButtonTextColor = Color.WHITE
        val defaultButtonBgColor = Color.parseColor("#008577")
        val defaultItemColor = Color.parseColor("#008577")
        val defaultResponseColor = Color.parseColor("#C51162")
        val defaultScreenBgColor = Color.parseColor("#F5F5F5")
        val defaultContinueTextColor = Color.WHITE
        val defaultContinueBgColor = Color.parseColor("#008577")

        // Reset local fields
        headerTextSize = defaultHeaderSize
        bodyTextSize = defaultBodySize
        buttonTextSize = defaultButtonSize
        itemTextSize = defaultItemSize
        responseTextSize = defaultResponseSize
        continueTextSize = defaultContinueSize

        headerTextColor = defaultHeaderColor
        bodyTextColor = defaultBodyColor
        buttonTextColor = defaultButtonTextColor
        buttonBackgroundColor = defaultButtonBgColor
        itemTextColor = defaultItemColor
        responseTextColor = defaultResponseColor
        screenBgColor = defaultScreenBgColor
        continueTextColor = defaultContinueTextColor
        continueBackgroundColor = defaultContinueBgColor

        // Apply to sliders and previews
        sliderHeader.progress = headerTextSize.toInt()
        sliderBody.progress = bodyTextSize.toInt()
        sliderButton.progress = buttonTextSize.toInt()
        sliderItem.progress = itemTextSize.toInt()
        sliderResponse.progress = responseTextSize.toInt()
        sliderContinue.progress = continueTextSize.toInt()

        tvHeaderSizeValue.text = sliderHeader.progress.toString()
        tvBodySizeValue.text = sliderBody.progress.toString()
        tvButtonSizeValue.text = sliderButton.progress.toString()
        tvItemSizeValue.text = sliderItem.progress.toString()
        tvResponseSizeValue.text = sliderResponse.progress.toString()
        tvContinueSizeValue.text = sliderContinue.progress.toString()

        previewHeaderTextView.textSize = headerTextSize
        previewBodyTextView.textSize = bodyTextSize
        previewButton.textSize = buttonTextSize
        previewItemTextView.textSize = itemTextSize
        previewResponseButton.textSize = responseTextSize
        previewContinueButton.textSize = continueTextSize

        applyColorPickerBoxColor(headerColorPicker, headerTextColor)
        applyColorPickerBoxColor(bodyColorPicker, bodyTextColor)
        applyColorPickerBoxColor(buttonTextColorPicker, buttonTextColor)
        applyColorPickerBoxColor(buttonBackgroundColorPicker, buttonBackgroundColor)
        applyColorPickerBoxColor(itemColorPicker, itemTextColor)
        applyColorPickerBoxColor(responseColorPicker, responseTextColor)
        applyColorPickerBoxColor(screenBackgroundColorPicker, screenBgColor)
        applyColorPickerBoxColor(continueTextColorPicker, continueTextColor)
        applyColorPickerBoxColor(continueBackgroundColorPicker, continueBackgroundColor)

        previewHeaderTextView.setTextColor(headerTextColor)
        previewBodyTextView.setTextColor(bodyTextColor)
        previewButton.setTextColor(buttonTextColor)
        previewButton.setBackgroundColor(buttonBackgroundColor)
        previewItemTextView.setTextColor(itemTextColor)
        previewResponseButton.setTextColor(responseTextColor)
        previewResponseButton.setBackgroundColor(buttonBackgroundColor)
        previewContinueButton.setTextColor(continueTextColor)
        previewContinueButton.setBackgroundColor(continueBackgroundColor)
        previewContainer.setBackgroundColor(screenBgColor)

        // Reset margins/padding to zero
        sliderResponseMargin.progress = 0
        tvResponseMarginValue.text = "0"
        applyResponseButtonMargin(0)

        sliderResponsePaddingH.progress = 0
        tvResponsePaddingHValue.text = "0"
        sliderResponsePaddingV.progress = 0
        tvResponsePaddingVValue.text = "0"
        applyResponseButtonPadding()

        sliderContinuePaddingH.progress = 0
        tvContinuePaddingHValue.text = "0"
        sliderContinuePaddingV.progress = 0
        tvContinuePaddingVValue.text = "0"
        applyContinueButtonPadding()
    }

    /**
     * Simple SeekBar listener to reduce boilerplate
     */
    private fun simpleSeekBarListener(onValueChanged: (Int) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onValueChanged(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
}
