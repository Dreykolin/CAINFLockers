// preferencesManager.kt
package com.example.cainflockers.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

// Creamos un DataStore a nivel de aplicación (singleton)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    // Definimos la clave para el ID del Spreadsheet
    companion object {
        val SPREADSHEET_ID_KEY = stringPreferencesKey("spreadsheet_id")
    }

    // Leemos el ID del Spreadsheet del DataStore
    val spreadsheetId = context.dataStore.data
        .map { preferences ->
            preferences[SPREADSHEET_ID_KEY]
        }

    // Escribimos el ID del Spreadsheet en el DataStore
    suspend fun saveSpreadsheetId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[SPREADSHEET_ID_KEY] = id
        }
    }
}