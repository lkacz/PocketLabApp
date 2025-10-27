// Filename: WelcomeDialogManager.kt
package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat

object WelcomeDialogManager {
    fun showWelcomeDialogIfNeeded(context: Context) {
        val sharedPref = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val hasShownWelcome = sharedPref.getBoolean(Prefs.KEY_WELCOME_DIALOG_SHOWN, false)

        if (!hasShownWelcome) {
            showWelcomeDialog(context, sharedPref)
        }
    }

    private fun showWelcomeDialog(context: Context, sharedPref: SharedPreferences) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_welcome, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.welcome_message)
        val dontShowAgainCheckBox = dialogView.findViewById<CheckBox>(R.id.dont_show_again_checkbox)

        // Format the message with HTML for links and formatting
        val messageHtml = """
            <p><b>Welcome to Pocket Lab App!</b></p>
            
            <p>This app runs research protocols saved as <b>.txt files</b>.</p>
            
            <p><b>Creating Protocols:</b></p>
            <ul>
                <li>Use the built-in <b>Protocol Manager</b> to edit protocols on your phone</li>
                <li>Or use the <b><a href="https://lkacz.github.io/PocketLabApp/">Online Protocol Editor</a></b> on your PC for a better editing experience</li>
            </ul>
            
            <p><b>Transferring Protocols:</b><br>
            Transfer protocol files to your phone via USB cable or cloud storage (Google Drive, Dropbox, etc.)</p>
            
            <p>For detailed instructions, consult the <b>Manual</b> from the main screen.</p>
        """.trimIndent()

        messageTextView.text = HtmlCompat.fromHtml(messageHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
        messageTextView.movementMethod = LinkMovementMethod.getInstance()

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Got it!") { _, _ ->
                if (dontShowAgainCheckBox.isChecked) {
                    sharedPref.edit().putBoolean(Prefs.KEY_WELCOME_DIALOG_SHOWN, true).apply()
                }
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }
}
