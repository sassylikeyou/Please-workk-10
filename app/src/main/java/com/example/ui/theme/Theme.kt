package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ServerColorScheme = lightColorScheme(
    primary = ServerPrimary,
    onPrimary = ServerOnPrimary,
    secondary = ServerSecondary,
    onSecondary = ServerOnSecondary,
    background = ServerBackground,
    onBackground = ServerTextPrimary,
    surface = ServerSurface,
    onSurface = ServerTextPrimary,
    surfaceVariant = ServerSurfaceVariant,
    onSurfaceVariant = ServerTextPrimary,
    error = ServerError,
    onError = ServerOnPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = ServerColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
