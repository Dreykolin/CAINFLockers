package com.example.cainflockers.data.services

import android.util.Log
import com.example.cainflockers.data.models.Solicitud
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

class GoogleSheetsService(private val credential: GoogleAccountCredential) {

    private val sheetsService: Sheets = Sheets.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    ).setApplicationName("CainfLockers").build()

    /**
     * Lee todas las filas de la hoja de cálculo y las convierte en una lista de Solicitudes.
     */
    suspend fun getSheets(spreadsheetId: String): List<Solicitud> {
        return try {
            val response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "Solicitudes!A:H")
                .execute()
            val values = response.getValues()
            if (values == null || values.isEmpty()) {
                emptyList()
            } else {
                values.drop(1).mapIndexed { index, row -> // Ignora la primera fila de encabezados
                    Solicitud(
                        timestamp = row.getOrElse(0) { "" }.toString(),
                        correoInstitucional = row.getOrElse(1) { "" }.toString(),
                        nombreEstudiante = row.getOrElse(2) { "" }.toString(),
                        matriculaEstudiante = row.getOrElse(3) { "" }.toString(),
                        ubicacionCasillero = row.getOrElse(4) { "" }.toString(),
                        renovacion = row.getOrElse(5) { "" }.toString(),
                        comprobanteUrl = row.getOrElse(6) { "" }.toString(),
                        estadoSolicitud = row.getOrElse(7) { "" }.toString(),
                        rowNumber = index + 2
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleSheetsService", "Error al leer datos de la hoja de cálculo: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Actualiza el valor de una celda específica en la hoja de cálculo.
     */
    suspend fun updateSheet(spreadsheetId: String, range: String, valueRange: ValueRange) =
        try {
            sheetsService.spreadsheets().values()
                .update(spreadsheetId, range, valueRange)
                .setValueInputOption("RAW")
                .execute()
        } catch (e: Exception) {
            Log.e("GoogleSheetsService", "Error al actualizar la celda: ${e.message}", e)
            null
        }
}