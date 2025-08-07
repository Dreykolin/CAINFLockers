package com.example.cainflockers.ui.screens
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cainflockers.data.models.Solicitud // Importa Solicitud
import com.example.cainflockers.ui.viewmodel.SolicitudesViewModel
import com.example.cainflockers.ui.components.SolicitudCard // ¡Importa SolicitudCard!
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
@Composable
fun ListaSolicitudesScreen(
    viewModel: SolicitudesViewModel,
    onViewReceiptClick: (String) -> Unit // Este callback viene de MainActivity para abrir URLs
) {
    // Observamos los estados de nuestro ViewModel
    val solicitudes by viewModel.solicitudes.collectAsState() // La lista de solicitudes
    val isLoading by viewModel.isLoading.collectAsState()     // Si está cargando datos
    val errorMessage by viewModel.errorMessage.collectAsState() // Si hay un mensaje de error

    // 'LaunchedEffect' para la carga inicial de datos.
    // El 'Unit' como clave asegura que se ejecuta solo una vez cuando la pantalla se compone inicialmente.
    LaunchedEffect(Unit) {
        viewModel.fetchSolicitudesFromCsv()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Padding general
        horizontalAlignment = Alignment.CenterHorizontally // Centra el contenido horizontalmente
    ) {


        Spacer(modifier = Modifier.height(8.dp)) // Espacio debajo del botón

        // --- Indicador de Carga / Mensaje de Error / Lista de Solicitudes ---
        if (isLoading) {
            LoadingState()

        } else if (errorMessage != null) {
            errorMessage?.let { message ->
                ErrorState(
                    errorMessage = message,
                    onRetryClick = { viewModel.fetchSolicitudesFromCsv() }
                )
            }
        } else if (solicitudes.isEmpty()) {
            // Si no hay errores, no está cargando y la lista está vacía
            Text(
                text = "No hay solicitudes disponibles.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            val sortedSolicitudes = solicitudes.sortedByDescending {
                it.timestamp // Usa la propiedad de timestamp para ordenar
            }
            // Si todo está bien y hay solicitudes, las muestra en una lista perezosa
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre cada SolicitudCard
            ) {
                items(solicitudes) { solicitud ->
                    // Usa el componente SolicitudCard para mostrar cada solicitud
                    // ¡Aquí se pasan AMBOS callbacks: onMarkReviewed y onViewReceipt!
                    SolicitudCard(
                        solicitud = solicitud,
                        onMarkReviewed = { currentSolicitud, newState ->
                            // Llama a la función del ViewModel para actualizar el estado
                            // ¡IMPORTANTE! Reemplaza "E" con la letra de la columna donde está el estado de tu solicitud.
                            viewModel.markSolicitudAsReviewed(currentSolicitud.timestamp, currentSolicitud.rowNumber, "H", newState) // <-- ¡CAMBIA "E" SI TU COLUMNA DE ESTADO ES OTRA!
                        },
                        onViewReceipt = { url ->
                            // Llama al callback que viene de MainActivity para abrir la URL
                            onViewReceiptClick(url)
                        }
                    )
                }
            }
        }
    }
}








@Composable
fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        Text(
            text = "Cargando solicitudes...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}












@Composable
fun ErrorState(
    errorMessage: String,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icono de advertencia
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error, // Usa el color de error de tu tema
            modifier = Modifier.size(64.dp) // Un tamaño más grande para que sea más visible
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Título del error
        Text(
            text = "¡Ups! No pudimos cargar los datos.",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Mensaje de error detallado
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Botón para reintentar
        Button(onClick = onRetryClick) {
            Text("Reintentar")
        }
    }
}