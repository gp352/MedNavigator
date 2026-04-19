package com.mednavigator.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ResponseCard(
    icon: ImageVector,
    label: String,
    text: String,
    accentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (text.isBlank()) "Waiting for input..." else text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (text.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
