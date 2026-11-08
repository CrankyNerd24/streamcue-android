package com.streamcueapp.streamcue.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.streamcueapp.streamcue.model.StreamingService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** The services you pay for — the Android analog of iOS's
 * @AppStorage(Subscriptions.key), via DataStore instead of UserDefaults. */
class SubscriptionsStore(context: Context) {
    private val dataStore = context.dataStore
    private val key = stringPreferencesKey("my_subscriptions")

    val services: Flow<List<StreamingService>> = dataStore.data.map { prefs ->
        prefs[key]?.let { raw ->
            runCatching { Json.decodeFromString<List<StreamingService>>(raw) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun save(services: List<StreamingService>) {
        dataStore.edit { prefs -> prefs[key] = Json.encodeToString(services) }
    }
}
