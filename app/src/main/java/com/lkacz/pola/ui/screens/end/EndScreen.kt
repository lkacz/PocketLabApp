package com.lkacz.pola.ui.screens.end

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lkacz.pola.Logger

/**
 * Replaces EndFragment using Compose.
 * The user can exit the activity, akin to the old fragment finishing the Activity.
 */
@Composable
fun EndScreen(
    navController: NavHostController,
    logger: Logger
) {
    val heading = "Study End"

    LaunchedEffect(Unit) {
        // Backup log file after a slight delay if needed
        logger.backupLogFile()
    }

    Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text(heading)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Thank you for participating.")
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                // Close the activity in Compose
                navController.context.apply {
                    (this as? androidx.activity.ComponentActivity)?.finish()
                }
            }) {
                Text("Exit")
            }
        }
    }
}
