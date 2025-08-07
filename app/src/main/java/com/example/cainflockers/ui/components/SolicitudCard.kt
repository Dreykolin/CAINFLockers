package com.example.cainflockers.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cainflockers.data.models.Solicitud
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment

@Composable
fun SolicitudCard(
    solicitud: Solicitud,
    modifier: Modifier = Modifier,
    onMarkReviewed: (Solicitud, String) -> Unit,
    onViewReceipt: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var actionToConfirm by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // --- CONTENIDO BÁSICO (SIEMPRE VISIBLE) ---
            Text(
                // Lógica condicional para mostrar "Estudiante: " solo si está expandido
                text = "${if (isExpanded) "Estudiante: " else ""}${solicitud.nombreEstudiante}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                // Lógica condicional para mostrar "Estado: " solo si está expandido
                text = "${if (isExpanded) "Estado: " else ""}${solicitud.estadoSolicitud}",
                style = MaterialTheme.typography.titleMedium,
                color = when (solicitud.estadoSolicitud.uppercase()) {
                    "RECHAZADO" -> Color.Red
                    "APROBADO" -> Color.Blue
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            // --- CONTENIDO DETALLADO (SOLO VISIBLE CUANDO isExpanded es true) ---
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Correo: ${solicitud.correoInstitucional}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Matrícula: ${solicitud.matriculaEstudiante}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "¿Qué ubicación de casillero quiere?: ${solicitud.ubicacionCasillero}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "¿Quiere renovar el mismo locker del semestre pasado?: ${solicitud.renovacion}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Fecha: ${solicitud.timestamp}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                solicitud.comprobanteUrl?.let { url ->
                    if (url.isNotBlank()) {
                        Button(
                            onClick = { onViewReceipt(url) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ver Comprobante")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            actionToConfirm = "APROBADO"
                            showConfirmDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    ) {
                        Text("Aceptar")
                    }
                    Button(
                        onClick = {
                            actionToConfirm = "RECHAZADO"
                            showConfirmDialog = true
                        },
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

    if (showConfirmDialog && actionToConfirm != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                actionToConfirm = null
            },
            title = {
                Text("¿Está seguro de querer ${if (actionToConfirm == "APROBADO") "aceptar" else "rechazar"} esta solicitud?")
            },
            dismissButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        actionToConfirm = null
                    }
                ) {
                    Text("No")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMarkReviewed(solicitud, actionToConfirm!!)
                        showConfirmDialog = false
                        actionToConfirm = null
                    }
                ) {
                    Text("Sí")
                }
            }
        )
    }
}