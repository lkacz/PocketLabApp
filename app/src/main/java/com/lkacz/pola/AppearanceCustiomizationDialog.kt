package com.lkacz.pola

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment

/**
 * A unified dialog for customizing both font sizes and colors
 * (header/body/button/item/response text + background).
 *
 * Revised to place color pickers next to each slider and to use a
 * standard color picker dialog for full customization.
 */
class AppearanceCustomizationDialog : DialogFragment() {

    // Preview UI references
    private lateinit var previewContainer: LinearLayout
    private lateinit var previewHeaderTextView: TextView
    private lateinit var previewBodyTextView: TextView
    private lateinit var previewButton: Button
    private lateinit var previewItemTextView: TextView
    private lateinit var previewResponseButton: Button

    // Sliders and corresponding indicators
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

    // Timer Sound
    private lateinit var timerSoundEditText: EditText
    private var tempMediaPlayer: android.media.MediaPlayer? = null

    // Current colors
    private var headerTextColor: Int = Color.BLACK
    private var bodyTextColor: Int = Color.DKGRAY
    private var buttonTextColor: Int = Color.WHITE
    private var buttonBackgroundColor: Int = Color.BLUE
    private var itemTextColor: Int = Color.BLUE
    private var responseTextColor: Int = Color.RED
    private var screenBgColor: Int = Color.WHITE

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

        // References
        previewContainer = view.findViewById(R.id.previewContainer)
        previewHeaderTextView = view.findViewById(R.id.previewHeaderTextView)
        previewBodyTextView = view.findViewById(R.id.previewBodyTextView)
        previewButton = view.findViewById(R.id.previewButton)
        previewItemTextView = view.findViewById(R.id.previewItemTextView)
        previewResponseButton = view.findViewById(R.id.previewResponseButton)

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

        timerSoundEditText = view.findViewById(R.id.timerSoundEditText)

        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

        // Load stored font sizes
        val currentHeaderSize = FontSizeManager.getHeaderSize(ctx).toInt()
        val currentBodySize = FontSizeManager.getBodySize(ctx).toInt()
        val currentButtonSize = FontSizeManager.getButtonSize(ctx).toInt()
        val currentItemSize = FontSizeManager.getItemSize(ctx).toInt()
        val currentResponseSize = FontSizeManager.getResponseSize(ctx).toInt()

        // Initialize slider positions
        sliderHeader.progress = currentHeaderSize.coerceIn(8, 100)
        sliderBody.progress = currentBodySize.coerceIn(8, 100)
        sliderButton.progress = currentButtonSize.coerceIn(8, 100)
        sliderItem.progress = currentItemSize.coerceIn(8, 100)
        sliderResponse.progress = currentResponseSize.coerceIn(8, 100)

        // Initialize numeric TextViews
        tvHeaderSizeValue.text = sliderHeader.progress.toString()
        tvBodySizeValue.text = sliderBody.progress.toString()
        tvButtonSizeValue.text = sliderButton.progress.toString()
        tvItemSizeValue.text = sliderItem.progress.toString()
        tvResponseSizeValue.text = sliderResponse.progress.toString()

        // Preview font sizes
        previewHeaderTextView.textSize = sliderHeader.progress.toFloat()
        previewBodyTextView.textSize = sliderBody.progress.toFloat()
        previewButton.textSize = sliderButton.progress.toFloat()
        previewItemTextView.textSize = sliderItem.progress.toFloat()
        previewResponseButton.textSize = sliderResponse.progress.toFloat()

        // Stored colors
        headerTextColor = ColorManager.getHeaderTextColor(ctx)
        bodyTextColor = ColorManager.getBodyTextColor(ctx)
        buttonTextColor = ColorManager.getButtonTextColor(ctx)
        buttonBackgroundColor = ColorManager.getButtonBackgroundColor(ctx)
        itemTextColor = ColorManager.getItemTextColor(ctx)
        responseTextColor = ColorManager.getResponseTextColor(ctx)
        screenBgColor = ColorManager.getScreenBackgroundColor(ctx)

        // Apply color previews
        previewHeaderTextView.setTextColor(headerTextColor)
        previewBodyTextView.setTextColor(bodyTextColor)
        previewButton.setTextColor(buttonTextColor)
        previewButton.setBackgroundColor(buttonBackgroundColor)
        previewItemTextView.setTextColor(itemTextColor)
        previewResponseButton.setTextColor(responseTextColor)
        previewResponseButton.setBackgroundColor(buttonBackgroundColor)
        previewContainer.setBackgroundColor(screenBgColor)

