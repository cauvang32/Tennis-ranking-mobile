package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFA8C7FA),
    secondary = Color(0xFFBAC6EA),
    tertiary = Color(0xFFD8E2FF),
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1A1C1E),
    onPrimary = Color(0xFF003062),
    onBackground = Color(0xFFE3E2E6),
    onSurface = Color(0xFFE3E2E6),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF44474E),
    tertiaryContainer = Color(0xFF0F5223),
    onTertiaryContainer = Color(0xFF6DD58C)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF005AC1),
    secondary = Color(0xFF525E7D),
    tertiary = Color(0xFF0061A4),
    background = Color(0xFFFDFBFF),
    surface = Color(0xFFFDFBFF),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    tertiaryContainer = Color(0xFFE6F4EA),
    onTertiaryContainer = Color(0xFF137333)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color disabled by default to preserve app brand consistency
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
