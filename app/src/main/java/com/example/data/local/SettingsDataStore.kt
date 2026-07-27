package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "codeide_settings")

data class SettingsState(
    val fontSize: Float = 14f,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val autoIndentation: Boolean = true,
    val themeMode: String = "darcula", // "darcula" or "light"
    val editorFont: String = "Monospace",
    val apiKey: String = "",
    val apiEndpoint: String = "https://api.openai.com/v1/chat/completions",
    val gitUserName: String = "Developer",
    val gitEmail: String = "developer@example.com",
    val gitToken: String = ""
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val FONT_SIZE = floatPreferencesKey("font_size")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val AUTO_INDENTATION = booleanPreferencesKey("auto_indentation")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val EDITOR_FONT = stringPreferencesKey("editor_font")
        val API_KEY = stringPreferencesKey("api_key")
        val API_ENDPOINT = stringPreferencesKey("api_endpoint")
        val GIT_USER_NAME = stringPreferencesKey("git_user_name")
        val GIT_EMAIL = stringPreferencesKey("git_email")
        val GIT_TOKEN = stringPreferencesKey("git_token")
    }

    val settingsFlow: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            fontSize = prefs[Keys.FONT_SIZE] ?: 14f,
            showLineNumbers = prefs[Keys.SHOW_LINE_NUMBERS] ?: true,
            wordWrap = prefs[Keys.WORD_WRAP] ?: false,
            autoIndentation = prefs[Keys.AUTO_INDENTATION] ?: true,
            themeMode = prefs[Keys.THEME_MODE] ?: "darcula",
            editorFont = prefs[Keys.EDITOR_FONT] ?: "Monospace",
            apiKey = prefs[Keys.API_KEY] ?: "",
            apiEndpoint = prefs[Keys.API_ENDPOINT] ?: "https://api.openai.com/v1/chat/completions",
            gitUserName = prefs[Keys.GIT_USER_NAME] ?: "Developer",
            gitEmail = prefs[Keys.GIT_EMAIL] ?: "developer@example.com",
            gitToken = prefs[Keys.GIT_TOKEN] ?: ""
        )
    }

    suspend fun updateFontSize(size: Float) {
        context.dataStore.edit { prefs -> prefs[Keys.FONT_SIZE] = size }
    }

    suspend fun updateShowLineNumbers(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.SHOW_LINE_NUMBERS] = show }
    }

    suspend fun updateWordWrap(wrap: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.WORD_WRAP] = wrap }
    }

    suspend fun updateAutoIndentation(auto: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_INDENTATION] = auto }
    }

    suspend fun updateThemeMode(theme: String) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = theme }
    }

    suspend fun updateEditorFont(font: String) {
        context.dataStore.edit { prefs -> prefs[Keys.EDITOR_FONT] = font }
    }

    suspend fun updateApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[Keys.API_KEY] = key }
    }

    suspend fun updateApiEndpoint(endpoint: String) {
        context.dataStore.edit { prefs -> prefs[Keys.API_ENDPOINT] = endpoint }
    }

    suspend fun updateGitProfile(userName: String, email: String, token: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GIT_USER_NAME] = userName
            prefs[Keys.GIT_EMAIL] = email
            prefs[Keys.GIT_TOKEN] = token
        }
    }
}
