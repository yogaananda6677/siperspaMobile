package ananda.yoga.infinityps

data class ChangePasswordRequest(
    val current_password: String,
    val password: String,
    val password_confirmation: String
)

data class ChangePasswordResponse(
    val message: String
)