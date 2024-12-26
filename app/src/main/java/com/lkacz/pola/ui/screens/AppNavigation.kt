package com.lkacz.pola.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.lkacz.pola.Logger
import com.lkacz.pola.ui.MainActivity
import com.lkacz.pola.ui.screens.end.EndScreen
import com.lkacz.pola.ui.screens.input.InputFieldScreen
import com.lkacz.pola.ui.screens.instruction.InstructionScreen
import com.lkacz.pola.ui.screens.scale.ScaleScreen
import com.lkacz.pola.ui.screens.start.StartScreen
import com.lkacz.pola.ui.screens.tap.TapInstructionScreen
import com.lkacz.pola.ui.screens.timer.TimerScreen
import androidx.navigation.compose.composable

/**
 * Central navigation graph for Compose.
 *
 * Each destination replaces a Fragment from the original project.
 * You could extend this to parse protocol lines dynamically and navigate accordingly.
 */
@Composable
fun AppNavigation(
    mainActivity: MainActivity,
    logger: Logger
) {
    val navController: NavHostController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "start"
    ) {
        composable("start") {
            StartScreen(
                navController = navController,
                mainActivity = mainActivity,
                logger = logger
            )
        }
        composable("instruction") {
            InstructionScreen(
                navController = navController,
                logger = logger
            )
        }
        composable("tap_instruction") {
            TapInstructionScreen(
                navController = navController,
                logger = logger
            )
        }
        composable("timer") {
            TimerScreen(
                navController = navController,
                logger = logger
            )
        }
        composable("scale") {
            ScaleScreen(
                navController = navController,
                logger = logger
            )
        }
        composable("input") {
            InputFieldScreen(
                navController = navController,
                logger = logger
            )
        }
        composable("end") {
            EndScreen(
                navController = navController,
                logger = logger
            )
        }
    }
}
