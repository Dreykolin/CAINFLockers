package com.example.cainflockers.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cainflockers.data.models.Solicitud
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

// Importaciones necesarias para Google Sheets API y Credenciales
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
// import com.google.api.services.sheets.v4.Sheets // Si vas a inicializar el servicio aquí
// import com.google.api.client.extensions.android.http.AndroidHttp // Si vas a inicializar el servicio aquí
// import com.google.api.client.json.gson.GsonFactory // Si vas a inicializar el servicio aquí


class SolicitudesViewModel : ViewModel() {

    private val _solicitudes = MutableStateFlow<List<Solicitud>>(emptyList())
    val solicitudes: StateFlow<List<Solicitud>> get() = _solicitudes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    // URL de tu CSV
    private val csvUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vSXWcbMwf9FPU4PId68Znb3sMl9aVBI57K9VkZtu-q_RugNOb2wbL939ARsmo50BnFp12J1r_CFw0fj/pub?output=csv"

    // --- PROPIEDAD Y FUNCIÓN PARA LAS CREDENCIALES DE GOOGLE ---
    // Esta propiedad almacenará las credenciales de Google del usuario autenticado.
    private var googleCredential: GoogleAccountCredential? = null

    /**
     * Establece las credenciales de Google para que el ViewModel pueda usarlas
     * para interactuar con APIs de Google como Sheets.
     * @param credential Las credenciales de Google obtenidas tras el inicio de sesión.
     */
    fun setGoogleCredential(credential: GoogleAccountCredential) {
        this.googleCredential = credential
        Log.d("SolicitudesViewModel", "Credencial de Google establecida.")
        // TODO: Aquí, una vez que tienes la credencial, podrías inicializar
        // el servicio de Google Sheets o cualquier otra operación que necesite
        // estas credenciales. Por ejemplo:
        // initializeSheetsService()
    }

    /**
     * Devuelve las credenciales de Google almacenadas.
     * @return Las credenciales de Google, o null si no se han establecido.
     */
    fun getGoogleCredential(): GoogleAccountCredential? {
        return googleCredential
    }

    // TODO: Ejemplo de cómo inicializar el servicio de Sheets (descomentar y adaptar si lo necesitas)
    /*
    private lateinit var sheetsService: Sheets

    private fun initializeSheetsService() {
        if (googleCredential != null) {
            sheetsService = Sheets.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                googleCredential
            ).setApplicationName("CAINFLockersApp").build() // Asegúrate de usar el nombre de tu app
            Log.d("SolicitudesViewModel", "Servicio de Google Sheets inicializado.")
        } else {
            Log.e("SolicitudesViewModel", "No se puede inicializar el servicio de Sheets: credencial nula.")
        }
    }

    // TODO: Ejemplo de cómo actualizar datos en una hoja de cálculo
    fun updateSheetData(spreadsheetId: String, range: String, data: List<List<Any>>) {
        viewModelScope.launch {
            try {
                if (!::sheetsService.isInitialized || googleCredential == null) {
                    // Si el servicio no está inicializado o la credencial es nula, intenta inicializar
                    initializeSheetsService()
                    if (googleCredential == null) {
                        _errorMessage.value = "Error: No hay credenciales de Google para actualizar la hoja."
                        return@launch
                    }
                }

                val body = com.google.api.services.sheets.v4.model.ValueRange().setValues(data)
                val result = sheetsService.spreadsheets().values().update(spreadsheetId, range, body)
                    .setValueInputOption("RAW") // O "USER_ENTERED"
                    .execute()
                Log.d("SheetsUpdate", "Hoja actualizada: ${result.updatedCells} celdas.")
                // Después de actualizar, podrías querer recargar los datos
                // fetchSolicitudesFromCsv()
            } catch (e: Exception) {
                Log.e("SheetsUpdate", "Error al actualizar la hoja: ${e.message}", e)
                _errorMessage.value = "Error al actualizar la hoja: ${e.localizedMessage}"
            }
        }
    }
    */
    // --- FIN DE LA SECCIÓN DE CREDENCIALES Y SHEETS API ---


    fun fetchSolicitudesFromCsv() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val csvText = withContext(Dispatchers.IO) {
                    Log.d("CSV_DOWNLOAD", "Intentando descargar CSV desde: $csvUrl")
                    val client = OkHttpClient()
                    val request = Request.Builder().url(csvUrl).build()
                    val response = client.newCall(request).execute()

                    response.use {
                        if (it.isSuccessful && it.body != null) {
                            it.body!!.string()
                        } else {
                            val errorBody = it.body?.string() ?: "N/A"
                            Log.e("CSV_DOWNLOAD", "Respuesta no exitosa o body nulo: código ${it.code}, body: $errorBody")
                            throw IOException("Error de red: Código ${it.code}, Mensaje: ${it.message}")
                        }
                    }
                }

                if (csvText.isNotEmpty()) {
                    val rows = csvReader().readAllWithHeader(csvText)
                    val lista = rows.map { row ->
                        Solicitud(
                            timestamp = row["timestamp"] ?: "",
                            numeroLocker = row["numeroLocker"] ?: "",
                            nombreEstudiante = row["nombreEstudiante"] ?: "",
                            rutEstudiante = row["rutEstudiante"] ?: "",
                            estadoSolicitud = row["estadoSolicitud"] ?: ""
                        )
                    }
                    _solicitudes.value = lista
                    Log.d("CSV_DOWNLOAD", "CSV procesado exitosamente. ${lista.size} solicitudes cargadas.")
                } else {
                    _errorMessage.value = "El archivo CSV está vacío o no se pudo descargar."
                    Log.e("CSV_DOWNLOAD", "El archivo CSV está vacío o no se pudo descargar.")
                }

            } catch (e: IOException) {
                Log.e("CSV_DOWNLOAD", "Error de red o E/S: ${e.message}", e)
                _errorMessage.value = "Error de conexión: ${e.localizedMessage ?: "Verifica tu conexión a internet."}"
            } catch (e: Exception) {
                Log.e("CSV_DOWNLOAD", "Se capturó una excepción general al procesar los datos: ${e.message}", e)
                _errorMessage.value = "Hubo un problema al procesar los datos."
            } finally {
                _isLoading.value = false
            }
        }
    }
}