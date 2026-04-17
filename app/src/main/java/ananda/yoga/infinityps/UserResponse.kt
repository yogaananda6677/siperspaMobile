package ananda.yoga.infinityps

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id_user") val idUser: Int,
    val name: String,
    val email: String?,
    val username : String?,
    val role: String?
)