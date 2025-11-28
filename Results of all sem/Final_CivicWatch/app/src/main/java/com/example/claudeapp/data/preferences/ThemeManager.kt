package com.example.claudeapp.data.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    private val themePreferences: ThemePreferences
) {
    private val _isDarkTheme = MutableStateFlow(themePreferences.isDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()
    
    private val _themeMode = MutableStateFlow(themePreferences.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()
    
    fun updateTheme() {
        _isDarkTheme.value = themePreferences.isDarkTheme()
        _themeMode.value = themePreferences.getThemeMode()
    }
    
    fun setThemeMode(mode: String) {
        themePreferences.setThemeMode(mode)
        updateTheme()
    }
    
    fun toggleTheme() {
        val currentMode = _themeMode.value
        val newMode = when (currentMode) {
            ThemePreferences.THEME_LIGHT -> ThemePreferences.THEME_DARK
            ThemePreferences.THEME_DARK -> ThemePreferences.THEME_SYSTEM
            else -> ThemePreferences.THEME_LIGHT
        }
        setThemeMode(newMode)
    }
}

@Composable
fun rememberThemeState(themeManager: ThemeManager): ThemeState {
    val isDarkTheme by themeManager.isDarkTheme.collectAsState()
    val themeMode by themeManager.themeMode.collectAsState()
    
    return ThemeState(
        isDarkTheme = isDarkTheme,
        themeMode = themeMode,
        toggleTheme = { themeManager.toggleTheme() },
        setThemeMode = { themeManager.setThemeMode(it) }
    )
}

data class ThemeState(
    val isDarkTheme: Boolean,
    val themeMode: String,
    val toggleTheme: () -> Unit,
    val setThemeMode: (String) -> Unit
)
