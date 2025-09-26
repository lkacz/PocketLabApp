// Filename: AppearanceCustomizationDialog.kt
package com.lkacz.pola

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment

class AppearanceCustomizationDialog : DialogFragment() {
    private lateinit var previewHeaderTextView: TextView
    private lateinit var previewBodyTextView: TextView
    private lateinit var previewContinueButton: Button
    private lateinit var previewItemTextView: TextView
    private lateinit var previewPaddingButtonContainer: LinearLayout
    private lateinit var previewPaddingButton1: Button
    private lateinit var previewPaddingButton2: Button
    private lateinit var previewContainerTop: LinearLayout
    private lateinit var previewContainerScaleAndResponse: LinearLayout

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

    private lateinit var sliderResponseSpacing: SeekBar
    private lateinit var tvResponseSpacingValue: TextView
    private lateinit var sliderResponsePaddingH: SeekBar
    private lateinit var tvResponsePaddingHValue: TextView
    private lateinit var sliderResponsePaddingV: SeekBar
    private lateinit var tvResponsePaddingVValue: TextView
    private lateinit var sliderContinuePaddingH: SeekBar
    private lateinit var tvContinuePaddingHValue: TextView
    private lateinit var sliderContinuePaddingV: SeekBar
    private lateinit var tvContinuePaddingVValue: TextView

    private lateinit var previewTimerTextView: TextView
    private lateinit var sliderTimer: SeekBar
    private lateinit var tvTimerSizeValue: TextView
    private lateinit var timerColorPicker: View
    private lateinit var spinnerTimerAlignment: Spinner
    private lateinit var sliderTimerPaddingH: SeekBar
    private lateinit var tvTimerPaddingHValue: TextView
    private lateinit var sliderTimerPaddingV: SeekBar
    private lateinit var tvTimerPaddingVValue: TextView

    private lateinit var headerColorPicker: View
    private lateinit var bodyColorPicker: View
    private lateinit var continueTextColorPicker: View
    private lateinit var continueBackgroundColorPicker: View
    private lateinit var itemColorPicker: View
    private lateinit var responseColorPicker: View
    private lateinit var buttonTextColorPicker: View
    private lateinit var buttonBackgroundColorPicker: View
    private lateinit var screenBackgroundColorPicker: View
    private lateinit var spinnerTransitions: Spinner

    private lateinit var spinnerContinueAlignment: Spinner
    private lateinit var spinnerHeaderAlignment: Spinner
    private lateinit var spinnerBodyAlignment: Spinner

    private var headerTextSize = 24f
    private var bodyTextSize = 16f
    private var continueTextSize = 18f
    private var itemTextSize = 16f
    private var responseTextSize = 16f

    private var timerTextSize = 18f

    private var headerTextColor = Color.BLACK
    private var bodyTextColor = Color.DKGRAY
    private var continueTextColor = Color.WHITE
    private var continueBackgroundColor = Color.parseColor("#008577")
    private var itemTextColor = Color.BLUE
    private var responseTextColor = Color.RED
    private var buttonTextColorVar = Color.WHITE
    private var buttonBackgroundColorVar = Color.BLUE
    private var screenBgColor = Color.WHITE
    private var currentTransitionMode = "off"

    private var timerTextColorVar = Color.BLACK

    private var currentContinueAlignment = "CENTER"
    private var currentHeaderAlignment = "CENTER"
    private var currentBodyAlignment = "CENTER"
    private var currentTimerAlignment = "CENTER"

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
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootScrollView =
            ScrollView(requireContext()).apply {
                isFillViewport = true
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }

