package com.lkacz.pola.ui.screens.instruction

import android.content.Context
import android.text.Spanned
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.lkacz.pola.HtmlImageLoader
import com.lkacz.pola.MediaFolderManager

/**
 * Replaces the old InstructionUiHelper approach that relied on XML-based views.
 * Renders HTML content (including inline images) and displays a Next button.
 *
 * @param context The context to retrieve folder URI and perform image loading.
 * @param header HTML string for the header.
 * @param body HTML string for the body.
 * @param nextButtonText Optional text for the button; defaults to "Next" if null or blank.
 * @param onNextClick Action performed on button click.
 */
@Composable
fun InstructionUiHelperCompose(
    context: Context,
    header: String,
    body: String,
    nextButtonText: String? = null,
    onNextClick: () -> Unit
) {
    // Retrieve the URI of the user-selected media folder
    val mediaFolderUri = remember {
        MediaFolderManager(context).getMediaFolderUri()
    }

    // Convert HTML to Spanned with inline image loading
    val headerContent: Spanned = remember(header, mediaFolderUri) {
        HtmlImageLoader.getSpannedFromHtml(
            context,
            mediaFolderUri,
            header
        )
    }
    val bodyContent: Spanned = remember(body, mediaFolderUri) {
        HtmlImageLoader.getSpannedFromHtml(
            context,
            mediaFolderUri,
            body
        )
    }

    val buttonLabel = nextButtonText?.takeIf { it.isNotBlank() } ?: "Next"
    val buttonContent: Spanned = remember(buttonLabel, mediaFolderUri) {
        HtmlImageLoader.getSpannedFromHtml(
            context,
            mediaFolderUri,
            buttonLabel
        )
    }

    Column {
        // Compose does not support direct styled text from Spanned in a single composable
        // unless you implement your own parser or adopt an existing library.
        // Below is a simplified approach: show as plain text or clickable text if needed.

        BasicText(
            text = HtmlCompat.fromHtml(headerContent.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
                .toString()
        )
        Spacer(modifier = Modifier.height(8.dp))

        BasicText(
            text = HtmlCompat.fromHtml(bodyContent.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
                .toString()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNextClick) {
            // Display button text as plain text
            Text(
                text = HtmlCompat.fromHtml(buttonContent.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
                    .toString()
            )
        }
    }
}
