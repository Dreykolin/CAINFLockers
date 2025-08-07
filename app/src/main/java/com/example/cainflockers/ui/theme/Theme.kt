package com.example.cainflockers.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80, // Puedes definir una paleta oscura diferente aquí
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = MainBlue, // <-- ¡Usamos tu color aquí!
    onPrimary = White,
    primaryContainer = LightBlue, // Un azul más claro para el contenedor de la barra superior
    onPrimaryContainer = White,
    surface = LightSurface, // Un fondo más amigable que el blanco puro
    onSurface = Black
)

@Composable
fun CAINFLockersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Desactivamos el color dinámico para usar el tuyo
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // ... (el código del color dinámico puede ser eliminado o dejado como está)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}