package com.lkacz.pola.ui.screens.input

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lkacz.pola.Logger

/**
 * Replaces InputFieldFragment using Compose.
 * Allows multiple text inputs.
 */
@Composable
fun InputFieldScreen(
    navController: NavHostController,
    logger: Logger
) {
    val heading = "Input Heading"
    val text = "Please provide your responses:"
    val buttonName = "Next"
    val inputFields = listOf("Field 1", "Field 2", "Field 3")

    val fieldValues = remember { mutableStateMapOf<String, String>() }

    // Initialize keys in map
    inputFields.forEach { fieldValues.putIfAbsent(it, "") }

    Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text(heading)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text)
            Spacer(modifier = Modifier.height(8.dp))

            inputFields.forEach { label ->
                val currentValue = fieldValues[label] ?: ""
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(text = label)
                    BasicTextField(
                        value = currentValue,
                        onValueChange = { newText ->
                            fieldValues[label] = newText
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                fieldValues.forEach { (field, value) ->
                    val isNumeric = value.toDoubleOrNull() != null
                    logger.logInputFieldFragment(
                        heading,
                        text,
                        field,
                        value,
                        isNumeric
                    )
                }
                navController.navigate("end")
            }) {
                Text(buttonName)
            }
        }
    }
}
