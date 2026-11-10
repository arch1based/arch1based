package com.arch1based.lensy.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.arch1based.lensy.core.Mood

private val Light = lightColorScheme(
    primary = Color(0xFF2B4EDB),
    primaryContainer = Color(0xFFDDE3FF),
    secondary = Color(0xFF008A70),
    background = Color(0xFFF6F7FF),
    surface = Color(0xFFF6F7FF),
    surfaceVariant = Color(0xFFE2E5F5),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB7C4FF),
    primaryContainer = Color(0xFF26358A),
    secondary = Color(0xFF5FE0C4),
    background = Color(0xFF10132B),
    surface = Color(0xFF10132B),
    surfaceVariant = Color(0xFF2A2F52),
)

@Composable
fun LensyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** The mascot's lens colour and the ring colour: green when fresh, red when overdue. */
fun moodColor(mood: Mood): Color = when (mood) {
    Mood.FRESH -> Color(0xFF19C9A3)
    Mood.OK -> Color(0xFF2FA8F5)
    Mood.TIRED -> Color(0xFFF5A623)
    Mood.DUE -> Color(0xFFFF7A59)
    Mood.OVERDUE -> Color(0xFFE5383B)
}
