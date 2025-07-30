package com.example.cainflockers.data.models
data class Solicitud(
    val timestamp: String,
    val numeroLocker: String,
    val nombreEstudiante: String,
    val rutEstudiante: String,
    val estadoSolicitud: String
)