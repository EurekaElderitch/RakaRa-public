package dev.rakara.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {
    companion object {
        val USER_KEY = stringPreferencesKey("user_key")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_PHOTO_URI = stringPreferencesKey("profile_photo_uri")
    }

    val userKey: Flow<String?> = context.dataStore.data.map { it[USER_KEY] }
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val profileName: Flow<String> = context.dataStore.data.map { it[PROFILE_NAME] ?: "User Raka" }
    val profilePhotoUri: Flow<String?> = context.dataStore.data.map { it[PROFILE_PHOTO_URI] }

    suspend fun saveUserKey(key: String) {
        context.dataStore.edit { it[USER_KEY] = key }
    }
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }
    suspend fun saveProfile(name: String, photoUri: String?) {
        context.dataStore.edit { prefs ->
            prefs[PROFILE_NAME] = name
            if (photoUri != null) prefs[PROFILE_PHOTO_URI] = photoUri
            else prefs.remove(PROFILE_PHOTO_URI)
        }
    }
}