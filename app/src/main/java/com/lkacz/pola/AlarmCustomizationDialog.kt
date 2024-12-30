// Filename: AlarmCustomizationDialog.kt
package com.lkacz.pola

import android.app.AlertDialog
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
    private var tempMediaPlayer: MediaPlayer? = null

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
        return inflater.inflate(R.layout.dialog_alarm_customization, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timerSoundEditText = view.findViewById(R.id.alarmSoundEditText)
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

        // Load current timer sound from shared prefs
        val currentTimerSound = prefs.getString("CUSTOM_TIMER_SOUND", "mytimersound.mp3")
            ?: "mytimersound.mp3"
        timerSoundEditText.setText(currentTimerSound)

        view.findViewById<Button>(R.id.btnPreviewAlarmSound).setOnClickListener {
            stopTempPlayer()
            val enteredSound = timerSoundEditText.text.toString().trim()
            prefs.edit().putString("CUSTOM_TIMER_SOUND", enteredSound).apply()

            val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()
            if (mediaFolderUri == null) {
                Toast.makeText(requireContext(), "No media folder selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val parentFolder = DocumentFile.fromTreeUri(requireContext(), mediaFolderUri)
            val soundFile = parentFolder?.findFile(enteredSound)
            if (soundFile == null || !soundFile.isFile) {
                Toast.makeText(requireContext(), "Sound file not found in media folder", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                tempMediaPlayer = MediaPlayer().apply {
                    val pfd = requireContext().contentResolver.openFileDescriptor(soundFile.uri, "r")
                    pfd?.use {
                        setDataSource(it.fileDescriptor)
                        prepare()
                        start()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error playing sound: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<Button>(R.id.btnAlarmOk).setOnClickListener {
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
        } catch (_: IllegalStateException) {}
        tempMediaPlayer = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTempPlayer()
    }
}
