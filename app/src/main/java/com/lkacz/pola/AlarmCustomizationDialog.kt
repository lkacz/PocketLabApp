// Filename: AlarmCustomizationDialog.kt
package com.lkacz.pola

import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment

/**
 * A dedicated dialog for customizing alarm sounds.
 * Moved all timer sound logic out of AppearanceCustomizationDialog into here.
 */
class AlarmCustomizationDialog : DialogFragment() {
    private lateinit var timerSoundEditText: EditText
    private lateinit var presetSpinner: Spinner
    private var tempMediaPlayer: MediaPlayer? = null

    private data class AlarmPreset(val label: String, val filename: String?)

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
    ): View? {
        return inflater.inflate(R.layout.dialog_alarm_customization, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        timerSoundEditText = view.findViewById(R.id.alarmSoundEditText)
        presetSpinner = view.findViewById(R.id.alarmPresetSpinner)
        val prefs = requireContext().getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val presets =
            listOf(
                AlarmPreset(
                    label = "Classic ring – built-in chime",
                    filename = "pola_alarm_classic.mp3",
                ),
                AlarmPreset(
                    label = "Soft – gentle chime",
                    filename = "pola_alarm_soft.wav",
                ),
                AlarmPreset(
                    label = "Medium – balanced tone",
                    filename = "pola_alarm_medium.wav",
                ),
                AlarmPreset(
                    label = "Hard – assertive alert",
                    filename = "pola_alarm_hard.wav",
                ),
                AlarmPreset(
                    label = "Extreme – maximum volume",
                    filename = "pola_alarm_extreme.wav",
                ),
                AlarmPreset(
                    label = "Custom (type filename below)",
                    filename = null,
                ),
            )
        presetSpinner.adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                presets.map { it.label },
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // Load current timer sound from shared prefs
        val currentTimerSound =
            prefs.getString("CUSTOM_TIMER_SOUND", "mytimersound.mp3")
                ?: "mytimersound.mp3"
        val presetIndex =
            presets.indexOfFirst { preset ->
                preset.filename != null && preset.filename.equals(currentTimerSound, ignoreCase = true)
            }
        val customIndex = presets.lastIndex
        if (presetIndex >= 0) {
            presetSpinner.setSelection(presetIndex)
            timerSoundEditText.setText(presets[presetIndex].filename)
            timerSoundEditText.isEnabled = false
        } else {
            presetSpinner.setSelection(customIndex)
            timerSoundEditText.setText(currentTimerSound)
            timerSoundEditText.isEnabled = true
        }

        presetSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val preset = presets[position]
                    if (preset.filename != null) {
                        timerSoundEditText.setText(preset.filename)
                        timerSoundEditText.isEnabled = false
                    } else {
                        timerSoundEditText.isEnabled = true
                        timerSoundEditText.requestFocus()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // No-op
                }
            }

        view.findViewById<Button>(R.id.btnPreviewAlarmSound).setOnClickListener {
            stopTempPlayer()
            val enteredSound = timerSoundEditText.text.toString().trim()
            prefs.edit().putString("CUSTOM_TIMER_SOUND", enteredSound).apply()

            val played =
                tryPlayFromResourcesFolder(enteredSound) || tryPlayFromAssets(enteredSound)
            if (!played) {
                Toast
                    .makeText(
                        requireContext(),
                        "Sound file not found. Add it to your resources folder or app assets.",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            }
        }

        view.findViewById<Button>(R.id.btnAlarmOk).setOnClickListener {
            val enteredSound = timerSoundEditText.text.toString().trim()
            if (enteredSound.isNotEmpty()) {
                prefs.edit().putString("CUSTOM_TIMER_SOUND", enteredSound).apply()
            }
            stopTempPlayer()
            dismiss()
        }

        view.findViewById<Button>(R.id.btnAlarmCancel).setOnClickListener {
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
        } catch (_: IllegalStateException) {
        }
        tempMediaPlayer = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTempPlayer()
    }

    private fun tryPlayFromResourcesFolder(fileName: String): Boolean {
        val resourcesFolderUri = ResourcesFolderManager(requireContext()).getResourcesFolderUri()
        if (resourcesFolderUri == null) {
            return false
        }
        val parentFolder = DocumentFile.fromTreeUri(requireContext(), resourcesFolderUri)
        val soundFile = parentFolder?.findFile(fileName)
        if (soundFile == null || !soundFile.isFile) {
            return false
        }

        return try {
            tempMediaPlayer =
                MediaPlayer().apply {
                    val pfd = requireContext().contentResolver.openFileDescriptor(soundFile.uri, "r")
                    if (pfd == null) {
                        throw IllegalStateException("Cannot open file descriptor for $fileName")
                    }
                    pfd.use {
                        setDataSource(it.fileDescriptor)
                    }
                    prepare()
                    start()
                }
            true
        } catch (e: Exception) {
            Toast
                .makeText(
                    requireContext(),
                    "Error playing sound: ${e.message}",
                    Toast.LENGTH_SHORT,
                )
                .show()
            false
        }
    }

    private fun tryPlayFromAssets(fileName: String): Boolean {
        return try {
            val assetManager = requireContext().assets
            assetManager.openFd(fileName).use { afd ->
                tempMediaPlayer =
                    MediaPlayer().apply {
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        prepare()
                        start()
                    }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
