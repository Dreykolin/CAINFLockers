package com.example.cainflockers
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.example.cainflockers.ui.theme.CAINFLockersTheme
import com.example.cainflockers.ui.viewmodel.SolicitudesViewModel
import com.example.cainflockers.ui.screens.ListaSolicitudesScreen

class MainActivity : ComponentActivity() {

    // Instancia de SolicitudesViewModel para manejar la lógica y los datos.
    // 'by viewModels()' asegura que el ViewModel sobrevive a cambios de configuración.
    private val viewModel: SolicitudesViewModel by viewModels()

    // Método que se llama cuando la actividad se crea por primera vez.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Carga las solicitudes desde un archivo CSV.
        // Esto se hace al inicio de la actividad.
        viewModel.fetchSolicitudesFromCsv()

        // Define la interfaz de usuario de la actividad usando Jetpack Compose.
        setContent {
            // Aplica el tema de tu aplicación.
            CAINFLockersTheme {
                // Un contenedor de superficie que ocupa todo el espacio disponible.
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Recopila el estado de las solicitudes del ViewModel.
                    // 'collectAsState()' convierte el Flow en un State que Compose puede observar.
                    val solicitudes = viewModel.solicitudes.collectAsState()

                    // Muestra la pantalla de la lista de solicitudes, pasándole el ViewModel.
                    // Asume que ListaSolicitudesScreen está diseñada para trabajar con SolicitudesViewModel.
                    ListaSolicitudesScreen(viewModel)
                }
            }
        }
    }
}