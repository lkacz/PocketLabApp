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
 * Revised layout places the top previews (Header, Body, Continue) at the top, followed by
 * sliders and additional previews for Scale Item and Response Buttons, then transitions,
 * and finally the bottom row of OK / Defaults / Cancel.
 */
class AppearanceCustomizationDialog : DialogFragment() {

    // Top preview elements
    private lateinit var previewHeaderTextView: TextView
    private lateinit var previewBodyTextView: TextView
    private lateinit var previewContinueButton: Button

    // Additional preview elements (Scale Item, Response Buttons)
    private lateinit var previewItemTextView: TextView
    private lateinit var previewResponseButton: Button
    private lateinit var previewPaddingButtonContainer: LinearLayout
    private lateinit var previewPaddingButton1: Button
    private lateinit var previewPaddingButton2: Button

    // Container(s)
    private lateinit var previewContainerTop: LinearLayout
    private lateinit var previewContainerScaleAndResponse: LinearLayout

    // Sliders for font sizes
    private lateinit var sliderHeader: SeekBar
    private lateinit var tvHeaderSizeValue: TextView
    private lateinit var sliderBody: SeekBar
    private lateinit var tvBodySizeValue: TextView
    private lateinit var sliderContinue: SeekBar
    private lateinit var tvContinueSizeValue: TextView
    private lateinit var sliderItem: SeekBar
    private lateinit var tvItemSizeValue: TextView
    private lateinit var sliderResponse: SeekBar
    private lateinit var tvResponseSizeValue: TextView

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
    private lateinit var continueTextColorPicker: View
    private lateinit var continueBackgroundColorPicker: View
    private lateinit var itemColorPicker: View
    private lateinit var responseColorPicker: View
    private lateinit var buttonTextColorPicker: View
    private lateinit var buttonBackgroundColorPicker: View
    private lateinit var screenBackgroundColorPicker: View

    // Timer sound placeholders
    private lateinit var timerSoundEditText: EditText

    // In-memory default values (replace with your own load/store logic)
    private var headerTextSize = 24f
    private var bodyTextSize = 16f
    private var continueTextSize = 18f
    private var itemTextSize = 16f
    private var responseTextSize = 16f

    private var headerTextColor = Color.BLACK
    private var bodyTextColor = Color.DKGRAY
    private var continueTextColor = Color.WHITE
    private var continueBackgroundColor = Color.parseColor("#008577")
    private var itemTextColor = Color.BLUE
    private var responseTextColor = Color.RED
    private var buttonTextColorVar = Color.WHITE
    private var buttonBackgroundColorVar = Color.BLUE
    private var screenBgColor = Color.WHITE

    // Transition mode
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

        // Top previews container
        previewContainerTop = view.findViewById(R.id.previewContainerTop)
        previewHeaderTextView = view.findViewById(R.id.previewHeaderTextView)
        previewBodyTextView = view.findViewById(R.id.previewBodyTextView)
        previewContinueButton = view.findViewById(R.id.previewContinueButton)

        // Additional previews (Scale Item, Response Buttons)
        previewContainerScaleAndResponse = view.findViewById(R.id.previewContainerScaleAndResponse)
        previewItemTextView = view.findViewById(R.id.previewItemTextView)
        previewResponseButton = view.findViewById(R.id.previewResponseButton)

        // Container for multiple sample response buttons
        previewPaddingButtonContainer = view.findViewById(R.id.previewPaddingButtonContainer)
        previewPaddingButton1 = view.findViewById(R.id.previewPaddingButton1)
        previewPaddingButton2 = view.findViewById(R.id.previewPaddingButton2)

        // Sliders from XML
        sliderHeader = view.findViewById(R.id.sliderHeaderSize)
        tvHeaderSizeValue = view.findViewById(R.id.tvHeaderSizeValue)
        sliderBody = view.findViewById(R.id.sliderBodySize)
        tvBodySizeValue = view.findViewById(R.id.tvBodySizeValue)
        sliderContinue = view.findViewById(R.id.sliderContinue)
        tvContinueSizeValue = view.findViewById(R.id.tvContinueSizeValue)
        sliderItem = view.findViewById(R.id.sliderItemSize)
        tvItemSizeValue = view.findViewById(R.id.tvItemSizeValue)
        sliderResponse = view.findViewById(R.id.sliderResponseSize)
        tvResponseSizeValue = view.findViewById(R.id.tvResponseSizeValue)

