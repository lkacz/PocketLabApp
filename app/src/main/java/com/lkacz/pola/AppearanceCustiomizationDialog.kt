package com.lkacz.pola

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.fragment.app.DialogFragment

/**
 * A unified dialog for customizing both font sizes and colors
 * (header/body/button/item/response text + background).
 */
class AppearanceCustomizationDialog : DialogFragment() {

    // Preview UI references
    private lateinit var previewContainer: LinearLayout
    private lateinit var previewHeaderTextView: TextView
    private lateinit var previewBodyTextView: TextView
    private lateinit var previewButton: Button
    private lateinit var previewItemTextView: TextView
    private lateinit var previewResponseButton: Button

    // Color pickers
    private lateinit var headerColorPicker: View
    private lateinit var bodyColorPicker: View
    private lateinit var buttonTextColorPicker: View
    private lateinit var buttonBackgroundColorPicker: View
    private lateinit var itemColorPicker: View
    private lateinit var responseColorPicker: View
    private lateinit var screenBackgroundColorPicker: View

    // Font-size numeric indicators
    private lateinit var tvHeaderSizeValue: TextView
    private lateinit var tvBodySizeValue: TextView
    private lateinit var tvButtonSizeValue: TextView
    private lateinit var tvItemSizeValue: TextView
    private lateinit var tvResponseSizeValue: TextView

    // Sliders
    private lateinit var sliderHeader: SeekBar
    private lateinit var sliderBody: SeekBar
    private lateinit var sliderButton: SeekBar
    private lateinit var sliderItem: SeekBar
    private lateinit var sliderResponse: SeekBar

    // Timer Sound
    private lateinit var timerSoundEditText: EditText
    private var tempMediaPlayer: android.media.MediaPlayer? = null

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_appearance_customization, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reference the entire preview container for setting background color
        previewContainer = view.findViewById(R.id.previewContainer)

        // Font previews
        previewHeaderTextView = view.findViewById(R.id.previewHeaderTextView)
        previewBodyTextView = view.findViewById(R.id.previewBodyTextView)
        previewButton = view.findViewById(R.id.previewButton)
        previewItemTextView = view.findViewById(R.id.previewItemTextView)
        previewResponseButton = view.findViewById(R.id.previewResponseButton)

        // Color pickers
        headerColorPicker = view.findViewById(R.id.headerColorPicker)
        bodyColorPicker = view.findViewById(R.id.bodyColorPicker)
        buttonTextColorPicker = view.findViewById(R.id.buttonTextColorPicker)
        buttonBackgroundColorPicker = view.findViewById(R.id.buttonBackgroundColorPicker)
        itemColorPicker = view.findViewById(R.id.itemColorPicker)
        responseColorPicker = view.findViewById(R.id.responseColorPicker)
        screenBackgroundColorPicker = view.findViewById(R.id.screenBackgroundColorPicker)

        // Font size indicators
        tvHeaderSizeValue = view.findViewById(R.id.tvHeaderSizeValue)
        tvBodySizeValue = view.findViewById(R.id.tvBodySizeValue)
        tvButtonSizeValue = view.findViewById(R.id.tvButtonSizeValue)
        tvItemSizeValue = view.findViewById(R.id.tvItemSizeValue)
        tvResponseSizeValue = view.findViewById(R.id.tvResponseSizeValue)

        // Sliders
        sliderHeader = view.findViewById(R.id.sliderHeaderSize)
        sliderBody = view.findViewById(R.id.sliderBodySize)
        sliderButton = view.findViewById(R.id.sliderButtonSize)
        sliderItem = view.findViewById(R.id.sliderItemSize)
        sliderResponse = view.findViewById(R.id.sliderResponseSize)

        // Timer Sound
        timerSoundEditText = view.findViewById(R.id.timerSoundEditText)

        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

        // ---- Initialize Font Sizes ----
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

