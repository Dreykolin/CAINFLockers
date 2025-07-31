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

     * Marca una solicitud como "revisada" en la hoja de cálculo de Google.

     * @param timestamp El timestamp de la solicitud para identificar la fila (para actualización local).

     * @param rowNumber El número de fila en la hoja de cálculo (base 1) para la solicitud.

     * @param estadoColumnLetter La letra de la columna donde se encuentra el estado (ej. "E" para columna E).

     */

    fun markSolicitudAsReviewed(timestamp: String, rowNumber: Int, estadoColumnLetter: String) {

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


// El nuevo valor

                val newValue = "REVISADO"

                val valueRange = ValueRange().setValues(listOf(listOf(newValue)))


                Log.d("SheetsUpdate", "Intentando actualizar celda $range con '$newValue'")


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

                        currentList[index] = currentList[index].copy(estadoSolicitud = newValue)

                        _solicitudes.value = currentList

                    }


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

                    val lista = rows.mapIndexed { index, row -> // Usamos mapIndexed para obtener el índice de fila

// ¡IMPORTANTE! Las hojas de cálculo tienen índice base 1.

// Si tu primera fila (index 0 de la lista) corresponde a la fila 2 de la hoja

// (porque la fila 1 son encabezados), entonces el rowNumber será (index + 2).

                        Solicitud(

                            timestamp = row["timestamp"] ?: "",

                            numeroLocker = row["numeroLocker"] ?: "",

                            nombreEstudiante = row["nombreEstudiante"] ?: "",

                            rutEstudiante = row["rutEstudiante"] ?: "",

                            estadoSolicitud = row["estadoSolicitud"] ?: "",

                            rowNumber = index + 2 // Esto calcula el número de fila en la hoja de cálculo.

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