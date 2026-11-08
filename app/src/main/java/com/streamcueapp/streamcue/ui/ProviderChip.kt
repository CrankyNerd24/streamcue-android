package com.streamcueapp.streamcue.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared "where to watch" badge — used by the show/movie detail screens
 * and the title preview screen. */
@Composable
fun ProviderChip(name: String, isFree: Boolean) {
    AssistChip(
        onClick = {},
        label = { Text(name) },
        modifier = Modifier.padding(end = 8.dp),
        colors = if (isFree) {
            AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.secondary)
        } else {
            AssistChipDefaults.assistChipColors()
        }
    )
}
