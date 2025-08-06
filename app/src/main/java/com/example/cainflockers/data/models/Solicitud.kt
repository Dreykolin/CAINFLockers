package com.example.cainflockers.data.models

data class Solicitud(
    val timestamp: String,
    val correoInstitucional: String,
    val nombreEstudiante: String,
    val matriculaEstudiante: String,
    val ubicacionCasillero: String,
    val renovacion: String,
    val comprobanteUrl: String? = null,
    val estadoSolicitud: String = "PENDIENTE", // Puedes definir un valor por defecto si lo deseas
    val rowNumber: Int = 0
)
