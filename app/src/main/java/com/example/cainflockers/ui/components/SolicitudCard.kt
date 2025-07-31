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
    // onMarkReviewed ahora recibe la Solicitud y el NUEVO estado deseado
    onMarkReviewed: (Solicitud, String) -> Unit
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
            // El texto de estado ahora es siempre visible y su color puede variar.
            // Eliminamos la redundancia del estado aquí.
            Text(
                text = "Estado: ${solicitud.estadoSolicitud}",
                style = MaterialTheme.typography.bodySmall,
                color = when (solicitud.estadoSolicitud.uppercase()) {
                    "REVISADO" -> MaterialTheme.colorScheme.primary // O Color.Green si prefieres
                    "APROBADA" -> Color.Green // Si "Aprobada" es un estado diferente con su propio color
                    else -> Color.Red // Para "PENDIENTE" o cualquier otro estado por defecto
                }
            )
            Text(
                text = "Fecha: ${solicitud.timestamp}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp)) // Espacio entre el texto y el botón

            // --- ¡EL BOTÓN SIEMPRE ESTÁ AQUÍ Y CAMBIA SU TEXTO Y ACCIÓN! ---
            val isReviewed = solicitud.estadoSolicitud.uppercase() == "REVISADO"
            val buttonText = if (isReviewed) "Marcar como No Revisado" else "Marcar como Revisado"
            // Define el estado al que se debe cambiar. Si es "REVISADO", se cambia a "PENDIENTE" (o tu estado inicial).
            val newState = if (isReviewed) "PENDIENTE" else "REVISADO"

            Button(
                onClick = { onMarkReviewed(solicitud, newState) }, // Pasa la solicitud y el NUEVO estado
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }
            // --- FIN DEL BOTÓN DE ALTERNANCIA ---
        }
    }
}