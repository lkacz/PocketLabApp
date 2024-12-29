package com.lkacz.pola

import SimpleItemSelectedListener
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner

/**
 * A custom view that provides a dropdown (Spinner) for protocol options,
 * plus optional actions for media folder selection and showing protocol content.
 *
 * On creation, call [setup] to wire up the spinner items and their callbacks.
 */
class ProtocolMenuDropdown @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private lateinit var spinner: Spinner
    private lateinit var btnSelectMediaFolder: Button
    private lateinit var btnShowProtocolContent: Button

    private var onFileSelectionClick: (() -> Unit)? = null
    private var onSelectDemoClick: (() -> Unit)? = null
    private var onSelectTutorialClick: (() -> Unit)? = null
    private var onSelectMediaFolderClick: (() -> Unit)? = null
    private var onShowProtocolContentClick: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.dropdown_protocol_menu, this, true)
        spinner = findViewById(R.id.spinnerProtocols)
        btnSelectMediaFolder = findViewById(R.id.btnSelectMediaFolder)
        btnShowProtocolContent = findViewById(R.id.btnShowProtocolContent)
    }

    /**
     * Call this to initialize logic after the view is inflated.
     */
    fun setup(
        onFileSelectionClick: () -> Unit,
        onSelectDemoClick: () -> Unit,
        onSelectTutorialClick: () -> Unit,
        onSelectMediaFolderClick: () -> Unit,
        onShowProtocolContentClick: () -> Unit
    ) {
        this.onFileSelectionClick = onFileSelectionClick
        this.onSelectDemoClick = onSelectDemoClick
        this.onSelectTutorialClick = onSelectTutorialClick
        this.onSelectMediaFolderClick = onSelectMediaFolderClick
        this.onShowProtocolContentClick = onShowProtocolContentClick

        val items = listOf(
            "Select Protocol Source",
            "File (.txt)",
            "Demo Protocol",
            "Tutorial Protocol"
        )
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
        spinner.adapter = adapter

        // Spinner selection logic
        spinner.setOnItemSelectedListener(
            SimpleItemSelectedListener { position ->
                when (position) {
                    1 -> this.onFileSelectionClick?.invoke()       // File
                    2 -> this.onSelectDemoClick?.invoke()          // Demo
                    3 -> this.onSelectTutorialClick?.invoke()      // Tutorial
                }
                // Reset spinner to default item after each selection
                spinner.setSelection(0)
            }
        )

        // Media folder button
        btnSelectMediaFolder.setOnClickListener {
            this.onSelectMediaFolderClick?.invoke()
        }

        // Show protocol content
        btnShowProtocolContent.setOnClickListener {
            this.onShowProtocolContentClick?.invoke()
        }
    }
}
