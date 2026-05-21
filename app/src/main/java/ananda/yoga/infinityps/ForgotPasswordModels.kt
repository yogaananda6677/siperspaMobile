package ananda.yoga.infinityps

data class ForgotPasswordRequest(
    val email: String
)

data class VerifyResetOtpRequest(
    val email: String,
    val otp: String
)

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val password: String,
    val password_confirmation: String
)