        val mainLinearLayout =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }
        rootScrollView.addView(mainLinearLayout)

        previewContainerTop =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        mainLinearLayout.addView(previewContainerTop)

        val headerLayoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dpToPx(8) }
        previewHeaderTextView =
            TextView(requireContext()).apply {
                text = "Header"
                textSize = 24f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = headerLayoutParams
            }
        previewContainerTop.addView(previewHeaderTextView)

        val bodyLayoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dpToPx(8) }
        previewBodyTextView =
            TextView(requireContext()).apply {
                text = "Body text, e.g. Lorem ipsum..."
                textSize = 16f
                gravity = Gravity.CENTER
                layoutParams = bodyLayoutParams
            }
        previewContainerTop.addView(previewBodyTextView)

        val continueBtnLp =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dpToPx(8)
                gravity = Gravity.END
            }
        previewContinueButton =
            Button(requireContext()).apply {
                text = "Continue Button"
                textSize = 16f
                layoutParams = continueBtnLp
            }
        previewContainerTop.addView(previewContinueButton)

        val timerLayoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dpToPx(8)
                bottomMargin = dpToPx(8)
            }
        previewTimerTextView =
            TextView(requireContext()).apply {
                text = "00:01"
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(0), 0, dpToPx(0))
                layoutParams = timerLayoutParams
            }
        previewContainerTop.addView(previewTimerTextView)

        val headerRow = newRowLayout()
        mainLinearLayout.addView(headerRow)
        val headerLabel =
            TextView(requireContext()).apply {
                text = "Header:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        headerRow.addView(headerLabel)
        sliderHeader =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(4)
                    }
            }
        headerRow.addView(sliderHeader)
        tvHeaderSizeValue =
            TextView(requireContext()).apply {
                text = "24"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        headerRow.addView(tvHeaderSizeValue)
        headerColorPicker =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                        leftMargin = dpToPx(8)
                    }
            }
        headerRow.addView(headerColorPicker)

        val bodyRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(bodyRow)
        val bodyLabel =
            TextView(requireContext()).apply {
                text = "Body:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        bodyRow.addView(bodyLabel)
        sliderBody =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(4)
                    }
            }
        bodyRow.addView(sliderBody)
        tvBodySizeValue =
            TextView(requireContext()).apply {
                text = "16"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        bodyRow.addView(tvBodySizeValue)
        bodyColorPicker =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                        leftMargin = dpToPx(8)
                    }
            }
        bodyRow.addView(bodyColorPicker)

        val continueRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(continueRow)
        val continueLabel =
            TextView(requireContext()).apply {
                text = "Continue BTN:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        continueRow.addView(continueLabel)
        sliderContinue =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(4)
                    }
            }
        continueRow.addView(sliderContinue)
        tvContinueSizeValue =
            TextView(requireContext()).apply {
                text = "18"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        continueRow.addView(tvContinueSizeValue)
        continueTextColorPicker =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                        leftMargin = dpToPx(8)
                    }
            }
        continueRow.addView(continueTextColorPicker)
        continueBackgroundColorPicker =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                        leftMargin = dpToPx(8)
                    }
            }
        continueRow.addView(continueBackgroundColorPicker)

        val continuePaddingHRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(continuePaddingHRow)
        val paddingHLabel =
            TextView(requireContext()).apply {
                text = "Continue Padding (L/R):"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        continuePaddingHRow.addView(paddingHLabel)
        sliderContinuePaddingH =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(4)
                    }
            }
        continuePaddingHRow.addView(sliderContinuePaddingH)
        tvContinuePaddingHValue =
            TextView(requireContext()).apply {
                text = "0"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        continuePaddingHRow.addView(tvContinuePaddingHValue)

        val continuePaddingVRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(continuePaddingVRow)
        val paddingVLabel =
            TextView(requireContext()).apply {
                text = "Continue Padding (T/B):"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        continuePaddingVRow.addView(paddingVLabel)
        sliderContinuePaddingV =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(4)
                    }
            }
        continuePaddingVRow.addView(sliderContinuePaddingV)
        tvContinuePaddingVValue =
            TextView(requireContext()).apply {
                text = "0"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        continuePaddingVRow.addView(tvContinuePaddingVValue)

        val headerAlignRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(headerAlignRow)
        val headerAlignLabel =
            TextView(requireContext()).apply {
                text = "Header Alignment:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        headerAlignRow.addView(headerAlignLabel)
        spinnerHeaderAlignment =
            Spinner(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(16)
                    }
            }
        headerAlignRow.addView(spinnerHeaderAlignment)

        val bodyAlignRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(bodyAlignRow)
        val bodyAlignLabel =
            TextView(requireContext()).apply {
                text = "Body Alignment:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        bodyAlignRow.addView(bodyAlignLabel)
        spinnerBodyAlignment =
            Spinner(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(16)
                    }
            }
        bodyAlignRow.addView(spinnerBodyAlignment)

        val continueAlignRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(continueAlignRow)
        val continueAlignLabel =
            TextView(requireContext()).apply {
                text = "Continue Alignment:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        continueAlignRow.addView(continueAlignLabel)
        spinnerContinueAlignment =
            Spinner(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(16)
                    }
            }
        continueAlignRow.addView(spinnerContinueAlignment)

        mainLinearLayout.addView(
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16),
                    ).apply { topMargin = dpToPx(8) }
            },
        )

        previewContainerScaleAndResponse =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), 0, dpToPx(16), 0)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        mainLinearLayout.addView(previewContainerScaleAndResponse)

        val itemTvLp =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dpToPx(8) }
        previewItemTextView =
            TextView(requireContext()).apply {
                text = "SCALE ITEM"
                gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = itemTvLp
            }
        previewContainerScaleAndResponse.addView(previewItemTextView)

        previewPaddingButtonContainer =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        previewContainerScaleAndResponse.addView(previewPaddingButtonContainer)

        val paddingBtn1Lp =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dpToPx(4) }
        previewPaddingButton1 =
            Button(requireContext()).apply {
                text = "Response 1"
                layoutParams = paddingBtn1Lp
            }
        previewPaddingButtonContainer.addView(previewPaddingButton1)

        previewPaddingButton2 =
            Button(requireContext()).apply {
                text = "Response 2"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        previewPaddingButtonContainer.addView(previewPaddingButton2)

        val itemRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(itemRow)
        val itemLabel =
            TextView(requireContext()).apply {
                text = "Item:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        itemRow.addView(itemLabel)
        sliderItem =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(4)
                    }
            }
        itemRow.addView(sliderItem)
        tvItemSizeValue =
            TextView(requireContext()).apply {
                text = "16"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        itemRow.addView(tvItemSizeValue)
        itemColorPicker =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                        leftMargin = dpToPx(8)
                    }
            }
        itemRow.addView(itemColorPicker)

        val responseRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(responseRow)
        val responseLabel =
            TextView(requireContext()).apply {
                text = "Response:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        responseRow.addView(responseLabel)
        sliderResponse =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(4)
                    }
            }
        responseRow.addView(sliderResponse)
        tvResponseSizeValue =
            TextView(requireContext()).apply {
                text = "16"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        responseRow.addView(tvResponseSizeValue)
        responseColorPicker =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                        leftMargin = dpToPx(8)
                    }
            }
        responseRow.addView(responseColorPicker)

        buttonTextColorPicker =
            View(requireContext()).apply {
                visibility = View.GONE
                layoutParams =
                    LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                        leftMargin = dpToPx(8)
                    }
            }
        responseRow.addView(buttonTextColorPicker)
        buttonBackgroundColorPicker =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                        leftMargin = dpToPx(8)
                    }
            }
        responseRow.addView(buttonBackgroundColorPicker)

        val responseSpacingRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(responseSpacingRow)
        val responseSpacingLabel =
            TextView(requireContext()).apply {
                text = "Response Spacing:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        responseSpacingRow.addView(responseSpacingLabel)
        sliderResponseSpacing =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(4)
                    }
            }
        responseSpacingRow.addView(sliderResponseSpacing)
        tvResponseSpacingValue =
            TextView(requireContext()).apply {
                text = "0"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        responseSpacingRow.addView(tvResponseSpacingValue)

        val responsePaddingHRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(responsePaddingHRow)
        val responsePaddingHLabel =
            TextView(requireContext()).apply {
                text = "Padding (Left/Right):"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        responsePaddingHRow.addView(responsePaddingHLabel)
        sliderResponsePaddingH =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(4)
                    }
            }
        responsePaddingHRow.addView(sliderResponsePaddingH)
        tvResponsePaddingHValue =
            TextView(requireContext()).apply {
                text = "0"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        responsePaddingHRow.addView(tvResponsePaddingHValue)

        val responsePaddingVRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(responsePaddingVRow)
        val responsePaddingVLabel =
            TextView(requireContext()).apply {
                text = "Padding (Top/Bottom):"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        responsePaddingVRow.addView(responsePaddingVLabel)
        sliderResponsePaddingV =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(4)
                    }
            }
        responsePaddingVRow.addView(sliderResponsePaddingV)
        tvResponsePaddingVValue =
            TextView(requireContext()).apply {
                text = "0"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        responsePaddingVRow.addView(tvResponsePaddingVValue)

        mainLinearLayout.addView(
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16),
                    ).apply { topMargin = dpToPx(8) }
            },
        )

        val timerRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(timerRow)
        val timerLabel =
            TextView(requireContext()).apply {
                text = "Timer Text:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        timerRow.addView(timerLabel)
        sliderTimer =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply { leftMargin = dpToPx(4) }
            }
        timerRow.addView(sliderTimer)
        tvTimerSizeValue =
            TextView(requireContext()).apply {
                text = "18"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        timerRow.addView(tvTimerSizeValue)
        timerColorPicker =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        dpToPx(36), dpToPx(36),
                    ).apply { leftMargin = dpToPx(8) }
            }
        timerRow.addView(timerColorPicker)

        val timerPaddingHRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(timerPaddingHRow)
        val timerPaddingHLabel =
            TextView(requireContext()).apply {
                text = "Timer Padding (L/R):"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        timerPaddingHRow.addView(timerPaddingHLabel)
        sliderTimerPaddingH =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(4)
                    }
            }
        timerPaddingHRow.addView(sliderTimerPaddingH)
        tvTimerPaddingHValue =
            TextView(requireContext()).apply {
                text = "0"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        timerPaddingHRow.addView(tvTimerPaddingHValue)

        val timerPaddingVRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(timerPaddingVRow)
        val timerPaddingVLabel =
            TextView(requireContext()).apply {
                text = "Timer Padding (T/B):"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        timerPaddingVRow.addView(timerPaddingVLabel)
        sliderTimerPaddingV =
            SeekBar(requireContext()).apply {
                max = 100
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(4)
                    }
            }
        timerPaddingVRow.addView(sliderTimerPaddingV)
        tvTimerPaddingVValue =
            TextView(requireContext()).apply {
                text = "0"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(4) }
            }
        timerPaddingVRow.addView(tvTimerPaddingVValue)

        val timerAlignRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
        mainLinearLayout.addView(timerAlignRow)
        val timerAlignLabel =
            TextView(requireContext()).apply {
                text = "Timer Alignment:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        timerAlignRow.addView(timerAlignLabel)
        spinnerTimerAlignment =
            Spinner(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        leftMargin = dpToPx(16)
                    }
            }
        timerAlignRow.addView(spinnerTimerAlignment)

        mainLinearLayout.addView(
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16),
                    ).apply { topMargin = dpToPx(8) }
            },
        )

        val bgRow =
            newRowLayout().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(4)
            }
        mainLinearLayout.addView(bgRow)
        val bgLabel =
            TextView(requireContext()).apply {
                text = "Background:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        bgRow.addView(bgLabel)
        screenBackgroundColorPicker =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                        leftMargin = dpToPx(8)
                    }
            }
        bgRow.addView(screenBackgroundColorPicker)

        mainLinearLayout.addView(
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16),
                    ).apply { topMargin = dpToPx(8) }
            },
        )

        val transitionsLabel =
            TextView(requireContext()).apply {
                text = "Slides transition style:"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        leftMargin = dpToPx(16)
                        topMargin = dpToPx(16)
                    }
            }
        mainLinearLayout.addView(transitionsLabel)
        spinnerTransitions =
            Spinner(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        leftMargin = dpToPx(16)
                        bottomMargin = dpToPx(16)
                    }
            }
        mainLinearLayout.addView(spinnerTransitions)

        val bottomButtonRow =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { bottomMargin = dpToPx(16) }
            }
        mainLinearLayout.addView(bottomButtonRow)
        val btnOk =
            Button(requireContext()).apply {
                text = "OK"
            }
        bottomButtonRow.addView(btnOk)
        val btnDefaults =
            Button(requireContext()).apply {
                text = "Defaults"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(16) }
            }
        bottomButtonRow.addView(btnDefaults)
        val btnCancel =
            Button(requireContext()).apply {
                text = "Cancel"
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = dpToPx(16) }
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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        loadPersistedValues()

        (view as ScrollView).setBackgroundColor(screenBgColor)

        sliderHeader.progress = headerTextSize.toInt().coerceIn(8, 100)
        sliderBody.progress = bodyTextSize.toInt().coerceIn(8, 100)
        sliderContinue.progress = continueTextSize.toInt().coerceIn(8, 100)
        sliderItem.progress = itemTextSize.toInt().coerceIn(8, 100)
        sliderResponse.progress = responseTextSize.toInt().coerceIn(8, 100)
        sliderTimer.progress = timerTextSize.toInt().coerceIn(8, 100)

        tvHeaderSizeValue.text = sliderHeader.progress.toString()
        tvBodySizeValue.text = sliderBody.progress.toString()
        tvContinueSizeValue.text = sliderContinue.progress.toString()
        tvItemSizeValue.text = sliderItem.progress.toString()
        tvResponseSizeValue.text = sliderResponse.progress.toString()
        tvTimerSizeValue.text = sliderTimer.progress.toString()

        previewHeaderTextView.textSize = sliderHeader.progress.toFloat()
        previewBodyTextView.textSize = sliderBody.progress.toFloat()
        previewContinueButton.textSize = sliderContinue.progress.toFloat()
        previewItemTextView.textSize = sliderItem.progress.toFloat()
        previewPaddingButton1.textSize = sliderResponse.progress.toFloat()
        previewPaddingButton2.textSize = sliderResponse.progress.toFloat()
        previewTimerTextView.textSize = sliderTimer.progress.toFloat()

        previewHeaderTextView.setTextColor(headerTextColor)
        previewBodyTextView.setTextColor(bodyTextColor)
        previewContinueButton.setTextColor(continueTextColor)
        previewContinueButton.setBackgroundColor(continueBackgroundColor)
        previewItemTextView.setTextColor(itemTextColor)
        previewPaddingButton1.setTextColor(responseTextColor)
        previewPaddingButton1.setBackgroundColor(buttonBackgroundColorVar)
        previewPaddingButton2.setTextColor(responseTextColor)
        previewPaddingButton2.setBackgroundColor(buttonBackgroundColorVar)
        previewTimerTextView.setTextColor(timerTextColorVar)

        applyColorPickerBoxColor(headerColorPicker, headerTextColor)
        applyColorPickerBoxColor(bodyColorPicker, bodyTextColor)
        applyColorPickerBoxColor(continueTextColorPicker, continueTextColor)
        applyColorPickerBoxColor(continueBackgroundColorPicker, continueBackgroundColor)
        applyColorPickerBoxColor(itemColorPicker, itemTextColor)
        applyColorPickerBoxColor(responseColorPicker, responseTextColor)
        applyColorPickerBoxColor(buttonTextColorPicker, buttonTextColorVar)
        applyColorPickerBoxColor(buttonBackgroundColorPicker, buttonBackgroundColorVar)
        applyColorPickerBoxColor(screenBackgroundColorPicker, screenBgColor)
        applyColorPickerBoxColor(timerColorPicker, timerTextColorVar)

        sliderResponseSpacing.max = 100
        val savedSpacing =
            SpacingManager.getResponseSpacing(requireContext()).toInt().coerceIn(0, 100)
        sliderResponseSpacing.progress = savedSpacing
        tvResponseSpacingValue.text = savedSpacing.toString()
        applyResponseSpacing(savedSpacing)

        sliderResponsePaddingH.max = 100
        val savedPaddingH =
            SpacingManager.getResponseButtonPaddingHorizontal(requireContext()).toInt()
                .coerceIn(0, 100)
        sliderResponsePaddingH.progress = savedPaddingH
        tvResponsePaddingHValue.text = savedPaddingH.toString()

        sliderResponsePaddingV.max = 100
        val savedPaddingV =
            SpacingManager.getResponseButtonPaddingVertical(requireContext()).toInt()
                .coerceIn(0, 100)
        sliderResponsePaddingV.progress = savedPaddingV
        tvResponsePaddingVValue.text = savedPaddingV.toString()
        applyResponseButtonPadding()

        sliderContinuePaddingH.max = 100
        val savedContinueH =
            SpacingManager.getContinueButtonPaddingHorizontal(requireContext()).toInt()
                .coerceIn(0, 100)
        sliderContinuePaddingH.progress = savedContinueH
        tvContinuePaddingHValue.text = savedContinueH.toString()

        sliderContinuePaddingV.max = 100
        val savedContinueV =
            SpacingManager.getContinueButtonPaddingVertical(requireContext()).toInt()
                .coerceIn(0, 100)
        sliderContinuePaddingV.progress = savedContinueV
        tvContinuePaddingVValue.text = savedContinueV.toString()
        applyContinueButtonPadding()

        sliderTimerPaddingH.max = 100
        val savedTimerH =
            SpacingManager.getTimerPaddingHorizontal(requireContext()).toInt().coerceIn(0, 100)
        sliderTimerPaddingH.progress = savedTimerH
        tvTimerPaddingHValue.text = savedTimerH.toString()

        sliderTimerPaddingV.max = 100
        val savedTimerV =
            SpacingManager.getTimerPaddingVertical(requireContext()).toInt().coerceIn(0, 100)
        sliderTimerPaddingV.progress = savedTimerV
        tvTimerPaddingVValue.text = savedTimerV.toString()
        applyTimerTextPadding()

        sliderHeader.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                val size = it.coerceIn(8, 100)
                headerTextSize = size.toFloat()
                previewHeaderTextView.textSize = size.toFloat()
                tvHeaderSizeValue.text = size.toString()
            },
        )
        sliderBody.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                val size = it.coerceIn(8, 100)
                bodyTextSize = size.toFloat()
                previewBodyTextView.textSize = size.toFloat()
                tvBodySizeValue.text = size.toString()
            },
        )
        sliderContinue.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                val size = it.coerceIn(8, 100)
                continueTextSize = size.toFloat()
                previewContinueButton.textSize = size.toFloat()
                tvContinueSizeValue.text = size.toString()
            },
        )
        sliderItem.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                val size = it.coerceIn(8, 100)
                itemTextSize = size.toFloat()
                previewItemTextView.textSize = size.toFloat()
                tvItemSizeValue.text = size.toString()
            },
        )
        sliderResponse.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                val size = it.coerceIn(8, 100)
                responseTextSize = size.toFloat()
                previewPaddingButton1.textSize = size.toFloat()
                previewPaddingButton2.textSize = size.toFloat()
                tvResponseSizeValue.text = size.toString()
            },
        )
        sliderTimer.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                val size = it.coerceIn(8, 100)
                timerTextSize = size.toFloat()
                previewTimerTextView.textSize = size.toFloat()
                tvTimerSizeValue.text = size.toString()
            },
        )

        sliderResponseSpacing.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                val spaceDp = it.coerceIn(0, 100)
                tvResponseSpacingValue.text = spaceDp.toString()
                applyResponseSpacing(spaceDp)
            },
        )
        sliderResponsePaddingH.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                tvResponsePaddingHValue.text = it.toString()
                applyResponseButtonPadding()
            },
        )
        sliderResponsePaddingV.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                tvResponsePaddingVValue.text = it.toString()
                applyResponseButtonPadding()
            },
        )
        sliderContinuePaddingH.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                tvContinuePaddingHValue.text = it.toString()
                applyContinueButtonPadding()
            },
        )
        sliderContinuePaddingV.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                tvContinuePaddingVValue.text = it.toString()
                applyContinueButtonPadding()
            },
        )
        sliderTimerPaddingH.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                tvTimerPaddingHValue.text = it.toString()
                applyTimerTextPadding()
            },
        )
        sliderTimerPaddingV.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                tvTimerPaddingVValue.text = it.toString()
                applyTimerTextPadding()
            },
        )

        // Added "Fade" to transition options
        val transitionOptions = listOf("No transition", "Slide to left", "Dissolve", "Fade")
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, transitionOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTransitions.adapter = adapter

        spinnerTransitions.setSelection(
            when (currentTransitionMode) {
                "off" -> 0
                "slide" -> 1
                "dissolve" -> 2
                "fade" -> 3
                else -> 0
            },
            false,
        )
        spinnerTransitions.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    v: View?,
                    position: Int,
                    id: Long,
                ) {
                    currentTransitionMode =
                        when (position) {
                            0 -> "off"
                            1 -> "slide"
                            2 -> "dissolve"
                            3 -> "fade"
                            else -> "off"
                        }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        val alignmentOptions = listOf("Left", "Center", "Right")
        val alignAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, alignmentOptions)
        alignAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerContinueAlignment.adapter = alignAdapter
        val prefs = requireContext().getSharedPreferences(Prefs.NAME, 0)
        val savedAlign = prefs.getString("CONTINUE_ALIGNMENT", "CENTER") ?: "CENTER"
        currentContinueAlignment = savedAlign.uppercase()
        spinnerContinueAlignment.setSelection(
            when (currentContinueAlignment) {
                "LEFT" -> 0
                "RIGHT" -> 2
                else -> 1
            },
            false,
        )
        spinnerContinueAlignment.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    v: View?,
                    position: Int,
                    id: Long,
                ) {
                    currentContinueAlignment = alignmentOptions[position].uppercase()
                    applyContinueAlignmentToPreview()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        spinnerHeaderAlignment.adapter = alignAdapter
        val savedHeaderAlign = prefs.getString("HEADER_ALIGNMENT", "CENTER") ?: "CENTER"
        currentHeaderAlignment = savedHeaderAlign.uppercase()
        spinnerHeaderAlignment.setSelection(
            when (currentHeaderAlignment) {
                "LEFT" -> 0
                "RIGHT" -> 2
                else -> 1
            },
            false,
        )
        spinnerHeaderAlignment.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    v: View?,
                    position: Int,
                    id: Long,
                ) {
                    currentHeaderAlignment = alignmentOptions[position].uppercase()
                    applyHeaderAlignmentToPreview()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        spinnerBodyAlignment.adapter = alignAdapter
        val savedBodyAlign = prefs.getString("BODY_ALIGNMENT", "CENTER") ?: "CENTER"
        currentBodyAlignment = savedBodyAlign.uppercase()
        spinnerBodyAlignment.setSelection(
            when (currentBodyAlignment) {
                "LEFT" -> 0
                "RIGHT" -> 2
                else -> 1
            },
            false,
        )
        spinnerBodyAlignment.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    v: View?,
                    position: Int,
                    id: Long,
                ) {
                    currentBodyAlignment = alignmentOptions[position].uppercase()
                    applyBodyAlignmentToPreview()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        spinnerTimerAlignment.adapter = alignAdapter
        val savedTimerAlign = prefs.getString("TIMER_ALIGNMENT", "CENTER") ?: "CENTER"
        currentTimerAlignment = savedTimerAlign.uppercase()
        spinnerTimerAlignment.setSelection(
            when (currentTimerAlignment) {
                "LEFT" -> 0
                "RIGHT" -> 2
                else -> 1
            },
            false,
        )
        spinnerTimerAlignment.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    v: View?,
                    position: Int,
                    id: Long,
                ) {
                    currentTimerAlignment = alignmentOptions[position].uppercase()
                    applyTimerAlignmentToPreview()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        applyHeaderAlignmentToPreview()
        applyBodyAlignmentToPreview()
        applyContinueAlignmentToPreview()
        applyTimerAlignmentToPreview()
    }

    private fun newRowLayout(): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun applyResponseSpacing(spaceDp: Int) {
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

    private fun applyTimerTextPadding() {
        val scale = resources.displayMetrics.density
        val horizontalPx = (sliderTimerPaddingH.progress * scale + 0.5f).toInt()
        val verticalPx = (sliderTimerPaddingV.progress * scale + 0.5f).toInt()
        previewTimerTextView.setPadding(horizontalPx, verticalPx, horizontalPx, verticalPx)
    }

    private fun applyColorPickerBoxColor(
        picker: View,
        color: Int,
    ) {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.black_outline)
        if (drawable is GradientDrawable) {
            drawable.setColor(color)
            picker.background = drawable
        }
        picker.setOnClickListener {
            ColorPickerHelper.showColorPickerDialog(requireContext(), color) { chosenColor ->
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

                    timerColorPicker -> {
                        timerTextColorVar = chosenColor
                        previewTimerTextView.setTextColor(chosenColor)
                    }
                }
                applyColorPickerBoxColor(picker, chosenColor)
            }
        }
    }

    private fun restoreAllDefaults() {
        val defaultHeaderSize = 24f
        val defaultBodySize = 16f
        val defaultContinueSize = 18f
        val defaultItemSize = 16f
        val defaultResponseSize = 16f
        val defaultTimerSize = 18f

        val defaultHeaderColor = Color.parseColor("#37474F")
        val defaultBodyColor = Color.parseColor("#616161")
        val defaultContinueTextColor = Color.WHITE
        val defaultContinueBgColor = Color.parseColor("#008577")
        val defaultItemColor = Color.parseColor("#008577")
    val defaultResponseColor = Color.WHITE
        val defaultScreenBgColor = Color.parseColor("#F5F5F5")
        val defaultButtonTextColor = Color.WHITE
        val defaultButtonBgColor = Color.parseColor("#008577")
        val defaultTimerColor = Color.BLACK

        headerTextSize = defaultHeaderSize
        bodyTextSize = defaultBodySize
        continueTextSize = defaultContinueSize
        itemTextSize = defaultItemSize
        responseTextSize = defaultResponseSize
        timerTextSize = defaultTimerSize

        headerTextColor = defaultHeaderColor
        bodyTextColor = defaultBodyColor
        continueTextColor = defaultContinueTextColor
        continueBackgroundColor = defaultContinueBgColor
        itemTextColor = defaultItemColor
        responseTextColor = defaultResponseColor
        buttonTextColorVar = defaultButtonTextColor
        buttonBackgroundColorVar = defaultButtonBgColor
        screenBgColor = defaultScreenBgColor
        timerTextColorVar = defaultTimerColor
        currentContinueAlignment = "CENTER"
        currentHeaderAlignment = "CENTER"
        currentBodyAlignment = "CENTER"
        currentTimerAlignment = "CENTER"

        sliderHeader.progress = headerTextSize.toInt()
        sliderBody.progress = bodyTextSize.toInt()
        sliderContinue.progress = continueTextSize.toInt()
        sliderItem.progress = itemTextSize.toInt()
        sliderResponse.progress = responseTextSize.toInt()
        sliderTimer.progress = timerTextSize.toInt()

        tvHeaderSizeValue.text = sliderHeader.progress.toString()
        tvBodySizeValue.text = sliderBody.progress.toString()
        tvContinueSizeValue.text = sliderContinue.progress.toString()
        tvItemSizeValue.text = sliderItem.progress.toString()
        tvResponseSizeValue.text = sliderResponse.progress.toString()
        tvTimerSizeValue.text = sliderTimer.progress.toString()

        previewHeaderTextView.textSize = headerTextSize
        previewBodyTextView.textSize = bodyTextSize
        previewContinueButton.textSize = continueTextSize
        previewItemTextView.textSize = itemTextSize
        previewPaddingButton1.textSize = responseTextSize
        previewPaddingButton2.textSize = responseTextSize
        previewTimerTextView.textSize = timerTextSize

        applyColorPickerBoxColor(headerColorPicker, headerTextColor)
        applyColorPickerBoxColor(bodyColorPicker, bodyTextColor)
        applyColorPickerBoxColor(continueTextColorPicker, continueTextColor)
        applyColorPickerBoxColor(continueBackgroundColorPicker, continueBackgroundColor)
        applyColorPickerBoxColor(itemColorPicker, itemTextColor)
        applyColorPickerBoxColor(responseColorPicker, responseTextColor)
        applyColorPickerBoxColor(buttonTextColorPicker, buttonTextColorVar)
        applyColorPickerBoxColor(buttonBackgroundColorPicker, buttonBackgroundColorVar)
        applyColorPickerBoxColor(screenBackgroundColorPicker, screenBgColor)
        applyColorPickerBoxColor(timerColorPicker, timerTextColorVar)

        previewHeaderTextView.setTextColor(headerTextColor)
        previewBodyTextView.setTextColor(bodyTextColor)
        previewContinueButton.setTextColor(continueTextColor)
        previewContinueButton.setBackgroundColor(continueBackgroundColor)
        previewItemTextView.setTextColor(itemTextColor)
        previewPaddingButton1.setTextColor(responseTextColor)
        previewPaddingButton1.setBackgroundColor(buttonBackgroundColorVar)
        previewPaddingButton2.setTextColor(responseTextColor)
        previewPaddingButton2.setBackgroundColor(buttonBackgroundColorVar)
        previewTimerTextView.setTextColor(timerTextColorVar)

        (view as? ScrollView)?.setBackgroundColor(screenBgColor)

        sliderResponseSpacing.progress = 0
        tvResponseSpacingValue.text = "0"
        applyResponseSpacing(0)

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

        sliderTimerPaddingH.progress = 0
        tvTimerPaddingHValue.text = "0"
        sliderTimerPaddingV.progress = 0
        tvTimerPaddingVValue.text = "0"
        applyTimerTextPadding()

        spinnerContinueAlignment.setSelection(1, false)
        spinnerHeaderAlignment.setSelection(1, false)
        spinnerBodyAlignment.setSelection(1, false)
        spinnerTimerAlignment.setSelection(1, false)

        applyHeaderAlignmentToPreview()
        applyBodyAlignmentToPreview()
        applyContinueAlignmentToPreview()
        applyTimerAlignmentToPreview()
    }

    private fun saveCurrentSelections() {
        ColorManager.setHeaderTextColor(requireContext(), headerTextColor)
        ColorManager.setBodyTextColor(requireContext(), bodyTextColor)
        ColorManager.setItemTextColor(requireContext(), itemTextColor)
        ColorManager.setResponseTextColor(requireContext(), responseTextColor)
        ColorManager.setButtonBackgroundColor(requireContext(), buttonBackgroundColorVar)
        ColorManager.setButtonTextColor(requireContext(), buttonTextColorVar)
        ColorManager.setContinueTextColor(requireContext(), continueTextColor)
        ColorManager.setContinueBackgroundColor(requireContext(), continueBackgroundColor)
        ColorManager.setScreenBackgroundColor(requireContext(), screenBgColor)
        ColorManager.setTimerTextColor(requireContext(), timerTextColorVar)

        FontSizeManager.setHeaderSize(requireContext(), headerTextSize)
        FontSizeManager.setBodySize(requireContext(), bodyTextSize)
        FontSizeManager.setItemSize(requireContext(), itemTextSize)
        FontSizeManager.setResponseSize(requireContext(), responseTextSize)
        FontSizeManager.setContinueSize(requireContext(), continueTextSize)
        FontSizeManager.setTimerSize(requireContext(), timerTextSize)

        SpacingManager.setResponseSpacing(
            requireContext(),
            sliderResponseSpacing.progress.toFloat(),
        )

        SpacingManager.setResponseButtonMargin(requireContext(), 0f)
        SpacingManager.setResponseButtonPaddingHorizontal(
            requireContext(),
            sliderResponsePaddingH.progress.toFloat(),
        )
        SpacingManager.setResponseButtonPaddingVertical(
            requireContext(),
            sliderResponsePaddingV.progress.toFloat(),
        )
        SpacingManager.setContinueButtonPaddingHorizontal(
            requireContext(),
            sliderContinuePaddingH.progress.toFloat(),
        )
        SpacingManager.setContinueButtonPaddingVertical(
            requireContext(),
            sliderContinuePaddingV.progress.toFloat(),
        )
        SpacingManager.setTimerPaddingHorizontal(
            requireContext(),
            sliderTimerPaddingH.progress.toFloat(),
        )
        SpacingManager.setTimerPaddingVertical(
            requireContext(),
            sliderTimerPaddingV.progress.toFloat(),
        )

        TransitionManager.setTransitionMode(requireContext(), currentTransitionMode)

        val prefs = requireContext().getSharedPreferences(Prefs.NAME, 0)
        prefs.edit().putString("CONTINUE_ALIGNMENT", currentContinueAlignment).apply()
        prefs.edit().putString("HEADER_ALIGNMENT", currentHeaderAlignment).apply()
        prefs.edit().putString("BODY_ALIGNMENT", currentBodyAlignment).apply()
        prefs.edit().putString("TIMER_ALIGNMENT", currentTimerAlignment).apply()
    }

    private fun simpleSeekBarListener(onValueChanged: (Int) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean,
            ) {
                onValueChanged(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

    private fun applyHeaderAlignmentToPreview() {
        when (currentHeaderAlignment) {
            "LEFT" -> previewHeaderTextView.gravity = Gravity.START
            "RIGHT" -> previewHeaderTextView.gravity = Gravity.END
            else -> previewHeaderTextView.gravity = Gravity.CENTER
        }
    }

    private fun applyBodyAlignmentToPreview() {
        when (currentBodyAlignment) {
            "LEFT" -> previewBodyTextView.gravity = Gravity.START
            "RIGHT" -> previewBodyTextView.gravity = Gravity.END
            else -> previewBodyTextView.gravity = Gravity.CENTER
        }
    }

    private fun applyContinueAlignmentToPreview() {
        val lp = previewContinueButton.layoutParams as? LinearLayout.LayoutParams ?: return
        lp.gravity =
            when (currentContinueAlignment) {
                "LEFT" -> Gravity.START
                "RIGHT" -> Gravity.END
                else -> Gravity.CENTER_HORIZONTAL
            }
        previewContinueButton.layoutParams = lp
    }

    private fun applyTimerAlignmentToPreview() {
        val lp = previewTimerTextView.layoutParams as? LinearLayout.LayoutParams ?: return
        lp.gravity =
            when (currentTimerAlignment) {
                "LEFT" -> Gravity.START
                "RIGHT" -> Gravity.END
                else -> Gravity.CENTER_HORIZONTAL
            }
        previewTimerTextView.layoutParams = lp
    }

    private fun loadPersistedValues() {
        headerTextSize = FontSizeManager.getHeaderSize(requireContext())
        bodyTextSize = FontSizeManager.getBodySize(requireContext())
        continueTextSize = FontSizeManager.getContinueSize(requireContext())
        itemTextSize = FontSizeManager.getItemSize(requireContext())
        responseTextSize = FontSizeManager.getResponseSize(requireContext())
        timerTextSize = FontSizeManager.getTimerSize(requireContext())

        headerTextColor = ColorManager.getHeaderTextColor(requireContext())
        bodyTextColor = ColorManager.getBodyTextColor(requireContext())
        continueTextColor = ColorManager.getContinueTextColor(requireContext())
        continueBackgroundColor = ColorManager.getContinueBackgroundColor(requireContext())
        itemTextColor = ColorManager.getItemTextColor(requireContext())
        responseTextColor = ColorManager.getResponseTextColor(requireContext())
        buttonTextColorVar = ColorManager.getButtonTextColor(requireContext())
        buttonBackgroundColorVar = ColorManager.getButtonBackgroundColor(requireContext())
        screenBgColor = ColorManager.getScreenBackgroundColor(requireContext())
        timerTextColorVar = ColorManager.getTimerTextColor(requireContext())

        val prefs = requireContext().getSharedPreferences(Prefs.NAME, 0)
        val savedTransitions = prefs.getString("TRANSITION_MODE", "off") ?: "off"
        currentTransitionMode =
            when (savedTransitions.lowercase()) {
                "off" -> "off"
                "dissolve" -> "dissolve"
                "fade" -> "fade"
                else -> "slide"
            }

        currentContinueAlignment =
            prefs.getString("CONTINUE_ALIGNMENT", "CENTER")?.uppercase() ?: "CENTER"
        currentHeaderAlignment =
            prefs.getString("HEADER_ALIGNMENT", "CENTER")?.uppercase() ?: "CENTER"
        currentBodyAlignment = prefs.getString("BODY_ALIGNMENT", "CENTER")?.uppercase() ?: "CENTER"
        currentTimerAlignment =
            prefs.getString("TIMER_ALIGNMENT", "CENTER")?.uppercase() ?: "CENTER"
    }
}
