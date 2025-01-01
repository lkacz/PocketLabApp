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
 * Timer-sound logic was moved out into AlarmCustomizationDialog.
 */
class AppearanceCustomizationDialog : DialogFragment() {

    private lateinit var previewContainer: LinearLayout
    private lateinit var previewHeaderTextView: TextView
    private lateinit var previewBodyTextView: TextView
    private lateinit var previewButton: Button
    private lateinit var previewItemTextView: TextView
    private lateinit var previewResponseButton: Button

    // We add a second "continue button" preview
    private lateinit var previewContinueButton: Button

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

    // CONTINUE button slider
    private lateinit var sliderContinue: SeekBar
    private lateinit var tvContinueSizeValue: TextView

    // Sliders for spacing (margin) and separate horizontal/vertical padding for response
    private lateinit var sliderResponseMargin: SeekBar
    private lateinit var tvResponseMarginValue: TextView
    private lateinit var sliderResponsePaddingH: SeekBar
    private lateinit var tvResponsePaddingHValue: TextView
    private lateinit var sliderResponsePaddingV: SeekBar
    private lateinit var tvResponsePaddingVValue: TextView

    // New sliders for CONTINUE button padding
    private lateinit var sliderContinuePaddingH: SeekBar
    private lateinit var tvContinuePaddingHValue: TextView
    private lateinit var sliderContinuePaddingV: SeekBar
    private lateinit var tvContinuePaddingVValue: TextView

    private lateinit var headerColorPicker: View
    private lateinit var bodyColorPicker: View
    private lateinit var buttonTextColorPicker: View
    private lateinit var buttonBackgroundColorPicker: View
    private lateinit var itemColorPicker: View
    private lateinit var responseColorPicker: View
    private lateinit var screenBackgroundColorPicker: View

    // New color pickers for CONTINUE button
    private lateinit var continueTextColorPicker: View
    private lateinit var continueBackgroundColorPicker: View

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

    // CONTINUE defaults
    private var continueTextColor: Int = Color.WHITE
    private var continueBackgroundColor: Int = Color.parseColor("#008577")

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

        // We'll add a preview for the continue button
        previewContinueButton = Button(requireContext()).apply {
            text = "Continue Button"
        }
        previewContainer.addView(previewContinueButton)

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

