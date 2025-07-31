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
import androidx.compose.foundation.layout.padding // Importar para el padding del contenido
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
import com.google.api.services.sheets.v4.SheetsScopes

// Importaciones para TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api // Para @OptIn
import androidx.compose.material3.Scaffold // Para la estructura de la pantalla
import androidx.compose.material3.TopAppBar // Para la barra superior
import androidx.compose.material3.TopAppBarDefaults // Para colores de la barra superior
import androidx.compose.ui.unit.dp // Para unidades de medida
import androidx.compose.foundation.layout.Row // Para organizar elementos en la barra superior
import androidx.compose.foundation.layout.Spacer // Para espacio flexible
import androidx.compose.foundation.layout.width // Para ancho del espaciador


class MainActivity : ComponentActivity() {

    private val viewModel: SolicitudesViewModel by viewModels()

    // Declaraciones para Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    @OptIn(ExperimentalMaterial3Api::class) // Anotación para usar TopAppBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Configuración de Google Sign-In ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
            } else {
                Log.e("MainActivity", "Fallo en el inicio de sesión con Google: ${result.resultCode}")
            }
        }
        // --- Fin de la Configuración de Google Sign-In ---

        // Carga las solicitudes desde un archivo CSV.
        viewModel.fetchSolicitudesFromCsv()

        setContent {
            CAINFLockersTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val solicitudes = viewModel.solicitudes.collectAsState()

                    val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)

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
                        val credential = GoogleAccountCredential.usingOAuth2(this, listOf(SheetsScopes.SPREADSHEETS))
                        credential.selectedAccount = lastSignedInAccount.account
                        viewModel.setGoogleCredential(credential)

                        // --- ¡ESTE ES EL BLOQUE MODIFICADO PARA INCLUIR LA NAVBAR! ---
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        titleContentColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    title = {
                                        Text("CAINFLockers") // Nombre de tu app
                                    },
                                    actions = {
                                        // Botón de Cerrar Sesión en la barra superior
                                        Button(onClick = { signOut() }) {
                                            Text("Cerrar Sesión")
                                        }
                                        Spacer(Modifier.width(8.dp)) // Espacio a la derecha del botón
                                    }
                                )
                            },
                            content = { paddingValues ->
                                Column(modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues) // Aplica el padding del Scaffold
                                ) {
                                    ListaSolicitudesScreen(viewModel) // Tu pantalla de solicitudes
                                    // El botón de cerrar sesión ya no necesita estar aquí abajo
                                }
                            }
                        )
                        // --- FIN DEL BLOQUE MODIFICADO ---
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

            val credential = GoogleAccountCredential.usingOAuth2(this, listOf(SheetsScopes.SPREADSHEETS))
            credential.selectedAccount = account.account
            viewModel.setGoogleCredential(credential)

            // --- ¡ADICIÓN CLAVE AQUÍ PARA FORZAR LA ACTUALIZACIÓN DE LA UI! ---
            // Recrea la actividad para que la UI se actualice y la lógica de onCreate se reevalúe.
            recreate()
            // --- FIN DE LA ADICIÓN ---

        } catch (e: ApiException) {
            Log.w("MainActivity", "signInResult:failed code=" + e.statusCode)
        }
    }

    private fun signOut() {
        googleSignInClient.signOut()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "Sesión de Google cerrada exitosamente.")
                    recreate()
                } else {
                    Log.e("MainActivity", "Error al cerrar sesión: ${task.exception?.message}")
                }
            }
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            Log.d("MainActivity", "Usuario ya autenticado en onStart: ${account.displayName}")
        }
    }
}