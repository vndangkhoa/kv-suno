package com.kvsuno.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kvsuno.app.data.model.AuthResponse
import com.kvsuno.app.data.model.User
import com.kvsuno.app.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Local studio creator profile & cross-device preference store.
 * KV Suno operates autonomously without mandatory server authentication.
 */
class AuthRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("kv_suno_profile", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(loadSavedUser())
    val currentUser: StateFlow<User?> = _currentUser

    private val _authToken = MutableStateFlow<String?>(prefs.getString("auth_token", null))
    val authToken: StateFlow<String?> = _authToken

    private fun loadSavedUser(): User? {
        val name = prefs.getString("user_name", null)?.trim().orEmpty()
        val handle = prefs.getString("user_handle", null)?.trim().orEmpty()
        if (name.isEmpty() && handle.isEmpty()) return null
        return User(
            id = handle.ifBlank { "local_creator" },
            name = name.ifBlank { handle },
            email = if (handle.isNotBlank()) "@${handle.removePrefix("@")}" else "Autonomous Studio"
        )
    }

    fun saveCreatorProfile(name: String, handle: String): User {
        val cleanName = name.trim().ifBlank { "Suno Producer" }
        val cleanHandle = handle.trim().removePrefix("@").ifBlank { "studio" }
        prefs.edit()
            .putString("user_name", cleanName)
            .putString("user_handle", cleanHandle)
            .putString("auth_token", "local_$cleanHandle")
            .apply()
        val user = User(
            id = cleanHandle,
            name = cleanName,
            email = "@$cleanHandle"
        )
        _currentUser.value = user
        _authToken.value = "local_$cleanHandle"
        return user
    }

    suspend fun login(email: String, pass: String): Result<AuthResponse> {
        val handle = email.substringBefore("@").ifBlank { "creator" }
        val user = saveCreatorProfile(handle, handle)
        return Result.success(AuthResponse(user = user, token = _authToken.value))
    }

    suspend fun register(name: String, email: String, pass: String): Result<AuthResponse> {
        val handle = email.substringBefore("@").ifBlank { name.lowercase().replace(" ", "") }
        val user = saveCreatorProfile(name, handle)
        return Result.success(AuthResponse(user = user, token = _authToken.value))
    }

    fun logout() {
        prefs.edit().clear().apply()
        _authToken.value = null
        _currentUser.value = null
        PlayerManager.clearAllUserData()
    }
}