        // Apply initial preview font sizes
        previewHeaderTextView.textSize = sliderHeader.progress.toFloat()
        previewBodyTextView.textSize = sliderBody.progress.toFloat()
        previewButton.textSize = sliderButton.progress.toFloat()
        previewItemTextView.textSize = sliderItem.progress.toFloat()
        previewResponseButton.textSize = sliderResponse.progress.toFloat()

        // ---- Initialize Colors ----
        fun colorIntToRgbString(color: Int): String {
            // Return a hex string like "#RRGGBB" for display in the small color preview
            return String.format("#%02X%02X%02X", color.red, color.green, color.blue)
        }

        // Get current color values from ColorManager
        val headerTextColor = ColorManager.getHeaderTextColor(ctx)
        val bodyTextColor = ColorManager.getBodyTextColor(ctx)
        val buttonTextColor = ColorManager.getButtonTextColor(ctx)
        val buttonBackgroundColor = ColorManager.getButtonBackgroundColor(ctx)
        val itemTextColor = ColorManager.getItemTextColor(ctx)
        val responseTextColor = ColorManager.getResponseTextColor(ctx)
        val screenBgColor = ColorManager.getScreenBackgroundColor(ctx)

        // Apply color previews
        previewHeaderTextView.setTextColor(headerTextColor)
        previewBodyTextView.setTextColor(bodyTextColor)
        previewButton.setTextColor(buttonTextColor)
        previewButton.setBackgroundColor(buttonBackgroundColor)
        previewItemTextView.setTextColor(itemTextColor)
        previewResponseButton.setTextColor(responseTextColor)
        previewResponseButton.setBackgroundColor(buttonBackgroundColor) // just to show a variant
        previewContainer.setBackgroundColor(screenBgColor)

        // Show current color blocks
        headerColorPicker.setBackgroundColor(headerTextColor)
        bodyColorPicker.setBackgroundColor(bodyTextColor)
        buttonTextColorPicker.setBackgroundColor(buttonTextColor)
        buttonBackgroundColorPicker.setBackgroundColor(buttonBackgroundColor)
        itemColorPicker.setBackgroundColor(itemTextColor)
        responseColorPicker.setBackgroundColor(responseTextColor)
        screenBackgroundColorPicker.setBackgroundColor(screenBgColor)

        // ---- Timer Sound ----
        val currentTimerSound = prefs.getString("CUSTOM_TIMER_SOUND", "mytimersound.mp3") ?: "mytimersound.mp3"
        timerSoundEditText.setText(currentTimerSound)

        // Font-size sliders
        sliderHeader.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setHeaderSize(ctx, value.toFloat())
                previewHeaderTextView.textSize = value.toFloat()
                tvHeaderSizeValue.text = value.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        sliderBody.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setBodySize(ctx, value.toFloat())
                previewBodyTextView.textSize = value.toFloat()
                tvBodySizeValue.text = value.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        sliderButton.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setButtonSize(ctx, value.toFloat())
                previewButton.textSize = value.toFloat()
                tvButtonSizeValue.text = value.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        sliderItem.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setItemSize(ctx, value.toFloat())
                previewItemTextView.textSize = value.toFloat()
                tvItemSizeValue.text = value.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        sliderResponse.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setResponseSize(ctx, value.toFloat())
                previewResponseButton.textSize = value.toFloat()
                tvResponseSizeValue.text = value.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Simple approach to handle color picks:
        // Tapping on each color box cycles through a small set of colors
        // or triggers a color dialog. (Here we just demonstrate a simple cycle.)
        val colorChoices = listOf(
            Color.BLACK, Color.DKGRAY, Color.GRAY, Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW,
            Color.CYAN, Color.MAGENTA, Color.parseColor("#FF6200EE"), Color.WHITE
        )

        fun cycleColor(current: Int): Int {
            val idx = colorChoices.indexOf(current)
            return if (idx < 0 || idx == colorChoices.lastIndex) colorChoices.first() else colorChoices[idx + 1]
        }

