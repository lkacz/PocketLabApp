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

class AppearanceCustomizationDialog : DialogFragment() {

    // Top preview elements
    private lateinit var previewHeaderTextView: TextView
    private lateinit var previewBodyTextView: TextView
    private lateinit var previewContinueButton: Button

    // Additional preview elements (Scale Item, Response Buttons)
    private lateinit var previewItemTextView: TextView
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

    // Slider for space between response buttons
    private lateinit var sliderSpaceBetweenBtns: SeekBar
    private lateinit var tvSpaceBetweenBtnsValue: TextView

    // Sliders for response padding
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

    // Spinner for transitions
    private lateinit var spinnerTransitions: Spinner

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
    ): View {
        // =============== 1) Create ScrollView (root) ===============
        val rootScrollView = ScrollView(requireContext()).apply {
            isFillViewport = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // =============== 2) Create the main LinearLayout container ===============
        val mainLinearLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        rootScrollView.addView(mainLinearLayout)

        // =============== 3) "previewContainerTop" linear layout ===============
        previewContainerTop = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        mainLinearLayout.addView(previewContainerTop)

        // 3a) Header preview
        val headerLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(8)
        }
        previewHeaderTextView = TextView(requireContext()).apply {
            text = "Header"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = headerLayoutParams
        }
        previewContainerTop.addView(previewHeaderTextView)

        // 3b) Body preview
        val bodyLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(8)
        }
        previewBodyTextView = TextView(requireContext()).apply {
            text =
                "Body text, e.g. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = bodyLayoutParams
        }
        previewContainerTop.addView(previewBodyTextView)

        // 3c) Continue button preview
        val continueBtnLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(8)
            gravity = Gravity.END
        }
        previewContinueButton = Button(requireContext()).apply {
            text = "Continue Button"
            textSize = 16f
            layoutParams = continueBtnLp
        }
        previewContainerTop.addView(previewContinueButton)

        // =============== 4) Sliders/pickers for Header ===============
        val headerRow = newRowLayout()
        mainLinearLayout.addView(headerRow)

        val headerLabel = TextView(requireContext()).apply {
            text = "Header:"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        headerRow.addView(headerLabel)

        sliderHeader = SeekBar(requireContext()).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(4)
            }
        }
        headerRow.addView(sliderHeader)

        tvHeaderSizeValue = TextView(requireContext()).apply {
            text = "24"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(4)
            }
        }
        headerRow.addView(tvHeaderSizeValue)

        headerColorPicker = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                leftMargin = dpToPx(8)
            }
        }
        headerRow.addView(headerColorPicker)

        // =============== 5) Body row ===============
        val bodyRow = newRowLayout().apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
        }
        mainLinearLayout.addView(bodyRow)

        val bodyLabel = TextView(requireContext()).apply {
            text = "Body:"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        bodyRow.addView(bodyLabel)

        sliderBody = SeekBar(requireContext()).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(4)
            }
        }
        bodyRow.addView(sliderBody)

        tvBodySizeValue = TextView(requireContext()).apply {
            text = "16"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(4)
            }
        }
        bodyRow.addView(tvBodySizeValue)

        bodyColorPicker = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                leftMargin = dpToPx(8)
            }
        }
        bodyRow.addView(bodyColorPicker)

        // =============== 6) Continue BTN slider row ===============
        val continueRow = newRowLayout().apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
        }
        mainLinearLayout.addView(continueRow)

        val continueLabel = TextView(requireContext()).apply {
            text = "Continue BTN:"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        continueRow.addView(continueLabel)

        sliderContinue = SeekBar(requireContext()).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(4)
            }
        }
        continueRow.addView(sliderContinue)

        tvContinueSizeValue = TextView(requireContext()).apply {
            text = "18"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(4)
            }
        }
        continueRow.addView(tvContinueSizeValue)

        continueTextColorPicker = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                leftMargin = dpToPx(8)
            }
        }
        continueRow.addView(continueTextColorPicker)

        continueBackgroundColorPicker = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                leftMargin = dpToPx(8)
            }
        }
        continueRow.addView(continueBackgroundColorPicker)

        // =============== 7) Continue BTN Padding (Left/Right) ===============
        val continuePaddingHRow = newRowLayout().apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
        }
        mainLinearLayout.addView(continuePaddingHRow)

        val paddingHLabel = TextView(requireContext()).apply {
            text = "Continue Padding (L/R):"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        continuePaddingHRow.addView(paddingHLabel)

        sliderContinuePaddingH = SeekBar(requireContext()).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(4)
            }
        }
        continuePaddingHRow.addView(sliderContinuePaddingH)

        tvContinuePaddingHValue = TextView(requireContext()).apply {
            text = "0"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(4)
            }
        }
        continuePaddingHRow.addView(tvContinuePaddingHValue)

        // =============== 8) Continue BTN Padding (Top/Bottom) ===============
        val continuePaddingVRow = newRowLayout().apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
        }
        mainLinearLayout.addView(continuePaddingVRow)

        val paddingVLabel = TextView(requireContext()).apply {
            text = "Continue Padding (T/B):"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        continuePaddingVRow.addView(paddingVLabel)

        sliderContinuePaddingV = SeekBar(requireContext()).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(4)
            }
        }
        continuePaddingVRow.addView(sliderContinuePaddingV)

        tvContinuePaddingVValue = TextView(requireContext()).apply {
            text = "0"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(4)
            }
        }
        continuePaddingVRow.addView(tvContinuePaddingVValue)

        // Spacer
        mainLinearLayout.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16)
            ).apply {
                topMargin = dpToPx(8)
            }
        })

        // =============== 9) "previewContainerScaleAndResponse" ===============
        previewContainerScaleAndResponse = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        mainLinearLayout.addView(previewContainerScaleAndResponse)

        // 9a) previewItemTextView
        val itemTvLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(8)
        }
        previewItemTextView = TextView(requireContext()).apply {
            text = "SCALE ITEM"
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = itemTvLp
        }
        previewContainerScaleAndResponse.addView(previewItemTextView)

        // 9b) Container with multiple response buttons
        previewPaddingButtonContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        previewContainerScaleAndResponse.addView(previewPaddingButtonContainer)

        val paddingBtn1Lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(4)
        }
        previewPaddingButton1 = Button(requireContext()).apply {
            text = "Response 1"
            layoutParams = paddingBtn1Lp
        }
        previewPaddingButtonContainer.addView(previewPaddingButton1)

        previewPaddingButton2 = Button(requireContext()).apply {
            text = "Response 2"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        previewPaddingButtonContainer.addView(previewPaddingButton2)

        // =============== 10) Slider for Item size ===============
        val itemRow = newRowLayout().apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
        }
        mainLinearLayout.addView(itemRow)

        val itemLabel = TextView(requireContext()).apply {
            text = "Item:"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        itemRow.addView(itemLabel)

        sliderItem = SeekBar(requireContext()).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(4)
            }
        }
        itemRow.addView(sliderItem)

        tvItemSizeValue = TextView(requireContext()).apply {
            text = "16"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(4)
            }
        }
        itemRow.addView(tvItemSizeValue)

        itemColorPicker = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                leftMargin = dpToPx(8)
            }
        }
        itemRow.addView(itemColorPicker)

        // =============== 11) Slider for Response size + color pickers ===============
        val responseRow = newRowLayout().apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
        }
        mainLinearLayout.addView(responseRow)

        val responseLabel = TextView(requireContext()).apply {
            text = "Response:"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        responseRow.addView(responseLabel)

        sliderResponse = SeekBar(requireContext()).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(4)
            }
        }
        responseRow.addView(sliderResponse)

        tvResponseSizeValue = TextView(requireContext()).apply {
            text = "16"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(4)
            }
        }
        responseRow.addView(tvResponseSizeValue)

        responseColorPicker = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                leftMargin = dpToPx(8)
            }
        }
        responseRow.addView(responseColorPicker)

        buttonTextColorPicker = View(requireContext()).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                leftMargin = dpToPx(8)
            }
        }
        responseRow.addView(buttonTextColorPicker)

        buttonBackgroundColorPicker = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                leftMargin = dpToPx(8)
            }
        }
        responseRow.addView(buttonBackgroundColorPicker)

        // =============== 12) Space Between Btns slider ===============
        val spaceBetweenBtnsRow = newRowLayout().apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
        }
        mainLinearLayout.addView(spaceBetweenBtnsRow)

        val spaceBetweenBtnsLabel = TextView(requireContext()).apply {
            text = "Space Between Btns:"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        spaceBetweenBtnsRow.addView(spaceBetweenBtnsLabel)

        sliderSpaceBetweenBtns = SeekBar(requireContext()).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(4)
            }
        }
        spaceBetweenBtnsRow.addView(sliderSpaceBetweenBtns)

        tvSpaceBetweenBtnsValue = TextView(requireContext()).apply {
            text = "0"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(4)
            }
        }
        spaceBetweenBtnsRow.addView(tvSpaceBetweenBtnsValue)

        // =============== 13) Response Padding (Left/Right) ===============
        val responsePaddingHRow = newRowLayout().apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
        }
        mainLinearLayout.addView(responsePaddingHRow)

        val responsePaddingHLabel = TextView(requireContext()).apply {
            text = "Padding (Left/Right):"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        responsePaddingHRow.addView(responsePaddingHLabel)

        sliderResponsePaddingH = SeekBar(requireContext()).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(4)
            }
        }
        responsePaddingHRow.addView(sliderResponsePaddingH)

        tvResponsePaddingHValue = TextView(requireContext()).apply {
            text = "0"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(4)
            }
        }
        responsePaddingHRow.addView(tvResponsePaddingHValue)

        // =============== 14) Response Padding (Top/Bottom) ===============
        val responsePaddingVRow = newRowLayout().apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
        }
        mainLinearLayout.addView(responsePaddingVRow)

        val responsePaddingVLabel = TextView(requireContext()).apply {
            text = "Padding (Top/Bottom):"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        responsePaddingVRow.addView(responsePaddingVLabel)

        sliderResponsePaddingV = SeekBar(requireContext()).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(4)
            }
        }
        responsePaddingVRow.addView(sliderResponsePaddingV)

        tvResponsePaddingVValue = TextView(requireContext()).apply {
            text = "0"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(4)
            }
        }
        responsePaddingVRow.addView(tvResponsePaddingVValue)

        // Spacer
        mainLinearLayout.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16)
            ).apply {
                topMargin = dpToPx(8)
            }
        })

        // =============== 15) Background color picker row ===============
        val bgRow = newRowLayout().apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(4)
        }
        mainLinearLayout.addView(bgRow)

        val bgLabel = TextView(requireContext()).apply {
            text = "Background:"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        bgRow.addView(bgLabel)

        screenBackgroundColorPicker = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                leftMargin = dpToPx(8)
            }
        }
        bgRow.addView(screenBackgroundColorPicker)

        // Spacer
        mainLinearLayout.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16)
            ).apply {
                topMargin = dpToPx(8)
            }
        })

        // =============== 17) Transitions Spinner ===============
        val transitionsLabel = TextView(requireContext()).apply {
            text = "Slides transition style:"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(16)
                topMargin = dpToPx(16)
            }
        }
        mainLinearLayout.addView(transitionsLabel)

        spinnerTransitions = Spinner(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(16)
                bottomMargin = dpToPx(16)
            }
        }
        mainLinearLayout.addView(spinnerTransitions)

        // =============== 18) Bottom row: OK | Defaults | Cancel ===============
        val bottomButtonRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
        }
        mainLinearLayout.addView(bottomButtonRow)

        val btnOk = Button(requireContext()).apply {
            text = "OK"
        }
        bottomButtonRow.addView(btnOk)

        val btnDefaults = Button(requireContext()).apply {
            text = "Defaults"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(16)
            }
        }
        bottomButtonRow.addView(btnDefaults)

        val btnCancel = Button(requireContext()).apply {
            text = "Cancel"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(16)
            }
        }
        bottomButtonRow.addView(btnCancel)

        btnOk.setOnClickListener {
            saveCurrentSelections()
            dismiss()
        }
        btnDefaults.setOnClickListener { restoreAllDefaults() }
        btnCancel.setOnClickListener { dismiss() }

        return rootScrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view as ScrollView).setBackgroundColor(screenBgColor)

        // Initialize slider values
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

        previewHeaderTextView.textSize = sliderHeader.progress.toFloat()
        previewBodyTextView.textSize = sliderBody.progress.toFloat()
        previewContinueButton.textSize = sliderContinue.progress.toFloat()
        previewItemTextView.textSize = sliderItem.progress.toFloat()
        previewPaddingButton1.textSize = sliderResponse.progress.toFloat()
        previewPaddingButton2.textSize = sliderResponse.progress.toFloat()

        // Load stored colors
        previewHeaderTextView.setTextColor(headerTextColor)
        previewBodyTextView.setTextColor(bodyTextColor)
        previewContinueButton.setTextColor(continueTextColor)
        previewContinueButton.setBackgroundColor(continueBackgroundColor)
        previewItemTextView.setTextColor(itemTextColor)
        previewPaddingButton1.setTextColor(responseTextColor)
        previewPaddingButton1.setBackgroundColor(buttonBackgroundColorVar)
        previewPaddingButton2.setTextColor(responseTextColor)
        previewPaddingButton2.setBackgroundColor(buttonBackgroundColorVar)

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

        // Initialize space between buttons
        sliderSpaceBetweenBtns.max = 100
        sliderSpaceBetweenBtns.progress = 0
        tvSpaceBetweenBtnsValue.text = "0"
        applySpaceBetweenButtons(0)

        // Initialize response padding
        sliderResponsePaddingH.max = 100
        sliderResponsePaddingH.progress = 0
        tvResponsePaddingHValue.text = "0"
        sliderResponsePaddingV.max = 100
        sliderResponsePaddingV.progress = 0
        tvResponsePaddingVValue.text = "0"
        applyResponseButtonPadding()

        // Initialize continue button padding
        sliderContinuePaddingH.max = 100
        sliderContinuePaddingH.progress = 0
        tvContinuePaddingHValue.text = "0"
        sliderContinuePaddingV.max = 100
        sliderContinuePaddingV.progress = 0
        tvContinuePaddingVValue.text = "0"
        applyContinueButtonPadding()

        // Slider listeners
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
            previewPaddingButton1.textSize = size.toFloat()
            previewPaddingButton2.textSize = size.toFloat()
            tvResponseSizeValue.text = size.toString()
        })

        sliderSpaceBetweenBtns.setOnSeekBarChangeListener(simpleSeekBarListener {
            val spaceDp = it.coerceIn(0, 100)
            tvSpaceBetweenBtnsValue.text = spaceDp.toString()
            applySpaceBetweenButtons(spaceDp)
        })
        sliderResponsePaddingH.setOnSeekBarChangeListener(simpleSeekBarListener {
            tvResponsePaddingHValue.text = it.toString()
            applyResponseButtonPadding()
        })
        sliderResponsePaddingV.setOnSeekBarChangeListener(simpleSeekBarListener {
            tvResponsePaddingVValue.text = it.toString()
            applyResponseButtonPadding()
        })
        sliderContinuePaddingH.setOnSeekBarChangeListener(simpleSeekBarListener {
            tvContinuePaddingHValue.text = it.toString()
            applyContinueButtonPadding()
        })
        sliderContinuePaddingV.setOnSeekBarChangeListener(simpleSeekBarListener {
            tvContinuePaddingVValue.text = it.toString()
            applyContinueButtonPadding()
        })

        // Spinner transitions
        val transitionOptions = listOf("No transition", "Slide to left")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, transitionOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTransitions.adapter = adapter
        spinnerTransitions.setSelection(
            if (currentTransitionMode == "off") 0 else 1,
            false
        )
        spinnerTransitions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                currentTransitionMode = if (position == 0) "off" else "slide"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun newRowLayout(): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun applySpaceBetweenButtons(spaceDp: Int) {
        val scale = resources.displayMetrics.density
        val spacePx = (spaceDp * scale + 0.5f).toInt()

        val params1 = previewPaddingButton1.layoutParams as LinearLayout.LayoutParams
        params1.topMargin = 0
        params1.bottomMargin = spacePx
        params1.leftMargin = 0
        params1.rightMargin = 0
        previewPaddingButton1.layoutParams = params1

        val params2 = previewPaddingButton2.layoutParams as LinearLayout.LayoutParams
        params2.topMargin = spacePx
        params2.bottomMargin = 0
        params2.leftMargin = 0
        params2.rightMargin = 0
        previewPaddingButton2.layoutParams = params2
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
        picker.setOnClickListener {
            openColorPicker(color) { chosenColor ->
                when (picker) {
                    headerColorPicker -> {
                        headerTextColor = chosenColor
                        previewHeaderTextView.setTextColor(chosenColor)
                    }
                    bodyColorPicker -> {
                        bodyTextColor = chosenColor
                        previewBodyTextView.setTextColor(chosenColor)
                    }
                    continueTextColorPicker -> {
                        continueTextColor = chosenColor
                        previewContinueButton.setTextColor(chosenColor)
                    }
                    continueBackgroundColorPicker -> {
                        continueBackgroundColor = chosenColor
                        previewContinueButton.setBackgroundColor(chosenColor)
                    }
                    itemColorPicker -> {
                        itemTextColor = chosenColor
                        previewItemTextView.setTextColor(chosenColor)
                    }
                    responseColorPicker -> {
                        responseTextColor = chosenColor
                        previewPaddingButton1.setTextColor(chosenColor)
                        previewPaddingButton2.setTextColor(chosenColor)
                    }
                    buttonTextColorPicker -> {
                        buttonTextColorVar = chosenColor
                        previewPaddingButton1.setTextColor(chosenColor)
                        previewPaddingButton2.setTextColor(chosenColor)
                    }
                    buttonBackgroundColorPicker -> {
                        buttonBackgroundColorVar = chosenColor
                        previewPaddingButton1.setBackgroundColor(chosenColor)
                        previewPaddingButton2.setBackgroundColor(chosenColor)
                    }
                    screenBackgroundColorPicker -> {
                        screenBgColor = chosenColor
                        (view as? ScrollView)?.setBackgroundColor(chosenColor)
                    }
                }
                applyColorPickerBoxColor(picker, chosenColor)
            }
        }
    }

    private fun openColorPicker(initialColor: Int, onColorSelected: (Int) -> Unit) {
        val dialogLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleText = TextView(requireContext()).apply {
            text = "Pick a Color"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            textSize = 16f
        }
        dialogLayout.addView(titleText)

        // Red row
        val redRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
            }
        }
        dialogLayout.addView(redRow)

        val labelR = TextView(requireContext()).apply {
            text = "R:"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        redRow.addView(labelR)

        val seekRed = SeekBar(requireContext()).apply {
            max = 255
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(8)
            }
        }
        redRow.addView(seekRed)

        // Green row
        val greenRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
            }
        }
        dialogLayout.addView(greenRow)

        val labelG = TextView(requireContext()).apply {
            text = "G:"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        greenRow.addView(labelG)

        val seekGreen = SeekBar(requireContext()).apply {
            max = 255
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(8)
            }
        }
        greenRow.addView(seekGreen)

        // Blue row
        val blueRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
            }
        }
        dialogLayout.addView(blueRow)

        val labelB = TextView(requireContext()).apply {
            text = "B:"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        blueRow.addView(labelB)

        val seekBlue = SeekBar(requireContext()).apply {
            max = 255
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(8)
            }
        }
        blueRow.addView(seekBlue)

        // Color preview
        val colorPreview = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(80)
            ).apply {
                topMargin = dpToPx(12)
            }
        }
        dialogLayout.addView(colorPreview)

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
            .setView(dialogLayout)
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
        previewPaddingButton1.setTextColor(responseTextColor)
        previewPaddingButton1.setBackgroundColor(buttonBackgroundColorVar)
        previewPaddingButton2.setTextColor(responseTextColor)
        previewPaddingButton2.setBackgroundColor(buttonBackgroundColorVar)

        (view as? ScrollView)?.setBackgroundColor(screenBgColor)

        sliderSpaceBetweenBtns.progress = 0
        tvSpaceBetweenBtnsValue.text = "0"
        applySpaceBetweenButtons(0)

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

    private fun saveCurrentSelections() {
        // Save final changes to managers
        // Colors
        ColorManager.setHeaderTextColor(requireContext(), headerTextColor)
        ColorManager.setBodyTextColor(requireContext(), bodyTextColor)
        ColorManager.setItemTextColor(requireContext(), itemTextColor)
        ColorManager.setResponseTextColor(requireContext(), responseTextColor)
        ColorManager.setButtonBackgroundColor(requireContext(), buttonBackgroundColorVar)
        ColorManager.setButtonTextColor(requireContext(), buttonTextColorVar)
        ColorManager.setContinueTextColor(requireContext(), continueTextColor)
        ColorManager.setContinueBackgroundColor(requireContext(), continueBackgroundColor)
        ColorManager.setScreenBackgroundColor(requireContext(), screenBgColor)

        // Font sizes
        FontSizeManager.setHeaderSize(requireContext(), headerTextSize)
        FontSizeManager.setBodySize(requireContext(), bodyTextSize)
        FontSizeManager.setItemSize(requireContext(), itemTextSize)
        FontSizeManager.setResponseSize(requireContext(), responseTextSize)
        FontSizeManager.setContinueSize(requireContext(), continueTextSize)

        // Spacing
        SpacingManager.setResponseButtonMargin(requireContext(), 0f)
        SpacingManager.setResponseButtonPaddingHorizontal(requireContext(), sliderResponsePaddingH.progress.toFloat())
        SpacingManager.setResponseButtonPaddingVertical(requireContext(), sliderResponsePaddingV.progress.toFloat())
        SpacingManager.setContinueButtonPaddingHorizontal(requireContext(), sliderContinuePaddingH.progress.toFloat())
        SpacingManager.setContinueButtonPaddingVertical(requireContext(), sliderContinuePaddingV.progress.toFloat())

        // Transitions
        TransitionManager.setTransitionMode(requireContext(), if (currentTransitionMode == "off") "off" else "slide")
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
