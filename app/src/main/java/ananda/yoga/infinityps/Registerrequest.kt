package ananda.yoga.infinityps

import android.adservices.ondevicepersonalization.UserData

data class RegisterRequest(
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val password_confirmation: String,
    val role: String = "pelanggan"
)

data class RegisterResponse(
    val message: String,
    val user: UserData,
    val token: String
)