        headerColorPicker.setOnClickListener {
            val newColor = cycleColor(ColorManager.getHeaderTextColor(ctx))
            ColorManager.setHeaderTextColor(ctx, newColor)
            previewHeaderTextView.setTextColor(newColor)
            it.setBackgroundColor(newColor)
        }

        bodyColorPicker.setOnClickListener {
            val newColor = cycleColor(ColorManager.getBodyTextColor(ctx))
            ColorManager.setBodyTextColor(ctx, newColor)
            previewBodyTextView.setTextColor(newColor)
            it.setBackgroundColor(newColor)
        }

        buttonTextColorPicker.setOnClickListener {
            val newColor = cycleColor(ColorManager.getButtonTextColor(ctx))
            ColorManager.setButtonTextColor(ctx, newColor)
            previewButton.setTextColor(newColor)
            previewResponseButton.setTextColor(newColor)
            it.setBackgroundColor(newColor)
        }

        buttonBackgroundColorPicker.setOnClickListener {
            val newColor = cycleColor(ColorManager.getButtonBackgroundColor(ctx))
            ColorManager.setButtonBackgroundColor(ctx, newColor)
            previewButton.setBackgroundColor(newColor)
            previewResponseButton.setBackgroundColor(newColor)
            it.setBackgroundColor(newColor)
        }

        itemColorPicker.setOnClickListener {
            val newColor = cycleColor(ColorManager.getItemTextColor(ctx))
            ColorManager.setItemTextColor(ctx, newColor)
            previewItemTextView.setTextColor(newColor)
            it.setBackgroundColor(newColor)
        }

        responseColorPicker.setOnClickListener {
            val newColor = cycleColor(ColorManager.getResponseTextColor(ctx))
            ColorManager.setResponseTextColor(ctx, newColor)
            previewResponseButton.setTextColor(newColor)
            it.setBackgroundColor(newColor)
        }

        screenBackgroundColorPicker.setOnClickListener {
            val newColor = cycleColor(ColorManager.getScreenBackgroundColor(ctx))
            ColorManager.setScreenBackgroundColor(ctx, newColor)
            previewContainer.setBackgroundColor(newColor)
            it.setBackgroundColor(newColor)
        }

        // Preview timer sound
        view.findViewById<Button>(R.id.btnPreviewSound).setOnClickListener {
            stopTempPlayer()
            val enteredSound = timerSoundEditText.text.toString().trim()
            prefs.edit().putString("CUSTOM_TIMER_SOUND", enteredSound).apply()

            val mediaFolderUri = MediaFolderManager(ctx).getMediaFolderUri()
            if (mediaFolderUri == null) {
                Toast.makeText(ctx, "No media folder selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val parentFolder = androidx.documentfile.provider.DocumentFile.fromTreeUri(ctx, mediaFolderUri)
            val soundFile = parentFolder?.findFile(enteredSound)
            if (soundFile == null || !soundFile.isFile) {
                Toast.makeText(ctx, "Sound file not found in media folder", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                tempMediaPlayer = android.media.MediaPlayer().apply {
                    val pfd = ctx.contentResolver.openFileDescriptor(soundFile.uri, "r")
                    pfd?.use {
                        setDataSource(it.fileDescriptor)
                        prepare()
                        start()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "Error playing sound: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // OK button
        val okButton = view.findViewById<Button>(R.id.btnOk)
        okButton.setOnClickListener {
            stopTempPlayer()
            dismiss()
        }

        // Cancel button
        val cancelButton = view.findViewById<Button>(R.id.btnCancel)
        cancelButton.setOnClickListener {
            stopTempPlayer()
            dismiss()
        }
    }

    private fun stopTempPlayer() {
        try {
            tempMediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: IllegalStateException) {}
        tempMediaPlayer = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTempPlayer()
    }
}
