package com.example.cainflockers.ui.screens



import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.material3.*

import androidx.compose.runtime.Composable

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp

import com.example.cainflockers.data.models.Solicitud

import com.example.cainflockers.ui.viewmodel.SolicitudesViewModel


@Composable

fun ListaSolicitudesScreen(viewModel: SolicitudesViewModel) {

    val solicitudes = viewModel.solicitudes.collectAsState()

    val isLoading = viewModel.isLoading.collectAsState()

    val errorMessage = viewModel.errorMessage.collectAsState()


    LaunchedEffect(Unit) {

        viewModel.fetchSolicitudesFromCsv()

    }


    Column(modifier = Modifier.padding(16.dp)) {

        Text("Solicitudes", style = MaterialTheme.typography.headlineMedium)


        when {

            isLoading.value -> {

                CircularProgressIndicator(modifier = Modifier.padding(16.dp))

            }

            errorMessage.value != null -> {

                Text(

                    text = "Error: ${errorMessage.value}",

                    color = MaterialTheme.colorScheme.error,

                    modifier = Modifier.padding(16.dp)

                )

            }

            solicitudes.value.isEmpty() -> {

                Text("No hay solicitudes disponibles", modifier = Modifier.padding(16.dp))

            }

            else -> {

                LazyColumn {

                    items(solicitudes.value) { solicitud ->

                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {

                            Column(modifier = Modifier.padding(16.dp)) {

                                Text("Fecha: ${solicitud.timestamp}")

                                Text("Locker: ${solicitud.numeroLocker}")

                                Text("Estudiante: ${solicitud.nombreEstudiante}")

                                Text("RUT: ${solicitud.rutEstudiante}")

                                Text("Estado: ${solicitud.estadoSolicitud}")

                            }

                        }

                    }

                }

            }

        }

    }

}