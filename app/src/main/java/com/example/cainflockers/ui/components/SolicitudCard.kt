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
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Datos del estudiante
            Text(
                text = "Estudiante: ${solicitud.nombreEstudiante}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Correo: ${solicitud.correoInstitucional}",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Matricula: ${solicitud.matriculaEstudiante}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = " ¿Qué ubicación de casillero quiere?: ${solicitud.ubicacionCasillero}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = " ¿Quiere renovar el mismo locker del semestre pasado?: ${solicitud.renovacion}",
                style = MaterialTheme.typography.bodyMedium
            )
            Row {
                Text(
                    text = "Estado: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface // o cualquier color fijo que uses para el label
                )
                Text(
                    text = solicitud.estadoSolicitud,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (solicitud.estadoSolicitud.uppercase()) {
                        "RECHAZADO" -> Color.Red
                        "APROBADO" -> Color.Blue
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            Text(
                text = "Fecha: ${solicitud.timestamp}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botón "Ver Comprobante"
            solicitud.comprobanteUrl?.let { url ->
                if (url.isNotBlank()) {
                    Button(
                        onClick = { onViewReceipt(url) },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Ver Comprobante")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Botones Aceptar / Rechazar en una Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onMarkReviewed(solicitud, "APROBADO") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Text("Aceptar")
                }
                Button(
                    onClick = { onMarkReviewed(solicitud, "RECHAZADO") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Text("Rechazar")
                }
            }
        }
    }
}

