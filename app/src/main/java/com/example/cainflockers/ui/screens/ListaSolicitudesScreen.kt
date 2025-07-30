package com.example.cainflockers.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.cainflockers.ui.components.SolicitudCard // ¡Importa SolicitudCard!
import com.example.cainflockers.ui.viewmodel.SolicitudesViewModel

@Composable
fun ListaSolicitudesScreen(viewModel: SolicitudesViewModel) {
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
        // --- Botón de Refrescar ---
        Button(
            onClick = { viewModel.fetchSolicitudesFromCsv() }, // Llama a la función para recargar los datos
            enabled = !isLoading, // El botón se deshabilita mientras está cargando
            modifier = Modifier.fillMaxWidth() // El botón ocupa todo el ancho
        ) {
            Text("Refrescar Datos")
        }

        Spacer(modifier = Modifier.height(16.dp)) // Espacio debajo del botón

        // --- Indicador de Carga / Mensaje de Error / Lista de Solicitudes ---
        if (isLoading) {
            // Si está cargando, muestra un indicador de progreso
            CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            Text("Cargando solicitudes...", style = MaterialTheme.typography.bodyLarge)
        } else if (errorMessage != null) {
            // Si hay un error, muestra el mensaje de error
            Text(
                text = "Error al cargar: $errorMessage",
                color = Color.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            // Opcionalmente, puedes agregar un botón para reintentar aquí
            Button(onClick = { viewModel.fetchSolicitudesFromCsv() }) {
                Text("Reintentar")
            }
        } else if (solicitudes.isEmpty()) {
            // Si no hay errores, no está cargando y la lista está vacía
            Text(
                text = "No hay solicitudes disponibles.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            // Si todo está bien y hay solicitudes, las muestra en una lista perezosa
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre cada SolicitudCard
            ) {
                items(solicitudes) { solicitud ->
                    // Usa el componente SolicitudCard para mostrar cada solicitud
                    SolicitudCard(solicitud = solicitud)
                }
            }
        }
    }
}