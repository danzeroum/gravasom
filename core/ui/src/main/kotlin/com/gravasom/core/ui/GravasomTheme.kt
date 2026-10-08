package com.gravasom.core.ui

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

private val Claro = lightColorScheme(primary = Color(0xFFB3261E), onPrimary = Color.White)
private val Escuro = darkColorScheme(primary = Color(0xFFFFB4AB), onPrimary = Color(0xFF690005))

/** Tema Material 3 do Gravasom: cores dinamicas no Android 12+, vermelho de "gravando" como cor primaria nos demais. */
@Composable
fun GravasomTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> Escuro
        else -> Claro
    }
    MaterialTheme(colorScheme = colors, content = content)
}
