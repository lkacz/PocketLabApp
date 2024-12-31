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
 * Dialog for customizing font sizes, UI colors, transitions, and response-button padding.
 * Timer-sound logic has been moved out into AlarmCustomizationDialog.
 */
class AppearanceCustomizationDialog : DialogFragment() {

    private lateinit var previewContainer: LinearLayout
    private lateinit var previewHeaderTextView: TextView
    private lateinit var previewBodyTextView: TextView
    private lateinit var previewButton: Button
    private lateinit var previewItemTextView: TextView
    private lateinit var previewResponseButton: Button

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

    private lateinit var sliderResponsePadding: SeekBar
    private lateinit var tvResponsePaddingValue: TextView

    private lateinit var headerColorPicker: View
    private lateinit var bodyColorPicker: View
    private lateinit var buttonTextColorPicker: View
    private lateinit var buttonBackgroundColorPicker: View
    private lateinit var itemColorPicker: View
    private lateinit var responseColorPicker: View
    private lateinit var screenBackgroundColorPicker: View

    // Additional preview container with two stacked buttons for padding demonstration
    private lateinit var previewPaddingButtonContainer: LinearLayout
    private lateinit var previewPaddingButton1: Button
    private lateinit var previewPaddingButton2: Button

    // Current colors
    private var headerTextColor: Int = Color.BLACK
    private var bodyTextColor: Int = Color.DKGRAY
    private var buttonTextColor: Int = Color.WHITE
    private var buttonBackgroundColor: Int = Color.BLUE
    private var itemTextColor: Int = Color.BLUE
    private var responseTextColor: Int = Color.RED
    private var screenBgColor: Int = Color.WHITE

    // Transitions spinner
    private lateinit var transitionsSpinner: Spinner

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

        // Previews
        previewContainer = view.findViewById(R.id.previewContainer)
        previewHeaderTextView = view.findViewById(R.id.previewHeaderTextView)
        previewBodyTextView = view.findViewById(R.id.previewBodyTextView)
        previewButton = view.findViewById(R.id.previewButton)
        previewItemTextView = view.findViewById(R.id.previewItemTextView)
        previewResponseButton = view.findViewById(R.id.previewResponseButton)

        // Sliders
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

        sliderResponsePadding = view.findViewById(R.id.sliderResponsePadding)
        tvResponsePaddingValue = view.findViewById(R.id.tvResponsePaddingValue)

        // Color pickers
        headerColorPicker = view.findViewById(R.id.headerColorPicker)
        bodyColorPicker = view.findViewById(R.id.bodyColorPicker)
        buttonTextColorPicker = view.findViewById(R.id.buttonTextColorPicker)
        buttonBackgroundColorPicker = view.findViewById(R.id.buttonBackgroundColorPicker)
        itemColorPicker = view.findViewById(R.id.itemColorPicker)
        responseColorPicker = view.findViewById(R.id.responseColorPicker)
        screenBackgroundColorPicker = view.findViewById(R.id.screenBackgroundColorPicker)

        // Padding preview container & buttons
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

        // Two stacked preview buttons
        previewPaddingButton1 = Button(requireContext()).apply {
            text = "Response 1"
            textSize = FontSizeManager.getResponseSize(requireContext())
            setTextColor(ColorManager.getResponseTextColor(requireContext()))
            setBackgroundColor(ColorManager.getButtonBackgroundColor(requireContext()))
        }
        previewPaddingButton2 = Button(requireContext()).apply {
            text = "Response 2"
            textSize = FontSizeManager.getResponseSize(requireContext())
            setTextColor(ColorManager.getResponseTextColor(requireContext()))
            setBackgroundColor(ColorManager.getButtonBackgroundColor(requireContext()))
        }
        previewPaddingButtonContainer.addView(previewPaddingButton1)
        previewPaddingButtonContainer.addView(previewPaddingButton2)

        val ctx = requireContext()
        // Load stored sizes
        val currentHeaderSize = FontSizeManager.getHeaderSize(ctx).toInt()
        val currentBodySize = FontSizeManager.getBodySize(ctx).toInt()
        val currentButtonSize = FontSizeManager.getButtonSize(ctx).toInt()
        val currentItemSize = FontSizeManager.getItemSize(ctx).toInt()
        val currentResponseSize = FontSizeManager.getResponseSize(ctx).toInt()

        sliderHeader.progress = currentHeaderSize.coerceIn(8, 100)
        sliderBody.progress = currentBodySize.coerceIn(8, 100)
        sliderButton.progress = currentButtonSize.coerceIn(8, 100)
        sliderItem.progress = currentItemSize.coerceIn(8, 100)
        sliderResponse.progress = currentResponseSize.coerceIn(8, 100)