        // New slider references for continue
        sliderContinue = SeekBar(requireContext())
        tvContinueSizeValue = TextView(requireContext()).apply {
            text = "0"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // We place the continue slider below the response slider in the existing layout
        val sliderContainer = view.findViewById<LinearLayout>(R.id.previewContainer)
        sliderContainer.addView(TextView(requireContext()).apply {
            text = "Continue Size:"
            setTextColor(Color.BLACK)
            textSize = 14f
        })
        sliderContainer.addView(sliderContinue)
        sliderContainer.addView(tvContinueSizeValue)

        // Response margin/padding
        sliderResponseMargin = view.findViewById(R.id.sliderResponseMargin)
        tvResponseMarginValue = view.findViewById(R.id.tvResponseMarginValue)
        sliderResponsePaddingH = view.findViewById(R.id.sliderResponsePaddingH)
        tvResponsePaddingHValue = view.findViewById(R.id.tvResponsePaddingHValue)
        sliderResponsePaddingV = view.findViewById(R.id.sliderResponsePaddingV)
        tvResponsePaddingVValue = view.findViewById(R.id.tvResponsePaddingVValue)

        // We also add continue padding references
        sliderContinuePaddingH = SeekBar(requireContext())
        tvContinuePaddingHValue = TextView(requireContext()).apply {
            text = "0"
        }
        sliderContinuePaddingV = SeekBar(requireContext())
        tvContinuePaddingVValue = TextView(requireContext()).apply {
            text = "0"
        }

        sliderContainer.addView(TextView(requireContext()).apply {
            text = "Continue Padding (Left/Right):"
            setTextColor(Color.BLACK)
            textSize = 14f
        })
        sliderContainer.addView(sliderContinuePaddingH)
        sliderContainer.addView(tvContinuePaddingHValue)

        sliderContainer.addView(TextView(requireContext()).apply {
            text = "Continue Padding (Top/Bottom):"
            setTextColor(Color.BLACK)
            textSize = 14f
        })
        sliderContainer.addView(sliderContinuePaddingV)
        sliderContainer.addView(tvContinuePaddingVValue)

        // Color pickers
        headerColorPicker = view.findViewById(R.id.headerColorPicker)
        bodyColorPicker = view.findViewById(R.id.bodyColorPicker)
        buttonTextColorPicker = view.findViewById(R.id.buttonTextColorPicker)
        buttonBackgroundColorPicker = view.findViewById(R.id.buttonBackgroundColorPicker)
        itemColorPicker = view.findViewById(R.id.itemColorPicker)
        responseColorPicker = view.findViewById(R.id.responseColorPicker)
        screenBackgroundColorPicker = view.findViewById(R.id.screenBackgroundColorPicker)

        // We'll add two new color pickers for continue text & background, placed similarly
        continueTextColorPicker = View(requireContext())
        continueBackgroundColorPicker = View(requireContext())
        sliderContainer.addView(TextView(requireContext()).apply {
            text = "Continue Text Color:"
            setTextColor(Color.BLACK)
        })
        sliderContainer.addView(continueTextColorPicker, 100, 100)

        sliderContainer.addView(TextView(requireContext()).apply {
            text = "Continue Background Color:"
            setTextColor(Color.BLACK)
        })
        sliderContainer.addView(continueBackgroundColorPicker, 100, 100)

        // Extra preview container with two stacked buttons
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

        previewPaddingButton1 = Button(requireContext()).apply {
            text = "Response 1"
        }
        previewPaddingButton2 = Button(requireContext()).apply {
            text = "Response 2"
        }
        previewPaddingButtonContainer.addView(previewPaddingButton1)
        previewPaddingButtonContainer.addView(previewPaddingButton2)

        val ctx = requireContext()

        // Load stored sizes
        sliderHeader.progress = FontSizeManager.getHeaderSize(ctx).toInt().coerceIn(8, 100)
        sliderBody.progress = FontSizeManager.getBodySize(ctx).toInt().coerceIn(8, 100)
        sliderButton.progress = FontSizeManager.getButtonSize(ctx).toInt().coerceIn(8, 100)
        sliderItem.progress = FontSizeManager.getItemSize(ctx).toInt().coerceIn(8, 100)
        sliderResponse.progress = FontSizeManager.getResponseSize(ctx).toInt().coerceIn(8, 100)
        sliderContinue.progress = FontSizeManager.getContinueSize(ctx).toInt().coerceIn(8, 100)

        tvHeaderSizeValue.text = sliderHeader.progress.toString()
        tvBodySizeValue.text = sliderBody.progress.toString()
        tvButtonSizeValue.text = sliderButton.progress.toString()
        tvItemSizeValue.text = sliderItem.progress.toString()
        tvResponseSizeValue.text = sliderResponse.progress.toString()
        tvContinueSizeValue.text = sliderContinue.progress.toString()

        // Apply to preview
        previewHeaderTextView.textSize = sliderHeader.progress.toFloat()
        previewBodyTextView.textSize = sliderBody.progress.toFloat()
        previewButton.textSize = sliderButton.progress.toFloat()
        previewItemTextView.textSize = sliderItem.progress.toFloat()
        previewResponseButton.textSize = sliderResponse.progress.toFloat()
        previewPaddingButton1.textSize = sliderResponse.progress.toFloat()
        previewPaddingButton2.textSize = sliderResponse.progress.toFloat()
        previewContinueButton.textSize = sliderContinue.progress.toFloat()

        // Load colors
        headerTextColor = ColorManager.getHeaderTextColor(ctx)
        bodyTextColor = ColorManager.getBodyTextColor(ctx)
        buttonTextColor = ColorManager.getButtonTextColor(ctx)
        buttonBackgroundColor = ColorManager.getButtonBackgroundColor(ctx)
        itemTextColor = ColorManager.getItemTextColor(ctx)
        responseTextColor = ColorManager.getResponseTextColor(ctx)
        screenBgColor = ColorManager.getScreenBackgroundColor(ctx)
        continueTextColor = ColorManager.getContinueTextColor(ctx)
        continueBackgroundColor = ColorManager.getContinueBackgroundColor(ctx)

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

        applyColorPickerBoxColor(headerColorPicker, headerTextColor)
        applyColorPickerBoxColor(bodyColorPicker, bodyTextColor)
        applyColorPickerBoxColor(buttonTextColorPicker, buttonTextColor)
        applyColorPickerBoxColor(buttonBackgroundColorPicker, buttonBackgroundColor)
        applyColorPickerBoxColor(itemColorPicker, itemTextColor)
        applyColorPickerBoxColor(responseColorPicker, responseTextColor)
        applyColorPickerBoxColor(screenBackgroundColorPicker, screenBgColor)
        applyColorPickerBoxColor(continueTextColorPicker, continueTextColor)
        applyColorPickerBoxColor(continueBackgroundColorPicker, continueBackgroundColor)

        // Set up spacing & padding for response
        val currentMarginDp = SpacingManager.getResponseButtonMargin(ctx).toInt().coerceIn(0, 100)
        sliderResponseMargin.max = 100
        sliderResponseMargin.progress = currentMarginDp
        tvResponseMarginValue.text = currentMarginDp.toString()
        applyResponseButtonMargin(currentMarginDp)

        val currentPaddingH = SpacingManager.getResponseButtonPaddingHorizontal(ctx).toInt().coerceIn(0, 100)
        sliderResponsePaddingH.max = 100
        sliderResponsePaddingH.progress = currentPaddingH
        tvResponsePaddingHValue.text = currentPaddingH.toString()

        val currentPaddingV = SpacingManager.getResponseButtonPaddingVertical(ctx).toInt().coerceIn(0, 100)
        sliderResponsePaddingV.max = 100
        sliderResponsePaddingV.progress = currentPaddingV
        tvResponsePaddingVValue.text = currentPaddingV.toString()
        applyResponseButtonPadding()

        // Set up padding for continue
        sliderContinuePaddingH.max = 100
        sliderContinuePaddingH.progress =
            SpacingManager.getContinueButtonPaddingHorizontal(ctx).toInt().coerceIn(0, 100)
        tvContinuePaddingHValue.text = sliderContinuePaddingH.progress.toString()

        sliderContinuePaddingV.max = 100
        sliderContinuePaddingV.progress =
            SpacingManager.getContinueButtonPaddingVertical(ctx).toInt().coerceIn(0, 100)
        tvContinuePaddingVValue.text = sliderContinuePaddingV.progress.toString()

        applyContinueButtonPadding()

        // Setup size sliders
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
        sliderContinue.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            FontSizeManager.setContinueSize(ctx, size.toFloat())
            previewContinueButton.textSize = size.toFloat()
            tvContinueSizeValue.text = size.toString()
        })

        // Response margin & padding
        sliderResponseMargin.setOnSeekBarChangeListener(simpleSeekBarListener {
            val marginDp = it.coerceIn(0, 100)
            SpacingManager.setResponseButtonMargin(ctx, marginDp.toFloat())
            tvResponseMarginValue.text = marginDp.toString()
            applyResponseButtonMargin(marginDp)
        })
        sliderResponsePaddingH.setOnSeekBarChangeListener(simpleSeekBarListener {
            val ph = it.coerceIn(0, 100)
            SpacingManager.setResponseButtonPaddingHorizontal(ctx, ph.toFloat())
            tvResponsePaddingHValue.text = ph.toString()
            applyResponseButtonPadding()
        })
        sliderResponsePaddingV.setOnSeekBarChangeListener(simpleSeekBarListener {
            val pv = it.coerceIn(0, 100)
            SpacingManager.setResponseButtonPaddingVertical(ctx, pv.toFloat())
            tvResponsePaddingVValue.text = pv.toString()
            applyResponseButtonPadding()
        })

        // Continue button padding
        sliderContinuePaddingH.setOnSeekBarChangeListener(simpleSeekBarListener {
            val ph = it.coerceIn(0, 100)
            SpacingManager.setContinueButtonPaddingHorizontal(ctx, ph.toFloat())
            tvContinuePaddingHValue.text = ph.toString()
            applyContinueButtonPadding()
        })
        sliderContinuePaddingV.setOnSeekBarChangeListener(simpleSeekBarListener {
            val pv = it.coerceIn(0, 100)
            SpacingManager.setContinueButtonPaddingVertical(ctx, pv.toFloat())
            tvContinuePaddingVValue.text = pv.toString()
            applyContinueButtonPadding()
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
        continueTextColorPicker.setOnClickListener {
            openColorPicker(continueTextColor) { chosenColor ->
                continueTextColor = chosenColor
                applyColorPickerBoxColor(continueTextColorPicker, chosenColor)
                ColorManager.setContinueTextColor(ctx, chosenColor)
                previewContinueButton.setTextColor(chosenColor)
            }
        }
        continueBackgroundColorPicker.setOnClickListener {
            openColorPicker(continueBackgroundColor) { chosenColor ->
                continueBackgroundColor = chosenColor
                applyColorPickerBoxColor(continueBackgroundColorPicker, chosenColor)
                ColorManager.setContinueBackgroundColor(ctx, chosenColor)
                previewContinueButton.setBackgroundColor(chosenColor)
            }
        }

        // Transitions Spinner
        transitionsSpinner = view.findViewById(R.id.spinnerTransitions)
        val transitionOptions = listOf("No transition", "Slide to left")
        val adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, transitionOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        transitionsSpinner.adapter = adapter

        val storedMode = TransitionManager.getTransitionMode(ctx)
        transitionsSpinner.setSelection(if (storedMode == "off") 0 else 1, false)
        transitionsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (position == 0) {
                    TransitionManager.setTransitionMode(ctx, "off")
                } else {
                    TransitionManager.setTransitionMode(ctx, "slide")
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

    private fun applyResponseButtonPadding() {
        val ctx = requireContext()
        val scale = resources.displayMetrics.density

        val horizontalDp = SpacingManager.getResponseButtonPaddingHorizontal(ctx)
        val verticalDp = SpacingManager.getResponseButtonPaddingVertical(ctx)
        val horizontalPx = (horizontalDp * scale + 0.5f).toInt()
        val verticalPx = (verticalDp * scale + 0.5f).toInt()

        fun updateButtonPadding(button: Button) {
            button.setPadding(horizontalPx, verticalPx, horizontalPx, verticalPx)
        }
        updateButtonPadding(previewPaddingButton1)
        updateButtonPadding(previewPaddingButton2)
        previewResponseButton.setPadding(horizontalPx, verticalPx, horizontalPx, verticalPx)
    }

    private fun applyContinueButtonPadding() {
        val ctx = requireContext()
        val scale = resources.displayMetrics.density
        val horizontalDp = SpacingManager.getContinueButtonPaddingHorizontal(ctx)
        val verticalDp = SpacingManager.getContinueButtonPaddingVertical(ctx)

        val horizontalPx = (horizontalDp * scale + 0.5f).toInt()
        val verticalPx = (verticalDp * scale + 0.5f).toInt()

        previewContinueButton.setPadding(horizontalPx, verticalPx, horizontalPx, verticalPx)
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
        val defaultContinueSize = 18f

        val defaultHeaderColor = Color.parseColor("#37474F")
        val defaultBodyColor = Color.parseColor("#616161")
        val defaultButtonTextColor = Color.WHITE
        val defaultButtonBgColor = Color.parseColor("#008577")
        val defaultItemColor = Color.parseColor("#008577")
        val defaultResponseColor = Color.parseColor("#C51162")
        val defaultScreenBgColor = Color.parseColor("#F5F5F5")
        val defaultContinueTextColor = Color.WHITE
        val defaultContinueBgColor = Color.parseColor("#008577")

        FontSizeManager.setHeaderSize(ctx, defaultHeaderSize)
        FontSizeManager.setBodySize(ctx, defaultBodySize)
        FontSizeManager.setButtonSize(ctx, defaultButtonSize)
        FontSizeManager.setItemSize(ctx, defaultItemSize)
        FontSizeManager.setResponseSize(ctx, defaultResponseSize)
        FontSizeManager.setContinueSize(ctx, defaultContinueSize)

        sliderHeader.progress = defaultHeaderSize.toInt()
        sliderBody.progress = defaultBodySize.toInt()
        sliderButton.progress = defaultButtonSize.toInt()
        sliderItem.progress = defaultItemSize.toInt()
        sliderResponse.progress = defaultResponseSize.toInt()
        sliderContinue.progress = defaultContinueSize.toInt()

        tvHeaderSizeValue.text = sliderHeader.progress.toString()
        tvBodySizeValue.text = sliderBody.progress.toString()
        tvButtonSizeValue.text = sliderButton.progress.toString()
        tvItemSizeValue.text = sliderItem.progress.toString()
        tvResponseSizeValue.text = sliderResponse.progress.toString()
        tvContinueSizeValue.text = sliderContinue.progress.toString()

        previewHeaderTextView.textSize = defaultHeaderSize
        previewBodyTextView.textSize = defaultBodySize
        previewButton.textSize = defaultButtonSize
        previewItemTextView.textSize = defaultItemSize
        previewResponseButton.textSize = defaultResponseSize
        previewContinueButton.textSize = defaultContinueSize

        ColorManager.setHeaderTextColor(ctx, defaultHeaderColor)
        ColorManager.setBodyTextColor(ctx, defaultBodyColor)
        ColorManager.setButtonTextColor(ctx, defaultButtonTextColor)
        ColorManager.setButtonBackgroundColor(ctx, defaultButtonBgColor)
        ColorManager.setItemTextColor(ctx, defaultItemColor)
        ColorManager.setResponseTextColor(ctx, defaultResponseColor)
        ColorManager.setScreenBackgroundColor(ctx, defaultScreenBgColor)
        ColorManager.setContinueTextColor(ctx, defaultContinueTextColor)
        ColorManager.setContinueBackgroundColor(ctx, defaultContinueBgColor)

        headerTextColor = defaultHeaderColor
        bodyTextColor = defaultBodyColor
        buttonTextColor = defaultButtonTextColor
        buttonBackgroundColor = defaultButtonBgColor
        itemTextColor = defaultItemColor
        responseTextColor = defaultResponseColor
        screenBgColor = defaultScreenBgColor
        continueTextColor = defaultContinueTextColor
        continueBackgroundColor = defaultContinueBgColor

        applyColorPickerBoxColor(headerColorPicker, defaultHeaderColor)
        applyColorPickerBoxColor(bodyColorPicker, defaultBodyColor)
        applyColorPickerBoxColor(buttonTextColorPicker, defaultButtonTextColor)
        applyColorPickerBoxColor(buttonBackgroundColorPicker, defaultButtonBgColor)
        applyColorPickerBoxColor(itemColorPicker, defaultItemColor)
        applyColorPickerBoxColor(responseColorPicker, defaultResponseColor)
        applyColorPickerBoxColor(screenBackgroundColorPicker, defaultScreenBgColor)
        applyColorPickerBoxColor(continueTextColorPicker, defaultContinueTextColor)
        applyColorPickerBoxColor(continueBackgroundColorPicker, defaultContinueBgColor)

        previewHeaderTextView.setTextColor(defaultHeaderColor)
        previewBodyTextView.setTextColor(defaultBodyColor)
        previewButton.setTextColor(defaultButtonTextColor)
        previewButton.setBackgroundColor(defaultButtonBgColor)
        previewItemTextView.setTextColor(defaultItemColor)
        previewResponseButton.setTextColor(defaultResponseColor)
        previewResponseButton.setBackgroundColor(defaultButtonBgColor)
        previewContinueButton.setTextColor(defaultContinueTextColor)
        previewContinueButton.setBackgroundColor(defaultContinueBgColor)
        previewContainer.setBackgroundColor(defaultScreenBgColor)

        // Reset margins/padding
        SpacingManager.setResponseButtonMargin(ctx, 0f)
        sliderResponseMargin.progress = 0
        tvResponseMarginValue.text = "0"
        applyResponseButtonMargin(0)

        SpacingManager.setResponseButtonPaddingHorizontal(ctx, 0f)
        sliderResponsePaddingH.progress = 0
        tvResponsePaddingHValue.text = "0"

        SpacingManager.setResponseButtonPaddingVertical(ctx, 0f)
        sliderResponsePaddingV.progress = 0
        tvResponsePaddingVValue.text = "0"
        applyResponseButtonPadding()

        SpacingManager.setContinueButtonPaddingHorizontal(ctx, 0f)
        sliderContinuePaddingH.progress = 0
        tvContinuePaddingHValue.text = "0"

        SpacingManager.setContinueButtonPaddingVertical(ctx, 0f)
        sliderContinuePaddingV.progress = 0
        tvContinuePaddingVValue.text = "0"
        applyContinueButtonPadding()
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
