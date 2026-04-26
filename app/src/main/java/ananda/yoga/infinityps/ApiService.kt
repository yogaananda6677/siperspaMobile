package ananda.yoga.infinityps

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("me")
    suspend fun getMe(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json"
    ): Response<UserResponse>

    @PUT("profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json",
        @Body request: UpdateProfileRequest
    ): Response<UpdateProfileResponse>

    @PUT("user/password")
    suspend fun updatePassword(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/json",
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>

    @POST("logout")
    suspend fun logout(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json"
    ): Response<Unit>

    @POST("register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @GET("monitoring/pelanggan")
    suspend fun getMonitoring(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json"
    ): Response<MonitoringResponse>

    @GET("produk")
    suspend fun getProduk(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json"
    ): Response<ProdukListResponse>

    @POST("transaksi")
    suspend fun createTransaksi(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String,
        @Body request: CreateTransaksiRequest
    ): Response<CreateTransaksiResponse>

    @GET("transaksi-saya")
    suspend fun getTransaksiSaya(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json"
    ): Response<HistoryResponse>

    @PATCH("transaksi/{id}/tambah-waktu")
    suspend fun tambahWaktu(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json",
        @Path("id") id: Int,
        @Body request: TambahWaktuRequest
    ): Response<DetailHistoryResponse>

    @PATCH("transaksi/{id}/tambah-produk")
    suspend fun tambahProduk(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json",
        @Path("id") id: Int,
        @Body request: TambahProdukRequest
    ): Response<DetailHistoryResponse>

    @POST("transaksi/{id}/bayar")
    suspend fun bayarTransaksi(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json",
        @Path("id") idTransaksi: Int,
        @Body request: BayarRequest
    ): Response<BayarResponse>

    @POST("transaksi/{id}/payment/qris")
    suspend fun createQrisPayment(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json",
        @Path("id") idTransaksi: Int
    ): Response<QrisPaymentResponse>

    @GET("transaksi/{id}")
    suspend fun getDetailTransaksi(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json",
        @Path("id") idTransaksi: Int
    ): Response<DetailHistoryResponse>

    @GET("transaksi/{id}/payment/qris/status")
    suspend fun checkQrisPaymentStatus(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json",
        @Path("id") idTransaksi: Int
    ): Response<QrisPaymentResponse>
}