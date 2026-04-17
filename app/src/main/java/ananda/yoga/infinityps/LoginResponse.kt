package ananda.yoga.infinityps

data class LoginResponse(
    val message: String,
    val token: String,
    val token_type: String,
    val user: UserResponse
)