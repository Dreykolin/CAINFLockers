package com.example.cainflockers

import android.os.Bundle
import android.content.Intent // Importar Intent
import android.util.Log // Importar Log para los mensajes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.ActivityResultLauncher // Importar ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts // Importar ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button // Importar Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text // Importar Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.cainflockers.ui.theme.CAINFLockersTheme
import com.example.cainflockers.ui.viewmodel.SolicitudesViewModel
import com.example.cainflockers.ui.screens.ListaSolicitudesScreen
import com.google.android.gms.common.api.Scope
// Importaciones para Google Sign-In
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

// Importaciones para Google Sheets API (no se usan directamente aquí, pero necesarias para las credenciales)
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.sheets.v4.SheetsScopes // Para los permisos de Sheets

class MainActivity : ComponentActivity() {

    private val viewModel: SolicitudesViewModel by viewModels()

    // Declaraciones para Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Configuración de Google Sign-In ---
        // 1. Configura las opciones de inicio de sesión de Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Solicita el token de ID (si lo necesitas para tu backend)
            // .requestIdToken(getString(R.string.default_web_client_id))

            // ¡Importante! Solicita los ámbitos (scopes) para acceder a Google Sheets
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))

            // ELIMINA O COMENTA ESTA LÍNEA QUE CAUSA EL ERROR:
            // .requestScopes(Scope(SheetsScopes.DRIVE_FILE), Scope(SheetsScopes.DRIVE_APPDATA)) // EJEMPLO: Si también necesitas Drive

            .requestEmail() // También puedes pedir el email del usuario
            .build()

        // 2. Crea el cliente de Google Sign-In
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 3. Configura el lanzador para el resultado del inicio de sesión
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
            } else {
                Log.e("MainActivity", "Fallo en el inicio de sesión con Google: ${result.resultCode}")
                // Aquí puedes mostrar un mensaje al usuario, por ejemplo, un Toast
            }
        }
        // --- Fin de la Configuración de Google Sign-In ---

        // Carga las solicitudes desde un archivo CSV.
        viewModel.fetchSolicitudesFromCsv()

        setContent {
            CAINFLockersTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val solicitudes = viewModel.solicitudes.collectAsState()

                    // Condicional para mostrar la pantalla de inicio de sesión o la lista de solicitudes
                    val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)

                    // --- ¡CAMBIO CLAVE AQUÍ! ---
                    // Verifica si no hay una cuenta, O si la cuenta no tiene los permisos necesarios.
                    if (lastSignedInAccount == null || !GoogleSignIn.hasPermissions(
                            lastSignedInAccount, // La cuenta que estamos verificando
                            Scope(SheetsScopes.SPREADSHEETS) // Los permisos específicos que necesitamos
                        )) {
                        // Si no hay una cuenta VÁLIDA con los permisos, muestra la pantalla de inicio de sesión.
                        Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Por favor, inicia sesión para continuar.")
                        Button(onClick = { signIn() }) {
                            Text("Iniciar Sesión con Google")
                        }
                    }
                } else {
                // Si el usuario ya está autenticado, pasa la credencial al ViewModel
                // para que pueda interactuar con la Sheets API
                val credential = GoogleAccountCredential.usingOAuth2(this, listOf(SheetsScopes.SPREADSHEETS))
                credential.selectedAccount = lastSignedInAccount.account
                viewModel.setGoogleCredential(credential) // Asumiendo que añades esta función a tu ViewModel

                // Muestra la pantalla principal de la aplicación.
                ListaSolicitudesScreen(viewModel)
            }
            }
        }
    }
}

// Función para iniciar el flujo de inicio de sesión con Google
private fun signIn() {
    val signInIntent = googleSignInClient.signInIntent
    googleSignInLauncher.launch(signInIntent)
}

// Función para manejar el resultado del inicio de sesión
private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
    try {
        val account = completedTask.getResult(ApiException::class.java)
        Log.d("MainActivity", "Inicio de sesión exitoso. ID de Google: ${account.id}")

        // Autenticación exitosa. Ahora tienes la cuenta de Google.
        // Pasa la credencial al ViewModel para que la use con la Sheets API.
        val credential = GoogleAccountCredential.usingOAuth2(this, listOf(SheetsScopes.SPREADSHEETS))
        credential.selectedAccount = account.account
        viewModel.setGoogleCredential(credential) // ¡Necesitarás añadir esta función en SolicitudesViewModel!

        // Fuerza la recomposición de la UI para que se muestre ListaSolicitudesScreen
        setContent {
            CAINFLockersTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ListaSolicitudesScreen(viewModel)
                }
            }
        }

    } catch (e: ApiException) {
        // El inicio de sesión falló. Mira el código de estado para más detalles.
        Log.w("MainActivity", "signInResult:failed code=" + e.statusCode)
        // Puedes mostrar un mensaje de error al usuario
        // Por ejemplo: Toast.makeText(this, "Error al iniciar sesión: ${e.statusCode}", Toast.LENGTH_LONG).show()
    }
}

// (Opcional) Verifica si el usuario ya inició sesión al iniciar la app
override fun onStart() {
    super.onStart()
    val account = GoogleSignIn.getLastSignedInAccount(this)
    if (account != null) {
        // Si el usuario ya está autenticado, no necesitas hacer nada aquí,
        // el setContent en onCreate ya lo manejará.
        Log.d("MainActivity", "Usuario ya autenticado en onStart: ${account.displayName}")
    }
}
}