        // Timer Sound
        val currentTimerSound = prefs.getString("CUSTOM_TIMER_SOUND", "mytimersound.mp3")
            ?: "mytimersound.mp3"
        timerSoundEditText.setText(currentTimerSound)

        // Slider change listeners
        sliderHeader.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                FontSizeManager.setHeaderSize(ctx, it.toFloat())
                previewHeaderTextView.textSize = it.toFloat()
                tvHeaderSizeValue.text = it.toString()
            }
        )

        sliderBody.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                FontSizeManager.setBodySize(ctx, it.toFloat())
                previewBodyTextView.textSize = it.toFloat()
                tvBodySizeValue.text = it.toString()
            }
        )

        sliderButton.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                FontSizeManager.setButtonSize(ctx, it.toFloat())
                previewButton.textSize = it.toFloat()
                tvButtonSizeValue.text = it.toString()
            }
        )

        sliderItem.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                FontSizeManager.setItemSize(ctx, it.toFloat())
                previewItemTextView.textSize = it.toFloat()
                tvItemSizeValue.text = it.toString()
            }
        )

        sliderResponse.setOnSeekBarChangeListener(
            simpleSeekBarListener {
                FontSizeManager.setResponseSize(ctx, it.toFloat())
                previewResponseButton.textSize = it.toFloat()
                tvResponseSizeValue.text = it.toString()
            }
        )

        // Now set up color pickers using standard color selection
        // For each UI element, we'll open a color picker dialog.
        // Header text color
        view.findViewById<View>(R.id.headerColorPicker).apply {
            setBackgroundColor(headerTextColor)
            setOnClickListener {
                openColorPicker(headerTextColor) { chosenColor ->
                    headerTextColor = chosenColor
                    setBackgroundColor(chosenColor)
                    ColorManager.setHeaderTextColor(ctx, chosenColor)
                    previewHeaderTextView.setTextColor(chosenColor)
                }
            }
        }

        // Body text color
        view.findViewById<View>(R.id.bodyColorPicker).apply {
            setBackgroundColor(bodyTextColor)
            setOnClickListener {
                openColorPicker(bodyTextColor) { chosenColor ->
                    bodyTextColor = chosenColor
                    setBackgroundColor(chosenColor)
                    ColorManager.setBodyTextColor(ctx, chosenColor)
                    previewBodyTextView.setTextColor(chosenColor)
                }
            }
        }

        // Button text color
        view.findViewById<View>(R.id.buttonTextColorPicker).apply {
            setBackgroundColor(buttonTextColor)
            setOnClickListener {
                openColorPicker(buttonTextColor) { chosenColor ->
                    buttonTextColor = chosenColor
                    setBackgroundColor(chosenColor)
                    ColorManager.setButtonTextColor(ctx, chosenColor)
                    previewButton.setTextColor(chosenColor)
                    previewResponseButton.setTextColor(chosenColor)
                }
            }
        }

        // Button background color
        view.findViewById<View>(R.id.buttonBackgroundColorPicker).apply {
            setBackgroundColor(buttonBackgroundColor)
            setOnClickListener {
                openColorPicker(buttonBackgroundColor) { chosenColor ->
                    buttonBackgroundColor = chosenColor
                    setBackgroundColor(chosenColor)
                    ColorManager.setButtonBackgroundColor(ctx, chosenColor)
                    previewButton.setBackgroundColor(chosenColor)
                    previewResponseButton.setBackgroundColor(chosenColor)
                }
            }
        }

        // Item text color
        view.findViewById<View>(R.id.itemColorPicker).apply {
            setBackgroundColor(itemTextColor)
            setOnClickListener {
                openColorPicker(itemTextColor) { chosenColor ->
                    itemTextColor = chosenColor
                    setBackgroundColor(chosenColor)
                    ColorManager.setItemTextColor(ctx, chosenColor)
                    previewItemTextView.setTextColor(chosenColor)
                }
            }
        }

        // Response text color
        view.findViewById<View>(R.id.responseColorPicker).apply {
            setBackgroundColor(responseTextColor)
            setOnClickListener {
                openColorPicker(responseTextColor) { chosenColor ->
                    responseTextColor = chosenColor
                    setBackgroundColor(chosenColor)
                    ColorManager.setResponseTextColor(ctx, chosenColor)
                    previewResponseButton.setTextColor(chosenColor)
                }
            }
        }

        // Screen background color
        view.findViewById<View>(R.id.screenBackgroundColorPicker).apply {
            setBackgroundColor(screenBgColor)
            setOnClickListener {
                openColorPicker(screenBgColor) { chosenColor ->
                    screenBgColor = chosenColor
                    setBackgroundColor(chosenColor)
                    ColorManager.setScreenBackgroundColor(ctx, chosenColor)
                    previewContainer.setBackgroundColor(chosenColor)
                }
            }
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
            val parentFolder = DocumentFile.fromTreeUri(ctx, mediaFolderUri)
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

        // Defaults button
        // Sets an elegant set of sizes/colors for all UI elements
        val defaultsButton = Button(requireContext()).apply {
            id = View.generateViewId()
            text = "Defaults"
            setOnClickListener {
                restoreAllDefaults(ctx)
            }
        }
        // Insert it into the same parent as the OK/Cancel buttons
        val buttonsLayout = cancelButton.parent as? LinearLayout
        buttonsLayout?.addView(defaultsButton, buttonsLayout.indexOfChild(cancelButton))
    }

    /**
     * Shows a standard color picker dialog. The callback returns the
     * newly chosen color as an ARGB integer.
     */
    private fun openColorPicker(initialColor: Int, onColorSelected: (Int) -> Unit) {
        // A simple AlertDialog with RGB inputs or a third-party color picker can be used.
        // Below is a basic example with RGB SeekBars, but you may replace it
        // with any robust color picker library or custom view as desired.

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
     * Sets a curated set of defaults to provide a cohesive, elegant style.
     * Also updates the UI preview.
     */
    private fun restoreAllDefaults(ctx: Context) {
        // Example "elegant" defaults
        val defaultHeaderSize = 48f
        val defaultBodySize = 18f
        val defaultButtonSize = 18f
        val defaultItemSize = 22f
        val defaultResponseSize = 16f

        val defaultHeaderColor = Color.parseColor("#37474F")      // Dark Gray
        val defaultBodyColor = Color.parseColor("#616161")        // Medium Gray
        val defaultButtonTextColor = Color.WHITE
        val defaultButtonBgColor = Color.parseColor("#008577")    // Teal
        val defaultItemColor = Color.parseColor("#008577")        // Teal
        val defaultResponseColor = Color.parseColor("#C51162")    // Pinkish
        val defaultScreenBgColor = Color.parseColor("#F5F5F5")    // Light Gray

        // Font sizes
        FontSizeManager.setHeaderSize(ctx, defaultHeaderSize)
        FontSizeManager.setBodySize(ctx, defaultBodySize)
        FontSizeManager.setButtonSize(ctx, defaultButtonSize)
        FontSizeManager.setItemSize(ctx, defaultItemSize)
        FontSizeManager.setResponseSize(ctx, defaultResponseSize)

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

        previewHeaderTextView.textSize = defaultHeaderSize
        previewBodyTextView.textSize = defaultBodySize
        previewButton.textSize = defaultButtonSize
        previewItemTextView.textSize = defaultItemSize
        previewResponseButton.textSize = defaultResponseSize

        // Colors
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

        view?.findViewById<View>(R.id.headerColorPicker)?.setBackgroundColor(defaultHeaderColor)
        view?.findViewById<View>(R.id.bodyColorPicker)?.setBackgroundColor(defaultBodyColor)
        view?.findViewById<View>(R.id.buttonTextColorPicker)?.setBackgroundColor(defaultButtonTextColor)
        view?.findViewById<View>(R.id.buttonBackgroundColorPicker)?.setBackgroundColor(defaultButtonBgColor)
        view?.findViewById<View>(R.id.itemColorPicker)?.setBackgroundColor(defaultItemColor)
        view?.findViewById<View>(R.id.responseColorPicker)?.setBackgroundColor(defaultResponseColor)
        view?.findViewById<View>(R.id.screenBackgroundColorPicker)?.setBackgroundColor(defaultScreenBgColor)

        previewHeaderTextView.setTextColor(defaultHeaderColor)
        previewBodyTextView.setTextColor(defaultBodyColor)
        previewButton.setTextColor(defaultButtonTextColor)
        previewButton.setBackgroundColor(defaultButtonBgColor)
        previewItemTextView.setTextColor(defaultItemColor)
        previewResponseButton.setTextColor(defaultResponseColor)
        previewResponseButton.setBackgroundColor(defaultButtonBgColor)
        previewContainer.setBackgroundColor(defaultScreenBgColor)
    }

    private fun stopTempPlayer() {
        try {
            tempMediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: IllegalStateException) { }
        tempMediaPlayer = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTempPlayer()
    }

    /**
     * Helper to shorten OnSeekBarChangeListener usage.
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