        sliderResponseMargin = view.findViewById(R.id.sliderResponseMargin)
        tvResponseMarginValue = view.findViewById(R.id.tvResponseMarginValue)
        sliderResponsePaddingH = view.findViewById(R.id.sliderResponsePaddingH)
        tvResponsePaddingHValue = view.findViewById(R.id.tvResponsePaddingHValue)
        sliderResponsePaddingV = view.findViewById(R.id.sliderResponsePaddingV)
        tvResponsePaddingVValue = view.findViewById(R.id.tvResponsePaddingVValue)

        sliderContinuePaddingH = view.findViewById(R.id.sliderContinuePaddingH)
        tvContinuePaddingHValue = view.findViewById(R.id.tvContinuePaddingHValue)
        sliderContinuePaddingV = view.findViewById(R.id.sliderContinuePaddingV)
        tvContinuePaddingVValue = view.findViewById(R.id.tvContinuePaddingVValue)

        // Color picker views
        headerColorPicker = view.findViewById(R.id.headerColorPicker)
        bodyColorPicker = view.findViewById(R.id.bodyColorPicker)
        continueTextColorPicker = view.findViewById(R.id.continueTextColorPicker)
        continueBackgroundColorPicker = view.findViewById(R.id.continueBackgroundColorPicker)
        itemColorPicker = view.findViewById(R.id.itemColorPicker)
        responseColorPicker = view.findViewById(R.id.responseColorPicker)
        buttonTextColorPicker = view.findViewById(R.id.buttonTextColorPicker)
        buttonBackgroundColorPicker = view.findViewById(R.id.buttonBackgroundColorPicker)
        screenBackgroundColorPicker = view.findViewById(R.id.screenBackgroundColorPicker)

        // Timer sound
        timerSoundEditText = view.findViewById(R.id.timerSoundEditText)

        // Initial slider progress
        sliderHeader.progress = headerTextSize.toInt().coerceIn(8, 100)
        sliderBody.progress = bodyTextSize.toInt().coerceIn(8, 100)
        sliderContinue.progress = continueTextSize.toInt().coerceIn(8, 100)
        sliderItem.progress = itemTextSize.toInt().coerceIn(8, 100)
        sliderResponse.progress = responseTextSize.toInt().coerceIn(8, 100)

        tvHeaderSizeValue.text = sliderHeader.progress.toString()
        tvBodySizeValue.text = sliderBody.progress.toString()
        tvContinueSizeValue.text = sliderContinue.progress.toString()
        tvItemSizeValue.text = sliderItem.progress.toString()
        tvResponseSizeValue.text = sliderResponse.progress.toString()

        // Apply to previews
        previewHeaderTextView.textSize = sliderHeader.progress.toFloat()
        previewBodyTextView.textSize = sliderBody.progress.toFloat()
        previewContinueButton.textSize = sliderContinue.progress.toFloat()
        previewItemTextView.textSize = sliderItem.progress.toFloat()
        previewResponseButton.textSize = sliderResponse.progress.toFloat()
        previewPaddingButton1.textSize = sliderResponse.progress.toFloat()
        previewPaddingButton2.textSize = sliderResponse.progress.toFloat()

        // Load stored colors
        previewHeaderTextView.setTextColor(headerTextColor)
        previewBodyTextView.setTextColor(bodyTextColor)
        previewContinueButton.setTextColor(continueTextColor)
        previewContinueButton.setBackgroundColor(continueBackgroundColor)
        previewItemTextView.setTextColor(itemTextColor)
        previewResponseButton.setTextColor(responseTextColor)
        previewResponseButton.setBackgroundColor(buttonBackgroundColorVar)
        previewPaddingButton1.setTextColor(responseTextColor)
        previewPaddingButton1.setBackgroundColor(buttonBackgroundColorVar)
        previewPaddingButton2.setTextColor(responseTextColor)
        previewPaddingButton2.setBackgroundColor(buttonBackgroundColorVar)

        // Main screen BG color
        val rootLayout = view.findViewById<ScrollView>(R.id.appearanceDialogRootScroll)
        rootLayout.setBackgroundColor(screenBgColor)

