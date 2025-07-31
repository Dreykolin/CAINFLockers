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
import com.google.api.services.sheets.v4.Sheets // Importa la clase Sheets
import com.google.api.client.extensions.android.http.AndroidHttp // Para el transporte HTTP
import com.google.api.client.json.gson.GsonFactory // Para el parser JSON
import com.google.api.services.sheets.v4.model.ValueRange // Para enviar datos a la hoja


class SolicitudesViewModel : ViewModel() {

    private val _solicitudes = MutableStateFlow<List<Solicitud>>(emptyList())
    val solicitudes: StateFlow<List<Solicitud>> get() = _solicitudes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    // URL de tu CSV (para la lectura)
    // Aunque ahora leeremos con la API, mantengo la URL por si acaso o para referencia.
    private val csvUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vSXWcbMwf9FPU4PId68Znb3sMl9aVBI57K9VkZtu-q_RugNOb2wbL939ARsmo50BnFp12J1r_CFw0fj/pub?output=csv"

    // --- Configuración para Google Sheets API ---
    // ¡TU ID DE HOJA DE CÁLCULO DE GOOGLE YA ESTÁ AQUÍ!
    // Este ID se extrajo de la URL que proporcionaste.
    private val spreadsheetId = "1G0PVUxAMSGi-SQAUvkGqqhqWNb5UR_oh0Ot7o6ljsMA"
    private var googleCredential: GoogleAccountCredential? = null
    private var sheetsService: Sheets? = null // El servicio de Sheets

    /**
     * Establece las credenciales de Google para que el ViewModel pueda usarlas
     * para interactuar con APIs de Google como Sheets.
     * @param credential Las credenciales de Google obtenidas tras el inicio de sesión.
     */
    fun setGoogleCredential(credential: GoogleAccountCredential) {
        this.googleCredential = credential
        Log.d("SolicitudesViewModel", "Credencial de Google establecida.")
        // Una vez que tienes las credenciales, inicializa el servicio de Google Sheets
        initializeSheetsService()
    }

    /**
     * Devuelve las credenciales de Google almacenadas.
     * Esta función puede mostrar una advertencia de "nunca usada" si no se llama
     * desde fuera de este ViewModel. Su propósito es ser un getter público.
     * @return Las credenciales de Google, o null si no se han establecido.
     */
    fun getGoogleCredential(): GoogleAccountCredential? {
        return googleCredential
    }

