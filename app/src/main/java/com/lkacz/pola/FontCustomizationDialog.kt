package com.lkacz.pola

import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment

class FontCustomizationDialog : DialogFragment() {

    private lateinit var previewHeaderTextView: TextView
    private lateinit var previewBodyTextView: TextView

    // Changed these from TextView to Button references
    private lateinit var previewButton: Button
    private lateinit var previewItemTextView: TextView
    private lateinit var previewResponseButton: Button

    private lateinit var timerSoundEditText: EditText
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var tvHeaderSizeValue: TextView
    private lateinit var tvBodySizeValue: TextView
    private lateinit var tvButtonSizeValue: TextView
    private lateinit var tvItemSizeValue: TextView
    private lateinit var tvResponseSizeValue: TextView

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

        // Preview components
        previewHeaderTextView = view.findViewById(R.id.previewHeaderTextView)
        previewBodyTextView = view.findViewById(R.id.previewBodyTextView)

        // Updated references to Button
        previewButton = view.findViewById(R.id.previewButton)
        previewItemTextView = view.findViewById(R.id.previewItemTextView)
        previewResponseButton = view.findViewById(R.id.previewResponseButton)

        // Numeric indicators
        tvHeaderSizeValue = view.findViewById(R.id.tvHeaderSizeValue)
        tvBodySizeValue = view.findViewById(R.id.tvBodySizeValue)
        tvButtonSizeValue = view.findViewById(R.id.tvButtonSizeValue)
        tvItemSizeValue = view.findViewById(R.id.tvItemSizeValue)
        tvResponseSizeValue = view.findViewById(R.id.tvResponseSizeValue)

        // Sliders
        val sliderHeader = view.findViewById<SeekBar>(R.id.sliderHeaderSize)
        val sliderBody = view.findViewById<SeekBar>(R.id.sliderBodySize)
        val sliderButton = view.findViewById<SeekBar>(R.id.sliderButtonSize)
        val sliderItem = view.findViewById<SeekBar>(R.id.sliderItemSize)
        val sliderResponse = view.findViewById<SeekBar>(R.id.sliderResponseSize)

        // Timer Sound
        timerSoundEditText = view.findViewById(R.id.timerSoundEditText)

        val context = requireContext()
        val prefs = context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

        val currentHeader = FontSizeManager.getHeaderSize(context).toInt()
        val currentBody = FontSizeManager.getBodySize(context).toInt()
        val currentButton = FontSizeManager.getButtonSize(context).toInt()
        val currentItem = FontSizeManager.getItemSize(context).toInt()
        val currentResponse = FontSizeManager.getResponseSize(context).toInt()
        val currentTimerSound = prefs.getString("CUSTOM_TIMER_SOUND", "mytimersound.mp3") ?: "mytimersound.mp3"

        // Initialize Sliders
        sliderHeader.progress = currentHeader.coerceIn(8, 100)
        sliderBody.progress = currentBody.coerceIn(8, 100)
        sliderButton.progress = currentButton.coerceIn(8, 100)
        sliderItem.progress = currentItem.coerceIn(8, 100)
        sliderResponse.progress = currentResponse.coerceIn(8, 100)

        // Initialize numeric TextViews
        tvHeaderSizeValue.text = sliderHeader.progress.toString()
        tvBodySizeValue.text = sliderBody.progress.toString()
        tvButtonSizeValue.text = sliderButton.progress.toString()
        tvItemSizeValue.text = sliderItem.progress.toString()
        tvResponseSizeValue.text = sliderResponse.progress.toString()

        // Initialize Timer Sound
        timerSoundEditText.setText(currentTimerSound)

        // Set initial preview sizes
        previewHeaderTextView.textSize = sliderHeader.progress.toFloat()
        previewBodyTextView.textSize = sliderBody.progress.toFloat()
        previewButton.textSize = sliderButton.progress.toFloat()
        previewItemTextView.textSize = sliderItem.progress.toFloat()
        previewResponseButton.textSize = sliderResponse.progress.toFloat()

        // Slider Listeners
        sliderHeader.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setHeaderSize(context, value.toFloat())
                previewHeaderTextView.textSize = value.toFloat()
                tvHeaderSizeValue.text = value.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sliderBody.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setBodySize(context, value.toFloat())
                previewBodyTextView.textSize = value.toFloat()
                tvBodySizeValue.text = value.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sliderButton.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setButtonSize(context, value.toFloat())
                previewButton.textSize = value.toFloat()
                tvButtonSizeValue.text = value.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sliderItem.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setItemSize(context, value.toFloat())
                previewItemTextView.textSize = value.toFloat()
                tvItemSizeValue.text = value.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sliderResponse.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                FontSizeManager.setResponseSize(context, value.toFloat())
                previewResponseButton.textSize = value.toFloat()
                tvResponseSizeValue.text = value.toString()
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
        } catch (_: IllegalStateException) {}
        mediaPlayer = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopMediaPlayer()
    }
}
