package com.lkacz.pola.ui.screens.scale

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lkacz.pola.Logger

/**
 * Replaces ScaleFragment using Compose.
 * User picks one of several responses (like a Likert scale).
 */
@Composable
fun ScaleScreen(
    navController: NavHostController,
    logger: Logger
) {
    val header = "Scale Header"
    val introduction = "How do you rate Jetpack Compose?"
    val item = "Your overall impression:"
    val responses = listOf("Poor", "Fair", "Good", "Excellent")

    Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text(header)
            Spacer(modifier = Modifier.height(8.dp))
            Text(introduction)
            Spacer(modifier = Modifier.height(8.dp))
            Text(item)
            Spacer(modifier = Modifier.height(16.dp))

            responses.forEachIndexed { index, response ->
                Button(
                    onClick = {
                        logger.logScaleFragment(header, introduction, item, index + 1, response)
                        navController.navigate("input")
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(response)
                }
            }
        }
    }
}
