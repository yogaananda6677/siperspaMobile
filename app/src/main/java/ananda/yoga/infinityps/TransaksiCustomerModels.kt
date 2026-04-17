package ananda.yoga.infinityps

import com.google.gson.annotations.SerializedName

data class ProdukListResponse(
    val data: List<CustomerProduk>
)

data class CustomerProduk(
    @SerializedName("id_produk") val idProduk: Int,
    val nama: String,
    val jenis: String,
    val harga: Long,
    val stock: Int
)

data class CreateTransaksiRequest(
    @SerializedName("id_user") val idUser: Int,
    @SerializedName("sumber_transaksi") val sumberTransaksi: String = "aplikasi",
    val sewa: List<SewaRequest>? = null,
    val produk: List<ProdukRequest>? = null
)

data class SewaRequest(
    @SerializedName("id_ps") val idPs: Int,
    @SerializedName("jam_mulai") val jamMulai: String,
    @SerializedName("durasi_menit") val durasiMenit: Int
)

data class ProdukRequest(
    @SerializedName("id_produk") val idProduk: Int,
    val qty: Int
)

data class CreateTransaksiResponse(
    val message: String,
    val data: CreatedTransaksiData?
)

data class CreatedTransaksiData(
    @SerializedName("id_transaksi") val idTransaksi: Int,
    @SerializedName("status_transaksi") val statusTransaksi: String,
    @SerializedName("total_harga") val totalHarga: Long
)

data class CartProdukItem(
    val produk: CustomerProduk,
    var qty: Int = 0
)