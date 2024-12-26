package com.lkacz.pola.ui.screens.timer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lkacz.pola.AlarmHelper
import com.lkacz.pola.Logger
import kotlinx.coroutines.delay

/**
 * Replaces TimerFragment using Compose.
 * Demonstrates a basic countdown that triggers an alarm.
 */
@Composable
fun TimerScreen(
    navController: NavHostController,
    logger: Logger
) {
    val header = "Timer Header"
    val body = "This is the timer body."
    val nextButtonText = "Next"
    val timeInSeconds = 5  // Example static value; real code would parse from protocol

    val context = androidx.compose.ui.platform.LocalContext.current
    val alarmHelper = remember { AlarmHelper(context) }

    LaunchedEffect(Unit) {
        logger.logTimerFragment(header, body, timeInSeconds)
    }

    var remainingSeconds by remember { mutableStateOf(timeInSeconds) }
    var isFinished by remember { mutableStateOf(false) }

    // Start countdown
    LaunchedEffect(timeInSeconds) {
        while (remainingSeconds > 0 && !isFinished) {
            delay(1000L)
            remainingSeconds--
        }
        if (!isFinished) {
            isFinished = true
            alarmHelper.startAlarm()
            logger.logTimerFragment(header, "Timer Finished", timeInSeconds)
        }
    }

    Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text(header)
            Spacer(modifier = Modifier.height(8.dp))
            Text(body)
            Spacer(modifier = Modifier.height(8.dp))

            if (!isFinished) {
                Text("Time left: $remainingSeconds seconds")
            } else {
                Text("Continue.")
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isFinished) {
                Button(onClick = {
                    alarmHelper.stopAlarm()
                    logger.logTimerFragment(header, "Next Button Clicked", 0)
                    navController.navigate("scale")
                }) {
                    Text(nextButtonText)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            alarmHelper.release()
            logger.logTimerFragment(header, "Destroyed", timeInSeconds)
        }
    }
}
