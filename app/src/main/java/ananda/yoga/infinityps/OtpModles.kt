package ananda.yoga.infinityps

data class VerifyOtpRequest(
    val email: String,
    val otp: String
)

data class ResendOtpRequest(
    val email: String
)

data class VerifyOtpResponse(
    val message: String,
    val user: UserResponse?
)