package com.example.cainflockers
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.content.Intent
import android.net.Uri // Importar Uri
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.google.firebase.messaging.FirebaseMessaging

import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource // <-- Y esta

import androidx.compose.foundation.layout.size // <-- Y esta

import androidx.compose.foundation.Image // <-- Agrega esta
class MainActivity : ComponentActivity() {

    private val viewModel: SolicitudesViewModel by viewModels()

    // Declaraciones para Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG = "MainActivity"
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d(TAG, "Permiso de notificaciones concedido")
                suscribirseATopic()
            } else {
                Log.d(TAG, "Permiso de notificaciones DENEGADO")
            }
        }
    private fun suscribirseATopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("new_requests")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Suscrito al topic 'new_requests'")
                } else {
                    Log.e(TAG, "Error al suscribirse al topic", task.exception)
                }
            }
    }

    @OptIn(ExperimentalMaterial3Api::class)
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
                Log.e(TAG, "Fallo en el inicio de sesión con Google: ${result.resultCode}")
            }
        }
        // --- Fin de la Configuración de Google Sign-In ---

        // Suscribirse al topic para recibir notificaciones de solicitudes
        FirebaseMessaging.getInstance().subscribeToTopic("new_requests")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Suscrito al topic 'new_requests'")
                } else {
                    Log.e(TAG, "Error al suscribirse al topic", task.exception)
                }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                suscribirseATopic()
            }
        } else {
            suscribirseATopic()
        }
        // Carga las solicitudes desde un archivo CSV (ahora leerá de la hoja de cálculo).
        viewModel.fetchSolicitudesFromCsv()

        // UI con Compose

        setContent {
            CAINFLockersTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isLoading by viewModel.isLoading.collectAsState()
                    val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)

                    if (lastSignedInAccount == null || !GoogleSignIn.hasPermissions(
                            lastSignedInAccount,
                            Scope(SheetsScopes.SPREADSHEETS)
                        )
                    ) {
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
                        val credential = GoogleAccountCredential.usingOAuth2(
                            this,
                            listOf(SheetsScopes.SPREADSHEETS)
                        )
                        credential.selectedAccount = lastSignedInAccount.account
                        viewModel.setGoogleCredential(credential)

                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        titleContentColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    title = {
                                        // Usamos un Row para colocar los elementos horizontalmente
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically // Centra el logo y el texto verticalmente
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_launcher_cainflockers_logo), // <-- ¡Cambia ic_launcher_foreground!
                                                contentDescription = "Logo de CAINFLockers",
                                                modifier = Modifier.size(52.dp) // Ajusta el tamaño del logo
                                            )
                                            Spacer(modifier = Modifier.width(8.dp)) // Espacio entre el logo y el texto
                                            Text("CAINFLockers")
                                        }
                                    },
                                    actions = {
                                        // ¡Botón de refrescar aquí!
                                        IconButton(
                                            onClick = { viewModel.fetchSolicitudesFromCsv() },
                                            enabled = !isLoading
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refrescar"
                                            )
                                        }
                                        // Botón de cerrar sesión
                                        Button(onClick = { signOut() }) {
                                            Text("Cerrar Sesión")
                                        }
                                        Spacer(Modifier.width(12.dp))
                                    }
                                )
                            },
                            content = { paddingValues ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                                ) {
                                    ListaSolicitudesScreen(
                                        viewModel = viewModel,
                                        onViewReceiptClick = { url -> openUrlInBrowser(url) }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "Inicio de sesión exitoso. ID de Google: ${account.id}")

            val credential = GoogleAccountCredential.usingOAuth2(this, listOf(SheetsScopes.SPREADSHEETS))
            credential.selectedAccount = account.account
            viewModel.setGoogleCredential(credential)

            recreate()

        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
        }
    }

    private fun signOut() {
        googleSignInClient.signOut()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Sesión de Google cerrada exitosamente.")
                    recreate()
                } else {
                    Log.e(TAG, "Error al cerrar sesión: ${task.exception?.message}")
                }
            }
    }

    private fun openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            Log.d(TAG, "Abriendo URL: $url")
        } catch (e: Exception) {
            Log.e(TAG, "Error al intentar abrir URL: $url", e)
        }
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            Log.d(TAG, "Usuario ya autenticado en onStart: ${account.displayName}")
        }
    }
}