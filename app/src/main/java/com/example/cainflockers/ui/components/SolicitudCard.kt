package com.example.cainflockers.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cainflockers.data.models.Solicitud
import androidx.compose.foundation.layout.Row // Importa Row para el nuevo diseño
import androidx.compose.ui.Alignment // Importa Alignment para alinear elementos
import androidx.compose.foundation.layout.width // Para controlar el ancho de los botones
// IntrinsicSize ya no es necesario si usamos un ancho fijo
// import androidx.compose.foundation.layout.IntrinsicSize


@Composable
fun SolicitudCard(
    solicitud: Solicitud,
    modifier: Modifier = Modifier,
    onMarkReviewed: (Solicitud, String) -> Unit,
    onViewReceipt: (String) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        // Contenedor principal de la tarjeta: una fila para los datos y los botones
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Padding general para el contenido de la tarjeta
            verticalAlignment = Alignment.CenterVertically // Alinea los elementos verticalmente en el centro
        ) {
            // Columna izquierda: Datos de la solicitud
            Column(modifier = Modifier.weight(1f)) { // Ocupa todo el espacio disponible a la izquierda
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
                    color = when (solicitud.estadoSolicitud.uppercase()) {
                        "REVISADO" -> MaterialTheme.colorScheme.primary
                        "APROBADA" -> Color.Green
                        else -> Color.Red
                    }
                )
                Text(
                    text = "Fecha: ${solicitud.timestamp}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            // Columna derecha: Botones
            Column(
                modifier = Modifier.padding(start = 8.dp), // Espacio entre los datos y los botones
                horizontalAlignment = Alignment.End // Alinea los botones a la derecha de su columna
            ) {
                // Definir un ancho fijo para ambos botones para que sean consistentes
                // "Marcar como No Revisado" es el texto más largo, estimamos un ancho para que quepa bien.
                val commonButtonWidth = 220.dp // Ajusta este valor si el texto no cabe o si quieres más/menos espacio

                // Botón "Ver Comprobante" (solo si la URL existe)
                solicitud.comprobanteUrl?.let { url ->
                    if (url.isNotBlank()) {
                        Button(
                            onClick = { onViewReceipt(url) },
                            modifier = Modifier.width(commonButtonWidth) // Aplicar ancho fijo
                        ) {
                            Text("Ver Comprobante")
                        }
                        Spacer(modifier = Modifier.height(4.dp)) // Pequeño espacio entre los botones
                    }
                }

                // Botón de alternancia de estado
                val isReviewed = solicitud.estadoSolicitud.uppercase() == "REVISADO"
                val buttonText = if (isReviewed) "Marcar como No Revisado" else "Marcar como Revisado"
                val newState = if (isReviewed) "PENDIENTE" else "REVISADO"

                Button(
                    onClick = { onMarkReviewed(solicitud, newState) },
                    modifier = Modifier.width(commonButtonWidth) // Aplicar ancho fijo
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}