        tvHeaderSizeValue.text = sliderHeader.progress.toString()
        tvBodySizeValue.text = sliderBody.progress.toString()
        tvButtonSizeValue.text = sliderButton.progress.toString()
        tvItemSizeValue.text = sliderItem.progress.toString()
        tvResponseSizeValue.text = sliderResponse.progress.toString()

        previewHeaderTextView.textSize = sliderHeader.progress.toFloat()
        previewBodyTextView.textSize = sliderBody.progress.toFloat()
        previewButton.textSize = sliderButton.progress.toFloat()
        previewItemTextView.textSize = sliderItem.progress.toFloat()
        previewResponseButton.textSize = sliderResponse.progress.toFloat()

        // Load colors
        headerTextColor = ColorManager.getHeaderTextColor(ctx)
        bodyTextColor = ColorManager.getBodyTextColor(ctx)
        buttonTextColor = ColorManager.getButtonTextColor(ctx)
        buttonBackgroundColor = ColorManager.getButtonBackgroundColor(ctx)
        itemTextColor = ColorManager.getItemTextColor(ctx)
        responseTextColor = ColorManager.getResponseTextColor(ctx)
        screenBgColor = ColorManager.getScreenBackgroundColor(ctx)

        previewHeaderTextView.setTextColor(headerTextColor)
        previewBodyTextView.setTextColor(bodyTextColor)
        previewButton.setTextColor(buttonTextColor)
        previewButton.setBackgroundColor(buttonBackgroundColor)
        previewItemTextView.setTextColor(itemTextColor)
        previewResponseButton.setTextColor(responseTextColor)
        previewResponseButton.setBackgroundColor(buttonBackgroundColor)
        previewContainer.setBackgroundColor(screenBgColor)

        applyColorPickerBoxColor(headerColorPicker, headerTextColor)
        applyColorPickerBoxColor(bodyColorPicker, bodyTextColor)
        applyColorPickerBoxColor(buttonTextColorPicker, buttonTextColor)
        applyColorPickerBoxColor(buttonBackgroundColorPicker, buttonBackgroundColor)
        applyColorPickerBoxColor(itemColorPicker, itemTextColor)
        applyColorPickerBoxColor(responseColorPicker, responseTextColor)
        applyColorPickerBoxColor(screenBackgroundColorPicker, screenBgColor)

