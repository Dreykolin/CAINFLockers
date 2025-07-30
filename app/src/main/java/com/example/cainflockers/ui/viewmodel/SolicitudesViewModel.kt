package com.example.cainflockers.ui.viewmodel

import android.util.Log // Asegúrate de que esta importación esté presente
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cainflockers.data.models.Solicitud // Asegúrate de que esta importación esté correcta
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.Dispatchers // Importa esto para usar Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow // Usaremos StateFlow para que coincida con collectAsState
import kotlinx.coroutines.flow.StateFlow // Usaremos StateFlow para que coincida con collectAsState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // Importa esto para mover la operación de red
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException // Importa esto para manejar errores de I/O

class SolicitudesViewModel : ViewModel() {

    // Usamos MutableStateFlow para que coincida con collectAsState en MainActivity
    private val _solicitudes = MutableStateFlow<List<Solicitud>>(emptyList())
    val solicitudes: StateFlow<List<Solicitud>> get() = _solicitudes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    // URL de tu CSV
    private val csvUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vSXWcbMwf9FPU4PId68Znb3sMl9aVBI57K9VkZtu-q_RugNOb2wbL939ARsmo50BnFp12J1r_CFw0fj/pub?output=csv"

    // NOTA: Hemos eliminado el 'init { fetchSolicitudesFromCsv() }' aquí
    // y la llamada en MainActivity, para que la carga se inicie solo desde
    // el LaunchedEffect en ListaSolicitudesScreen, que es lo más común y limpio.

    fun fetchSolicitudesFromCsv() {
        viewModelScope.launch { // Esto lanza la corrutina, por defecto en Dispatchers.Main
            _isLoading.value = true
            _errorMessage.value = null // Limpiar errores previos

            try {
                // *** CAMBIO CRUCIAL: Mover la operación de red a Dispatchers.IO ***
                // 'withContext(Dispatchers.IO)' asegura que la operación de red
                // no bloquee el hilo principal de la UI, solucionando NetworkOnMainThreadException.
                val csvText = withContext(Dispatchers.IO) {
                    Log.d("CSV_DOWNLOAD", "Intentando descargar CSV desde: $csvUrl")
                    val client = OkHttpClient()
                    val request = Request.Builder().url(csvUrl).build()
                    val response = client.newCall(request).execute() // La operación de red que bloquea

                    response.use { // Asegura que la respuesta se cierre correctamente
                        if (it.isSuccessful && it.body != null) {
                            it.body!!.string() // Lee el cuerpo de la respuesta como String
                        } else {
                            // Si la respuesta no es exitosa o el cuerpo es nulo, lanzamos una excepción
                            val errorBody = it.body?.string() ?: "N/A"
                            Log.e("CSV_DOWNLOAD", "Respuesta no exitosa o body nulo: código ${it.code}, body: $errorBody")
                            throw IOException("Error de red: Código ${it.code}, Mensaje: ${it.message}")
                        }
                    }
                }

                // Si llegamos aquí, el CSV se descargó correctamente.
                // Ahora lo parseamos (esto puede permanecer en el hilo principal
                // o moverse a Dispatchers.Default si el CSV es muy grande).
                if (csvText.isNotEmpty()) {
                    val rows = csvReader().readAllWithHeader(csvText)
                    val lista = rows.map { row ->
                        // *** ¡RESPETANDO TUS NOMBRES DE DATOS ORIGINALES! ***
                        Solicitud(
                            timestamp = row["timestamp"] ?: "",
                            numeroLocker = row["numeroLocker"] ?: "",
                            nombreEstudiante = row["nombreEstudiante"] ?: "",
                            rutEstudiante = row["rutEstudiante"] ?: "",
                            estadoSolicitud = row["estadoSolicitud"] ?: ""
                        )
                    }
                    _solicitudes.value = lista // Actualiza el StateFlow
                    Log.d("CSV_DOWNLOAD", "CSV procesado exitosamente. ${lista.size} solicitudes cargadas.")
                } else {
                    _errorMessage.value = "El archivo CSV está vacío o no se pudo descargar."
                    Log.e("CSV_DOWNLOAD", "El archivo CSV está vacío o no se pudo descargar.")
                }

            } catch (e: IOException) {
                // Captura errores específicos de red (IOException)
                Log.e("CSV_DOWNLOAD", "Error de red o E/S: ${e.message}", e)
                _errorMessage.value = "Error de conexión: ${e.localizedMessage ?: "Verifica tu conexión a internet."}"
            } catch (e: Exception) {
                // Captura cualquier otra excepción (ej. problemas de parseo del CSV)
                Log.e("CSV_DOWNLOAD", "Se capturó una excepción general al procesar los datos: ${e.message}", e)
                _errorMessage.value = "Hubo un problema al procesar los datos."
            } finally {
                _isLoading.value = false // Siempre desactiva el indicador de carga
            }
        }
    }
}