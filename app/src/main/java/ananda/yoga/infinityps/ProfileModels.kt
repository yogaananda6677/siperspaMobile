package ananda.yoga.infinityps

data class UpdateProfileRequest(
    val name: String,
    val username: String,
    val email: String
)

data class UpdateProfileResponse(
    val message: String,
    val user: UserResponse
)