        sliderHeader.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            FontSizeManager.setHeaderSize(ctx, size.toFloat())
            previewHeaderTextView.textSize = size.toFloat()
            tvHeaderSizeValue.text = size.toString()
        })

        sliderBody.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            FontSizeManager.setBodySize(ctx, size.toFloat())
            previewBodyTextView.textSize = size.toFloat()
            tvBodySizeValue.text = size.toString()
        })

        sliderButton.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            FontSizeManager.setButtonSize(ctx, size.toFloat())
            previewButton.textSize = size.toFloat()
            tvButtonSizeValue.text = size.toString()
        })

        sliderItem.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            FontSizeManager.setItemSize(ctx, size.toFloat())
            previewItemTextView.textSize = size.toFloat()
            tvItemSizeValue.text = size.toString()
        })

        sliderResponse.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            FontSizeManager.setResponseSize(ctx, size.toFloat())
            previewResponseButton.textSize = size.toFloat()
            previewPaddingButton1.textSize = size.toFloat()
            previewPaddingButton2.textSize = size.toFloat()
            tvResponseSizeValue.text = size.toString()
        })

        // Handle response-button padding
        val currentPaddingDp = SpacingManager.getResponseButtonPadding(ctx).toInt().coerceIn(0, 100)
        sliderResponsePadding.max = 100
        sliderResponsePadding.progress = currentPaddingDp
        tvResponsePaddingValue.text = currentPaddingDp.toString()
        applyResponseButtonMargin(currentPaddingDp)

        sliderResponsePadding.setOnSeekBarChangeListener(simpleSeekBarListener {
            val pad = it.coerceIn(0, 100)
            SpacingManager.setResponseButtonPadding(ctx, pad.toFloat())
            tvResponsePaddingValue.text = pad.toString()
            applyResponseButtonMargin(pad)
        })

        // Color pickers
        headerColorPicker.setOnClickListener {
            openColorPicker(headerTextColor) { chosenColor ->
                headerTextColor = chosenColor
                applyColorPickerBoxColor(headerColorPicker, chosenColor)
                ColorManager.setHeaderTextColor(ctx, chosenColor)
                previewHeaderTextView.setTextColor(chosenColor)
            }
        }
        bodyColorPicker.setOnClickListener {
            openColorPicker(bodyTextColor) { chosenColor ->
                bodyTextColor = chosenColor
                applyColorPickerBoxColor(bodyColorPicker, chosenColor)
                ColorManager.setBodyTextColor(ctx, chosenColor)
                previewBodyTextView.setTextColor(chosenColor)
            }
        }
        buttonTextColorPicker.setOnClickListener {
            openColorPicker(buttonTextColor) { chosenColor ->
                buttonTextColor = chosenColor
                applyColorPickerBoxColor(buttonTextColorPicker, chosenColor)
                ColorManager.setButtonTextColor(ctx, chosenColor)
                previewButton.setTextColor(chosenColor)
            }
        }
        buttonBackgroundColorPicker.setOnClickListener {
            openColorPicker(buttonBackgroundColor) { chosenColor ->
                buttonBackgroundColor = chosenColor
                applyColorPickerBoxColor(buttonBackgroundColorPicker, chosenColor)
                ColorManager.setButtonBackgroundColor(ctx, chosenColor)
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
                ColorManager.setItemTextColor(ctx, chosenColor)
                previewItemTextView.setTextColor(chosenColor)
            }
        }
        responseColorPicker.setOnClickListener {
            openColorPicker(responseTextColor) { chosenColor ->
                responseTextColor = chosenColor
                applyColorPickerBoxColor(responseColorPicker, chosenColor)
                ColorManager.setResponseTextColor(ctx, chosenColor)
                previewResponseButton.setTextColor(chosenColor)
                previewPaddingButton1.setTextColor(chosenColor)
                previewPaddingButton2.setTextColor(chosenColor)
            }
        }
        screenBackgroundColorPicker.setOnClickListener {
            openColorPicker(screenBgColor) { chosenColor ->
                screenBgColor = chosenColor
                applyColorPickerBoxColor(screenBackgroundColorPicker, chosenColor)
                ColorManager.setScreenBackgroundColor(ctx, chosenColor)
                previewContainer.setBackgroundColor(chosenColor)
            }
        }

        // Transitions Spinner
        transitionsSpinner = view.findViewById(R.id.spinnerTransitions)
        val transitionOptions = listOf("No transition", "Slide to left")
        val adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, transitionOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        transitionsSpinner.adapter = adapter

        // Set initial selection based on stored mode
        val storedMode = TransitionManager.getTransitionMode(ctx)
        transitionsSpinner.setSelection(if (storedMode == "off") 0 else 1, false)
        transitionsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                v: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> TransitionManager.setTransitionMode(ctx, "off")
                    1 -> TransitionManager.setTransitionMode(ctx, "slide")
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // OK / Cancel
        val okButton = view.findViewById<Button>(R.id.btnOk)
        okButton.setOnClickListener {
            dismiss()
        }
        val cancelButton = view.findViewById<Button>(R.id.btnCancel)
        cancelButton.setOnClickListener {
            dismiss()
        }

        // "Defaults" Button
        val buttonsLayout = cancelButton.parent as? LinearLayout
        val defaultsButton = Button(requireContext()).apply {
            id = View.generateViewId()
            text = "Defaults"
            setOnClickListener {
                restoreAllDefaults(ctx)
            }
        }
        buttonsLayout?.addView(defaultsButton, buttonsLayout.indexOfChild(cancelButton))
    }

    private fun applyResponseButtonMargin(padDp: Int) {
        val scale = resources.displayMetrics.density
        val padPx = (padDp * scale + 0.5f).toInt()

        fun updateButtonMargin(button: Button) {
            val params = button.layoutParams as LinearLayout.LayoutParams
            params.setMargins(padPx, padPx, padPx, padPx)
            button.layoutParams = params
        }
        updateButtonMargin(previewPaddingButton1)
        updateButtonMargin(previewPaddingButton2)
    }

    private fun applyColorPickerBoxColor(picker: View, color: Int) {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.black_outline)
        if (drawable is GradientDrawable) {
            drawable.setColor(color)
            picker.background = drawable
        }
    }

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

    private fun restoreAllDefaults(ctx: Context) {
        val defaultHeaderSize = 48f
        val defaultBodySize = 18f
        val defaultButtonSize = 18f
        val defaultItemSize = 22f
        val defaultResponseSize = 16f

        val defaultHeaderColor = Color.parseColor("#37474F")
        val defaultBodyColor = Color.parseColor("#616161")
        val defaultButtonTextColor = Color.WHITE
        val defaultButtonBgColor = Color.parseColor("#008577")
        val defaultItemColor = Color.parseColor("#008577")
        val defaultResponseColor = Color.parseColor("#C51162")
        val defaultScreenBgColor = Color.parseColor("#F5F5F5")

        FontSizeManager.setHeaderSize(ctx, defaultHeaderSize.coerceIn(8f, 100f))
        FontSizeManager.setBodySize(ctx, defaultBodySize.coerceIn(8f, 100f))
        FontSizeManager.setButtonSize(ctx, defaultButtonSize.coerceIn(8f, 100f))
        FontSizeManager.setItemSize(ctx, defaultItemSize.coerceIn(8f, 100f))
        FontSizeManager.setResponseSize(ctx, defaultResponseSize.coerceIn(8f, 100f))

        sliderHeader.progress = defaultHeaderSize.toInt().coerceIn(8, 100)
        sliderBody.progress = defaultBodySize.toInt().coerceIn(8, 100)
        sliderButton.progress = defaultButtonSize.toInt().coerceIn(8, 100)
        sliderItem.progress = defaultItemSize.toInt().coerceIn(8, 100)
        sliderResponse.progress = defaultResponseSize.toInt().coerceIn(8, 100)

        tvHeaderSizeValue.text = sliderHeader.progress.toString()
        tvBodySizeValue.text = sliderBody.progress.toString()
        tvButtonSizeValue.text = sliderButton.progress.toString()
        tvItemSizeValue.text = sliderItem.progress.toString()
        tvResponseSizeValue.text = sliderResponse.progress.toString()

        previewHeaderTextView.textSize = sliderHeader.progress.toFloat()
        previewBodyTextView.textSize = sliderBody.progress.toFloat()
        previewButton.textSize = sliderButton.progress.toFloat()
        previewItemTextView.textSize = sliderItem.progress.toFloat()
        previewResponseButton.textSize = sliderResponse.progress.toFloat()
        previewPaddingButton1.textSize = sliderResponse.progress.toFloat()
        previewPaddingButton2.textSize = sliderResponse.progress.toFloat()

        ColorManager.setHeaderTextColor(ctx, defaultHeaderColor)
        ColorManager.setBodyTextColor(ctx, defaultBodyColor)
        ColorManager.setButtonTextColor(ctx, defaultButtonTextColor)
        ColorManager.setButtonBackgroundColor(ctx, defaultButtonBgColor)
        ColorManager.setItemTextColor(ctx, defaultItemColor)
        ColorManager.setResponseTextColor(ctx, defaultResponseColor)
        ColorManager.setScreenBackgroundColor(ctx, defaultScreenBgColor)

        headerTextColor = defaultHeaderColor
        bodyTextColor = defaultBodyColor
        buttonTextColor = defaultButtonTextColor
        buttonBackgroundColor = defaultButtonBgColor
        itemTextColor = defaultItemColor
        responseTextColor = defaultResponseColor
        screenBgColor = defaultScreenBgColor

        applyColorPickerBoxColor(headerColorPicker, defaultHeaderColor)
        applyColorPickerBoxColor(bodyColorPicker, defaultBodyColor)
        applyColorPickerBoxColor(buttonTextColorPicker, defaultButtonTextColor)
        applyColorPickerBoxColor(buttonBackgroundColorPicker, defaultButtonBgColor)
        applyColorPickerBoxColor(itemColorPicker, defaultItemColor)
        applyColorPickerBoxColor(responseColorPicker, defaultResponseColor)
        applyColorPickerBoxColor(screenBackgroundColorPicker, defaultScreenBgColor)

        previewHeaderTextView.setTextColor(defaultHeaderColor)
        previewBodyTextView.setTextColor(defaultBodyColor)
        previewButton.setTextColor(defaultButtonTextColor)
        previewButton.setBackgroundColor(defaultButtonBgColor)
        previewItemTextView.setTextColor(defaultItemColor)
        previewResponseButton.setTextColor(defaultResponseColor)
        previewResponseButton.setBackgroundColor(defaultButtonBgColor)
        previewContainer.setBackgroundColor(defaultScreenBgColor)

        // Reset response-button padding
        SpacingManager.setResponseButtonPadding(ctx, 0f)
        sliderResponsePadding.progress = 0
        tvResponsePaddingValue.text = "0"
        applyResponseButtonMargin(0)

        // NO forced transitions set here anymore. (Removed hardcoded "slide" default)
    }

    private fun simpleSeekBarListener(onValueChanged: (Int) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onValueChanged(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
}
