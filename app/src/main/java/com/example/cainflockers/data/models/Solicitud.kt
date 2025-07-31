package com.example.cainflockers.data.models

data class Solicitud(
    val timestamp: String,
    val numeroLocker: String,
    val nombreEstudiante: String,
    val rutEstudiante: String,
    val estadoSolicitud: String,
    val rowNumber: Int = 0,
    val comprobanteUrl: String? = null // <--- ¡NUEVA LÍNEA AÑADIDA! Puede ser nulo si no hay comprobante.
)
