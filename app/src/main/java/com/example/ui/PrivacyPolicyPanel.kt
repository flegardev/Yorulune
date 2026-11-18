package com.example.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The full policy is bundled locally and remains available without a browser or network. */
@Composable
fun PrivacyPolicyPanel() {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = true }, modifier = Modifier.testTag("privacy_support")) {
        Text("Privacy & support")
    }
    if (open) {
        val context = LocalContext.current
        val text by produceState("Loading local privacy policy…", context) {
            value = withContext(Dispatchers.IO) {
                runCatching { context.assets.open("privacy-policy.txt").bufferedReader().use { it.readText() } }
                    .getOrElse { "The bundled policy could not be opened. Support: tini.flegar@gmail.com" }
            }
        }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Privacy & support") },
            text = { Text(text, modifier = Modifier.heightIn(max = 340.dp)
                .verticalScroll(rememberScrollState()).testTag("privacy_policy_text")) },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Done") } }
        )
    }
}
