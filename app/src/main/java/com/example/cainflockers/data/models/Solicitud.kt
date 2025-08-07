package com.example.cainflockers.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Solicitud(
    @SerialName("Marca temporal") val timestamp: String,
    @SerialName("Dirección de correo electrónico") val correoInstitucional: String,
    @SerialName("Nombre Completo") val nombreEstudiante: String,
    @SerialName("Número de Matrícula") val matriculaEstudiante: String,
    @SerialName("¿Qué ubicación de casillero quieres?") val ubicacionCasillero: String,
    @SerialName("¿Quieres renovar el mismo locker del semestre pasado?") val renovacion: String,
    @SerialName("Anexo Comprobante de pago") val comprobanteUrl: String? = null,
    val estadoSolicitud: String = "PENDIENTE",
    val rowNumber: Int = 0
)