        // Color picker backgrounds
        applyColorPickerBoxColor(headerColorPicker, headerTextColor)
        applyColorPickerBoxColor(bodyColorPicker, bodyTextColor)
        applyColorPickerBoxColor(continueTextColorPicker, continueTextColor)
        applyColorPickerBoxColor(continueBackgroundColorPicker, continueBackgroundColor)
        applyColorPickerBoxColor(itemColorPicker, itemTextColor)
        applyColorPickerBoxColor(responseColorPicker, responseTextColor)
        applyColorPickerBoxColor(buttonTextColorPicker, buttonTextColorVar)
        applyColorPickerBoxColor(buttonBackgroundColorPicker, buttonBackgroundColorVar)
        applyColorPickerBoxColor(screenBackgroundColorPicker, screenBgColor)

        // Response margin/padding
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

        // Continue button padding
        sliderContinuePaddingH.max = 100
        sliderContinuePaddingH.progress = 0
        tvContinuePaddingHValue.text = "0"
        sliderContinuePaddingV.max = 100
        sliderContinuePaddingV.progress = 0
        tvContinuePaddingVValue.text = "0"
        applyContinueButtonPadding()

        // Setup slider listeners
        sliderHeader.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            headerTextSize = size.toFloat()
            previewHeaderTextView.textSize = size.toFloat()
            tvHeaderSizeValue.text = size.toString()
        })
        sliderBody.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            bodyTextSize = size.toFloat()
            previewBodyTextView.textSize = size.toFloat()
            tvBodySizeValue.text = size.toString()
        })
        sliderContinue.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            continueTextSize = size.toFloat()
            previewContinueButton.textSize = size.toFloat()
            tvContinueSizeValue.text = size.toString()
        })
        sliderItem.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            itemTextSize = size.toFloat()
            previewItemTextView.textSize = size.toFloat()
            tvItemSizeValue.text = size.toString()
        })
        sliderResponse.setOnSeekBarChangeListener(simpleSeekBarListener {
            val size = it.coerceIn(8, 100)
            responseTextSize = size.toFloat()
            previewResponseButton.textSize = size.toFloat()
            previewPaddingButton1.textSize = size.toFloat()
            previewPaddingButton2.textSize = size.toFloat()
            tvResponseSizeValue.text = size.toString()
        })

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

        // Color pickers
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
        buttonTextColorPicker.setOnClickListener {
            openColorPicker(buttonTextColorVar) { chosenColor ->
                buttonTextColorVar = chosenColor
                applyColorPickerBoxColor(buttonTextColorPicker, chosenColor)
                previewResponseButton.setTextColor(chosenColor)
                previewPaddingButton1.setTextColor(chosenColor)
                previewPaddingButton2.setTextColor(chosenColor)
            }
        }
        buttonBackgroundColorPicker.setOnClickListener {
            openColorPicker(buttonBackgroundColorVar) { chosenColor ->
                buttonBackgroundColorVar = chosenColor
                applyColorPickerBoxColor(buttonBackgroundColorPicker, chosenColor)
                previewResponseButton.setBackgroundColor(chosenColor)
                previewPaddingButton1.setBackgroundColor(chosenColor)
                previewPaddingButton2.setBackgroundColor(chosenColor)
            }
        }
        screenBackgroundColorPicker.setOnClickListener {
            openColorPicker(screenBgColor) { chosenColor ->
                screenBgColor = chosenColor
                applyColorPickerBoxColor(screenBackgroundColorPicker, chosenColor)
                rootLayout.setBackgroundColor(chosenColor)
            }
        }

        // Spinner for transitions
        val transitionsSpinner = view.findViewById<Spinner>(R.id.spinnerTransitions)
        val transitionOptions = listOf("No transition", "Slide to left")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, transitionOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        transitionsSpinner.adapter = adapter
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

        // Setup bottom row buttons
        val okButton = view.findViewById<Button>(R.id.btnOk)
        val defaultsButton = view.findViewById<Button>(R.id.btnDefaults)
        val cancelButton = view.findViewById<Button>(R.id.btnCancel)

        okButton.setOnClickListener { dismiss() }
        defaultsButton.setOnClickListener { restoreAllDefaults() }
        cancelButton.setOnClickListener { dismiss() }
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

    private fun applyContinueButtonPadding() {
        val scale = resources.displayMetrics.density
        val horizontalPx = (sliderContinuePaddingH.progress * scale + 0.5f).toInt()
        val verticalPx = (sliderContinuePaddingV.progress * scale + 0.5f).toInt()
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

    private fun restoreAllDefaults() {
        val defaultHeaderSize = 24f
        val defaultBodySize = 16f
        val defaultContinueSize = 18f
        val defaultItemSize = 16f
        val defaultResponseSize = 16f

        val defaultHeaderColor = Color.parseColor("#37474F")
        val defaultBodyColor = Color.parseColor("#616161")
        val defaultContinueTextColor = Color.WHITE
        val defaultContinueBgColor = Color.parseColor("#008577")
        val defaultItemColor = Color.parseColor("#008577")
        val defaultResponseColor = Color.parseColor("#C51162")
        val defaultScreenBgColor = Color.parseColor("#F5F5F5")

        val defaultButtonTextColor = Color.WHITE
        val defaultButtonBgColor = Color.parseColor("#008577")

        headerTextSize = defaultHeaderSize
        bodyTextSize = defaultBodySize
        continueTextSize = defaultContinueSize
        itemTextSize = defaultItemSize
        responseTextSize = defaultResponseSize

        headerTextColor = defaultHeaderColor
        bodyTextColor = defaultBodyColor
        continueTextColor = defaultContinueTextColor
        continueBackgroundColor = defaultContinueBgColor
        itemTextColor = defaultItemColor
        responseTextColor = defaultResponseColor
        buttonTextColorVar = defaultButtonTextColor
        buttonBackgroundColorVar = defaultButtonBgColor
        screenBgColor = defaultScreenBgColor

        sliderHeader.progress = headerTextSize.toInt()
        sliderBody.progress = bodyTextSize.toInt()
        sliderContinue.progress = continueTextSize.toInt()
        sliderItem.progress = itemTextSize.toInt()
        sliderResponse.progress = responseTextSize.toInt()

        tvHeaderSizeValue.text = sliderHeader.progress.toString()
        tvBodySizeValue.text = sliderBody.progress.toString()
        tvContinueSizeValue.text = sliderContinue.progress.toString()
        tvItemSizeValue.text = sliderItem.progress.toString()
        tvResponseSizeValue.text = sliderResponse.progress.toString()

        previewHeaderTextView.textSize = headerTextSize
        previewBodyTextView.textSize = bodyTextSize
        previewContinueButton.textSize = continueTextSize
        previewItemTextView.textSize = itemTextSize
        previewResponseButton.textSize = responseTextSize
        previewPaddingButton1.textSize = responseTextSize
        previewPaddingButton2.textSize = responseTextSize

        applyColorPickerBoxColor(headerColorPicker, headerTextColor)
        applyColorPickerBoxColor(bodyColorPicker, bodyTextColor)
        applyColorPickerBoxColor(continueTextColorPicker, continueTextColor)
        applyColorPickerBoxColor(continueBackgroundColorPicker, continueBackgroundColor)
        applyColorPickerBoxColor(itemColorPicker, itemTextColor)
        applyColorPickerBoxColor(responseColorPicker, responseTextColor)
        applyColorPickerBoxColor(buttonTextColorPicker, buttonTextColorVar)
        applyColorPickerBoxColor(buttonBackgroundColorPicker, buttonBackgroundColorVar)
        applyColorPickerBoxColor(screenBackgroundColorPicker, screenBgColor)

        previewHeaderTextView.setTextColor(headerTextColor)
        previewBodyTextView.setTextColor(bodyTextColor)
        previewContinueButton.setTextColor(continueTextColor)
        previewContinueButton.setBackgroundColor(continueBackgroundColor)
        previewItemTextView.setTextColor(itemTextColor)
        previewResponseButton.setTextColor(responseTextColor)
        previewResponseButton.setBackgroundColor(buttonBackgroundColorVar)
        previewPaddingButton1.setTextColor(responseTextColor)
        previewPaddingButton1.setBackgroundColor(buttonBackgroundColorVar)
        previewPaddingButton2.setTextColor(responseTextColor)
        previewPaddingButton2.setBackgroundColor(buttonBackgroundColorVar)

        val rootLayout = view?.findViewById<ScrollView>(R.id.appearanceDialogRootScroll)
        rootLayout?.setBackgroundColor(screenBgColor)

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

    private fun simpleSeekBarListener(onValueChanged: (Int) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onValueChanged(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
}
