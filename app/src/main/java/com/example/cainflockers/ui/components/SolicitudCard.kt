package com.example.cainflockers.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button // Importa Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cainflockers.data.models.Solicitud // Importa tu modelo Solicitud

@Composable
fun SolicitudCard(
    solicitud: Solicitud,
    modifier: Modifier = Modifier,
    onMarkReviewed: (Solicitud) -> Unit // Función de callback para cuando se presiona el botón
) {
    Card(
        modifier = modifier
            .fillMaxWidth() // Ocupa todo el ancho disponible
            .padding(vertical = 4.dp), // Espaciado vertical entre cards
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color.LightGray) // Borde para visualización
    ) {
        Column(modifier = Modifier.padding(16.dp)) { // Relleno dentro de la card
            Text(
                text = "Locker N°: ${solicitud.numeroLocker}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Estudiante: ${solicitud.nombreEstudiante}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "RUT: ${solicitud.rutEstudiante}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Estado: ${solicitud.estadoSolicitud}",
                style = MaterialTheme.typography.bodySmall,
                color = if (solicitud.estadoSolicitud == "Aprobada") Color.Green else Color.Red
            )
            Text(
                text = "Fecha: ${solicitud.timestamp}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp)) // Espacio entre el texto y el botón

            // Botón para cambiar el estado a "Revisado"
            // Solo muestra el botón si el estado actual no es ya "REVISADO" (ignora mayúsculas/minúsculas)
            if (solicitud.estadoSolicitud.uppercase() != "REVISADO") {
                Button(
                    onClick = { onMarkReviewed(solicitud) }, // Llama al callback pasado desde ListaSolicitudesScreen
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Marcar como Revisado")
                }
            } else {
                Text(
                    text = "Estado: Revisado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary // Color para indicar que ya está revisado
                )
            }
        }
    }
}