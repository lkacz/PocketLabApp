package com.lkacz.pola

import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment

/**
 * A dialog fragment that allows user customization of:
 *  - HEADER_SIZE, BODY_SIZE, BUTTON_SIZE, ITEM_SIZE, RESPONSE_SIZE
 *  - TIMER_SOUND (from the user-selected media folder)
 *
 * Changes persist in SharedPreferences. Sliders update each preview text in real-time.
 * A "Preview Sound" button attempts to play the selected custom timer sound if found in
 * the media folder. The user can accept changes by tapping "OK".
 *
 * **Changes Made & Reasoning:**
 * 1) Replaced the single [previewTextView] with five separate TextViews that each show how
 *    the respective slider affects the text size. The preview lines are:
 *    "Header", "Body", "Continue Button", "Scale Items", "Response Buttons".
 *    This clarifies for users exactly which slider corresponds to which UI element.
 * 2) Updated each slider’s listener to immediately reflect its size changes in the corresponding
 *    preview TextView, while still persisting values with [FontSizeManager].
 * 3) Retained existing functionalities for timer sound preview, OK/Cancel buttons, and safe
 *    cleanup of the [MediaPlayer].
 */
class FontCustomizationDialog : DialogFragment() {

    private lateinit var previewHeaderTextView: TextView
    private lateinit var previewBodyTextView: TextView
    private lateinit var previewButtonTextView: TextView
    private lateinit var previewItemTextView: TextView
    private lateinit var previewResponseTextView: TextView

    private lateinit var timerSoundEditText: EditText
    private var mediaPlayer: MediaPlayer? = null

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
        return inflater.inflate(R.layout.dialog_font_customization, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Preview TextViews for each category
        previewHeaderTextView = view.findViewById(R.id.previewHeaderTextView)
        previewBodyTextView = view.findViewById(R.id.previewBodyTextView)
        previewButtonTextView = view.findViewById(R.id.previewButtonTextView)
        previewItemTextView = view.findViewById(R.id.previewItemTextView)
        previewResponseTextView = view.findViewById(R.id.previewResponseTextView)

        // Sliders
        val sliderHeader = view.findViewById<SeekBar>(R.id.sliderHeaderSize)
        val sliderBody = view.findViewById<SeekBar>(R.id.sliderBodySize)
        val sliderButton = view.findViewById<SeekBar>(R.id.sliderButtonSize)
        val sliderItem = view.findViewById<SeekBar>(R.id.sliderItemSize)
        val sliderResponse = view.findViewById<SeekBar>(R.id.sliderResponseSize)

        // Timer Sound
        timerSoundEditText = view.findViewById(R.id.timerSoundEditText)

        // Load current settings
        val context = requireContext()
        val currentHeader = FontSizeManager.getHeaderSize(context).toInt()
        val currentBody = FontSizeManager.getBodySize(context).toInt()
        val currentButton = FontSizeManager.getButtonSize(context).toInt()
        val currentItem = FontSizeManager.getItemSize(context).toInt()
        val currentResponse = FontSizeManager.getResponseSize(context).toInt()
        val prefs = context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        val currentTimerSound = prefs.getString("CUSTOM_TIMER_SOUND", "mytimersound.mp3") ?: "mytimersound.mp3"

        // Initialize Sliders
        sliderHeader.progress = currentHeader.coerceIn(8, 100)
        sliderBody.progress = currentBody.coerceIn(8, 100)
        sliderButton.progress = currentButton.coerceIn(8, 100)
        sliderItem.progress = currentItem.coerceIn(8, 100)
        sliderResponse.progress = currentResponse.coerceIn(8, 100)
        timerSoundEditText.setText(currentTimerSound)

        // Set the initial preview sizes
        previewHeaderTextView.textSize = sliderHeader.progress.toFloat()
        previewBodyTextView.textSize = sliderBody.progress.toFloat()
        previewButtonTextView.textSize = sliderButton.progress.toFloat()
        previewItemTextView.textSize = sliderItem.progress.toFloat()
        previewResponseTextView.textSize = sliderResponse.progress.toFloat()

        // === Slider Listeners ===
        sliderHeader.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setHeaderSize(context, value.toFloat())
                previewHeaderTextView.textSize = value.toFloat()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sliderBody.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setBodySize(context, value.toFloat())
                previewBodyTextView.textSize = value.toFloat()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sliderButton.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setButtonSize(context, value.toFloat())
                previewButtonTextView.textSize = value.toFloat()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sliderItem.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setItemSize(context, value.toFloat())
                previewItemTextView.textSize = value.toFloat()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sliderResponse.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setResponseSize(context, value.toFloat())
                previewResponseTextView.textSize = value.toFloat()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Preview timer sound
        val previewSoundButton = view.findViewById<Button>(R.id.btnPreviewSound)
        previewSoundButton.setOnClickListener {
            stopMediaPlayer()
            val enteredSound = timerSoundEditText.text.toString().trim()
            prefs.edit().putString("CUSTOM_TIMER_SOUND", enteredSound).apply()

            val mediaFolderUri = MediaFolderManager(context).getMediaFolderUri()
            if (mediaFolderUri == null) {
                Toast.makeText(context, "No media folder selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val parentFolder = DocumentFile.fromTreeUri(context, mediaFolderUri)
            val soundFile = parentFolder?.findFile(enteredSound)
            if (soundFile == null || !soundFile.isFile) {
                Toast.makeText(context, "Sound file not found in media folder", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                mediaPlayer = MediaPlayer().apply {
                    val pfd = context.contentResolver.openFileDescriptor(soundFile.uri, "r")
                    pfd?.use {
                        setDataSource(it.fileDescriptor)
                        prepare()
                        start()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error playing sound: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // OK button
        val okButton = view.findViewById<Button>(R.id.btnOk)
        okButton.setOnClickListener {
            stopMediaPlayer()
            dismiss()
        }

        // Cancel button
        val cancelButton = view.findViewById<Button>(R.id.btnCancel)
        cancelButton.setOnClickListener {
            stopMediaPlayer()
            dismiss()
        }
    }

    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: IllegalStateException) {
        }
        mediaPlayer = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopMediaPlayer()
    }
}