    private fun initializeSheetsService() {
        if (googleCredential != null && sheetsService == null) {
            sheetsService = Sheets.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                googleCredential
            ).setApplicationName("CAINFLockersApp").build() // Asegúrate de usar el nombre de tu app
            Log.d("SolicitudesViewModel", "Servicio de Google Sheets inicializado.")
        } else if (sheetsService != null) {
            Log.d("SolicitudesViewModel", "Servicio de Google Sheets ya inicializado.")
        } else {
            Log.e("SolicitudesViewModel", "No se puede inicializar el servicio de Sheets: credencial nula.")
        }
    }

    /**
     * Cambia el estado de una solicitud en la hoja de cálculo de Google.
     * @param timestamp El timestamp de la solicitud para identificar la fila (para actualización local).
     * @param rowNumber El número de fila en la hoja de cálculo (base 1) para la solicitud.
     * @param estadoColumnLetter La letra de la columna donde se encuentra el estado (ej. "E" para columna E).
     * @param newState El nuevo estado al que se debe cambiar la solicitud (ej. "REVISADO" o "PENDIENTE").
     */
    fun markSolicitudAsReviewed(timestamp: String, rowNumber: Int, estadoColumnLetter: String, newState: String) { // <-- ¡newState AÑADIDO AQUÍ!
        viewModelScope.launch(Dispatchers.IO) { // Ejecutar en hilo de IO para operaciones de red
            _isLoading.value = true
            _errorMessage.value = null

            if (sheetsService == null) {
                initializeSheetsService() // Intenta inicializar de nuevo si es nulo
                if (sheetsService == null) {
                    _errorMessage.value = "Error: Servicio de Google Sheets no inicializado. ¿Credenciales válidas?"
                    _isLoading.value = false
                    Log.e("SolicitudesViewModel", "Servicio de Sheets nulo al intentar actualizar.")
                    return@launch
                }
            }

            try {
                // El rango de la celda a actualizar, por ejemplo, "Hoja1!E2"
                // Asumiendo que tu hoja se llama "Solicitudes" y la columna de estado es la E.
                // ¡MODIFICA "Solicitudes" si tu hoja tiene otro nombre!
                val range = "Solicitudes!$estadoColumnLetter$rowNumber"

                // El nuevo valor es el 'newState' que se pasó como parámetro
                val valueRange = ValueRange().setValues(listOf(listOf(newState)))

                Log.d("SheetsUpdate", "Intentando actualizar celda $range con '$newState'")

                val result = sheetsService?.spreadsheets()?.values()?.update(
                    spreadsheetId,
                    range,
                    valueRange
                )?.setValueInputOption("RAW")?.execute() // "RAW" inserta el valor tal cual, "USER_ENTERED" aplica formato.

                if (result != null) {
                    Log.d("SheetsUpdate", "Actualización exitosa: ${result.updatedCells} celdas actualizadas. Fila: $rowNumber")
                    // Opcional: Actualizar el StateFlow _solicitudes localmente para que la UI se refresque sin recargar todo.
                    val currentList = _solicitudes.value.toMutableList()
                    val index = currentList.indexOfFirst { it.timestamp == timestamp }
                    if (index != -1) {
                        currentList[index] = currentList[index].copy(estadoSolicitud = newState) // Usa el newState
                        _solicitudes.value = currentList
                    }
                    // Después de una actualización exitosa, recarga los datos para asegurar la consistencia.
                    fetchSolicitudesFromSheet() // <-- ¡LLAMADA A LA NUEVA FUNCIÓN DE LECTURA!

                } else {
                    _errorMessage.value = "Error: No se recibió respuesta al actualizar la hoja."
                    Log.e("SheetsUpdate", "Respuesta nula al actualizar la hoja.")
                }

            } catch (e: Exception) {
                Log.e("SheetsUpdate", "Error al marcar como revisado: ${e.message}", e)
                _errorMessage.value = "Error al actualizar estado: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    // --- ¡NUEVA FUNCIÓN PARA LEER DIRECTAMENTE DE LA HOJA DE CÁLCULO! ---
    fun fetchSolicitudesFromSheet() {
        viewModelScope.launch(Dispatchers.IO) { // Ejecutar en hilo de IO para operaciones de red
            _isLoading.value = true
            _errorMessage.value = null

            if (sheetsService == null) {
                initializeSheetsService() // Intenta inicializar de nuevo si es nulo
                if (sheetsService == null) {
                    _errorMessage.value = "Error: Servicio de Google Sheets no inicializado. ¿Credenciales válidas?"
                    _isLoading.value = false
                    Log.e("SolicitudesViewModel", "Servicio de Sheets nulo al intentar leer.")
                    return@launch
                }
            }

            try {
                // Rango para leer todos los datos desde la hoja "Solicitudes"
                // Asume que la primera fila son encabezados y los datos comienzan en la fila 2.
                // ¡MODIFICA "Solicitudes" si tu hoja tiene otro nombre!
                val range = "Solicitudes!A:Z" // Lee desde la columna A hasta la Z

                Log.d("SheetsRead", "Intentando leer datos desde: $spreadsheetId, rango: $range")

                val response = sheetsService?.spreadsheets()?.values()?.get(spreadsheetId, range)?.execute()
                val values = response?.getValues()

                if (values != null && values.isNotEmpty()) {
                    // La primera fila son los encabezados, los datos reales empiezan en la segunda fila (índice 1)
                    val lista = values.drop(1).mapIndexed { index, row -> // drop(1) para saltar los encabezados
                        // Mapea los valores de la fila a tu objeto Solicitud
                        // Asumimos un orden fijo de columnas basado en tu CSV original:
                        // Columna 0: timestamp
                        // Columna 1: numeroLocker
                        // Columna 2: nombreEstudiante
                        // Columna 3: rutEstudiante
                        // Columna 4: estadoSolicitud
                        // Asegúrate de que este orden coincida con tu hoja real.
                        Solicitud(
                            timestamp = row.getOrElse(0) { "" }.toString(),
                            numeroLocker = row.getOrElse(1) { "" }.toString(),
                            nombreEstudiante = row.getOrElse(2) { "" }.toString(),
                            rutEstudiante = row.getOrElse(3) { "" }.toString(),
                            estadoSolicitud = row.getOrElse(4) { "" }.toString(),
                            rowNumber = index + 2 // rowNumber en la hoja (base 1) = índice de la lista + 2 (por los encabezados)
                        )
                    }
                    _solicitudes.value = lista
                    Log.d("SheetsRead", "Datos de la hoja procesados exitosamente. ${lista.size} solicitudes cargadas.")
                } else {
                    _errorMessage.value = "La hoja de cálculo está vacía o no se pudieron leer los datos."
                    Log.e("SheetsRead", "La hoja de cálculo está vacía o no se pudieron leer los datos.")
                }

            } catch (e: Exception) {
                Log.e("SheetsRead", "Error al leer la hoja de cálculo: ${e.message}", e)
                _errorMessage.value = "Error al leer datos: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    // --- FIN DE LA NUEVA FUNCIÓN DE LECTURA ---


    // Renombra la función anterior si no la usas, o elimínala.
    // fun fetchSolicitudesFromCsv() { ... }
    // O simplemente haz que fetchSolicitudesFromCsv llame a fetchSolicitudesFromSheet
    fun fetchSolicitudesFromCsv() { // Mantengo el nombre por compatibilidad con ListaSolicitudesScreen
        fetchSolicitudesFromSheet()
    }
}