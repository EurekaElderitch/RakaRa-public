package dev.rakara.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {
    companion object {
        val USER_KEY = stringPreferencesKey("user_key")
        val KEY_TIER = stringPreferencesKey("key_tier")
        val KEY_EXPIRES_AT = longPreferencesKey("key_expires_at")
        val KEY_LAST_VALIDATED = longPreferencesKey("key_last_validated")
        val KEY_ACTIVATED_AT = longPreferencesKey("key_activated_at")
        val KEY_DEVICE_ID = stringPreferencesKey("key_device_id")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_PHOTO_URI = stringPreferencesKey("profile_photo_uri")
    }

    val userKey: Flow<String?> = context.dataStore.data.map { it[USER_KEY] }
    val keyTier: Flow<String?> = context.dataStore.data.map { it[KEY_TIER] }
    val keyExpiresAt: Flow<Long?> = context.dataStore.data.map { it[KEY_EXPIRES_AT] }
    val keyLastValidated: Flow<Long?> = context.dataStore.data.map { it[KEY_LAST_VALIDATED] }
    val keyActivatedAt: Flow<Long?> = context.dataStore.data.map { it[KEY_ACTIVATED_AT] }
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val profileName: Flow<String> = context.dataStore.data.map { it[PROFILE_NAME] ?: "User Raka" }
    val profilePhotoUri: Flow<String?> = context.dataStore.data.map { it[PROFILE_PHOTO_URI] }

    
    suspend fun saveUserKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_KEY] = key
            if (prefs[KEY_ACTIVATED_AT] == null) {
                prefs[KEY_ACTIVATED_AT] = System.currentTimeMillis()
            }
        }
    }

    
    suspend fun saveValidatedKey(
        key: String,
        deviceId: String,
        tier: String,
        expiresAtSeconds: Long,
    ) {
        context.dataStore.edit { prefs ->
            prefs[USER_KEY] = key
            prefs[KEY_DEVICE_ID] = deviceId
            prefs[KEY_TIER] = tier
            prefs[KEY_EXPIRES_AT] = expiresAtSeconds
            prefs[KEY_LAST_VALIDATED] = System.currentTimeMillis()
            if (prefs[KEY_ACTIVATED_AT] == null) {
                prefs[KEY_ACTIVATED_AT] = System.currentTimeMillis()
            }
        }
    }

    
    suspend fun updateLastValidated() {
        context.dataStore.edit {
            it[KEY_LAST_VALIDATED] = System.currentTimeMillis()
        }
    }

    
    suspend fun clearKey() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_KEY)
            prefs.remove(KEY_TIER)
            prefs.remove(KEY_EXPIRES_AT)
            prefs.remove(KEY_LAST_VALIDATED)
            prefs.remove(KEY_ACTIVATED_AT)
            prefs.remove(KEY_DEVICE_ID)
        }
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