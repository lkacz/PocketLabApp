package com.lkacz.pola.ui.screens.instruction

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.navigation.NavHostController
import com.lkacz.pola.Logger

/**
 * Replaces InstructionFragment with Compose.
 *
 * In a dynamic flow, you'd load header/body from protocol instructions.
 */
@Composable
fun InstructionScreen(
    navController: NavHostController,
    logger: Logger
) {
    val header = "Instruction Header"
    val body = """
        This is the instruction body.
        <br><i>HTML formatting is possible.</i>
    """.trimIndent()
    val nextButtonText = "Next"

    // Logging akin to onCreate
    LaunchedEffect(Unit) {
        logger.logInstructionFragment(header, body)
    }

    Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text(
                text = HtmlCompat.fromHtml(body, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { navController.navigate("tap_instruction") }) {
                Text(nextButtonText)
            }
        }
    }
}
