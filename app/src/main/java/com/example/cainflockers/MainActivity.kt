package com.example.cainflockers

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cainflockers.ui.theme.CAINFLockersTheme
import com.example.cainflockers.ui.viewmodel.SolicitudesViewModel
import com.example.cainflockers.ui.screens.ListaSolicitudesScreen
import com.google.android.gms.common.api.Scope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.firebase.messaging.FirebaseMessaging
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private val viewModel: SolicitudesViewModel by viewModels()

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

        viewModel.fetchSolicitudesFromCsv()

        setContent {
            CAINFLockersTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val isLoading by viewModel.isLoading.collectAsState()
                    val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)

                    val coroutineScope = rememberCoroutineScope()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                    if (lastSignedInAccount == null || !GoogleSignIn.hasPermissions(
                            lastSignedInAccount,
                            Scope(SheetsScopes.SPREADSHEETS)
                        )) {
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
                        val credential = GoogleAccountCredential.usingOAuth2(this, listOf(SheetsScopes.SPREADSHEETS))
                        credential.selectedAccount = lastSignedInAccount.account
                        viewModel.setGoogleCredential(credential)

                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                ModalDrawerSheet {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Aquí puedes poner el título o logo de la app si quieres
                                        Text(
                                            text = "Ajustes",
                                            style = MaterialTheme.typography.titleLarge,
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Divider()
                                        Spacer(Modifier.height(16.dp))

                                        // Botón de "Cerrar Sesión" sin el estilo de botón feo
                                        Text(
                                            text = "Cerrar Sesión",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { signOut() } // ¡Aquí se llama a signOut!
                                                .padding(16.dp)
                                        )
                                    }
                                }
                            }
                        ) {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            titleContentColor = MaterialTheme.colorScheme.primary,
                                        ),
                                        title = {}, // El título se deja vacío
                                        actions = {
                                            // Botón de refrescar a la izquierda
                                            IconButton(
                                                onClick = { viewModel.fetchSolicitudesFromCsv() },
                                                enabled = !isLoading
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Refrescar"
                                                )
                                            }
                                        },
                                        // Botón para abrir el slider
                                        navigationIcon = {
                                            Row(
                                                modifier = Modifier.clickable {
                                                    coroutineScope.launch {
                                                        drawerState.open()
                                                    }
                                                }
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val displayName = lastSignedInAccount.displayName ?: ""
                                                val photoUrl = lastSignedInAccount.photoUrl?.toString()

                                                if (photoUrl != null) {
                                                    AsyncImage(
                                                        model = photoUrl,
                                                        contentDescription = "Perfil de Google",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                    )
                                                } else {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_launcher_foreground), // Icono por defecto
                                                        contentDescription = "Perfil de Google",
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                    )
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Text(displayName, style = MaterialTheme.typography.titleMedium)
                                            }
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