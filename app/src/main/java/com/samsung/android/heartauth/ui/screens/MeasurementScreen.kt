package com.samsung.android.heartauth.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.samsung.android.heartauth.R
import com.samsung.android.heartauth.ui.components.GradientCircularProgressIndicator

@Composable
fun MeasurementScreen(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp), contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.intro2),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )

        GradientCircularProgressIndicator(
            progress = progress , modifier = Modifier.size(120.dp)
        )
    }
}
