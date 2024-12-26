package com.lkacz.pola.ui.screens.tap

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lkacz.pola.Logger
import com.lkacz.pola.TouchCounter

/**
 * Replaces TapInstructionFragment.
 * Tapping the screen [threshold] times quickly unlocks the "Next" button.
 */
@Composable
fun TapInstructionScreen(
    navController: NavHostController,
    logger: Logger
) {
    val header = "Tap Instruction Header"
    val body = "Tap the screen 3 times quickly to unlock Next."
    val nextButtonText = "Next"
    val resetTimeMs = 1000L
    val threshold = 3
    val touchCounter = remember { TouchCounter(resetTimeMs, threshold) }
    var isNextVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logger.logInstructionFragment(header, body)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (touchCounter.onTouch()) {
                            logger.logOther("Tap threshold reached in TapInstructionScreen")
                            isNextVisible = true
                        }
                    }
                )
            }
            .padding(16.dp)
    ) {
        Column {
            Text(header)
            Spacer(modifier = Modifier.height(16.dp))
            Text(body)
            Spacer(modifier = Modifier.height(24.dp))

            if (isNextVisible) {
                Button(onClick = { navController.navigate("timer") }) {
                    Text(nextButtonText)
                }
            }
        }
    }
}
