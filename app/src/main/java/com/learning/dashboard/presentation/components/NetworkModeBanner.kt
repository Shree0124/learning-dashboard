package com.learning.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learning.dashboard.presentation.theme.WarningOrange
import com.learning.dashboard.presentation.theme.WarningOrangeBg

@Composable
fun NetworkModeBanner(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isOffline) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WarningOrangeBg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = "Offline Mode",
            tint = WarningOrange,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Offline Mode Active • Serving cached courses from local Room database",
            style = MaterialTheme.typography.labelSmall,
            color = WarningOrange
        )
    }
}
