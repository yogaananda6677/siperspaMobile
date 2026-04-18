package ananda.yoga.infinityps

import com.google.gson.annotations.SerializedName

data class HistoryResponse(
    val data: List<HistoryItem>
)

data class HistoryItem(
    @SerializedName("id_transaksi") val idTransaksi: Int,
    val tanggal: String,
    @SerializedName("total_harga") val totalHarga: Double,
    @SerializedName("status_transaksi") val statusTransaksi: String,
    @SerializedName("sumber_transaksi") val sumberTransaksi: String,
    val user: HistoryUser?,
    @SerializedName("detail_sewa") val detailSewa: List<HistoryDetailSewa> = emptyList(),
    @SerializedName("detail_produk") val detailProduk: List<HistoryDetailProduk> = emptyList(),
    val pembayaran: HistoryPembayaran?
)

data class HistoryUser(
    @SerializedName("id_user") val idUser: Int,
    val name: String,
    val username: String,
    val email: String
)

data class HistoryDetailSewa(
    @SerializedName("id_dt_booking") val idDtBooking: Int,
    @SerializedName("id_ps") val idPs: Int,
    @SerializedName("jam_mulai") val jamMulai: String,
    @SerializedName("jam_selesai") val jamSelesai: String?,
    @SerializedName("durasi_menit") val durasiMenit: Int?,
    val subtotal: Double?,
    val playstation: HistoryPlaystation?
)

data class HistoryPlaystation(
    @SerializedName("id_ps") val idPs: Int,
    @SerializedName("nomor_ps") val nomorPs: String,
    @SerializedName("status_ps") val statusPs: String,
    val tipe: HistoryTipePs?
)

data class HistoryTipePs(
    @SerializedName("id_tipe") val idTipe: Int,
    @SerializedName("nama_tipe") val namaTipe: String
)

data class HistoryDetailProduk(
    @SerializedName("id_detail_produk") val idDetailProduk: Int,
    val qty: Int,
    val subtotal: Double?,
    val produk: HistoryProduk?
)

data class HistoryProduk(
    @SerializedName("id_produk") val idProduk: Int,
    val nama: String,
    val harga: Long
)

data class HistoryPembayaran(
    @SerializedName("id_pembayaran") val idPembayaran: Int,
    @SerializedName("metode_pembayaran") val metodePembayaran: String?,
    @SerializedName("total_bayar") val totalBayar: Double?,
    val kembalian: Double?,
    @SerializedName("waktu_bayar") val waktuBayar: String?,
    @SerializedName("status_bayar") val statusBayar: String
)

data class BayarRequest(
    @SerializedName("metode_pembayaran") val metodePembayaran: String,
    @SerializedName("total_bayar") val totalBayar: Double? = null
)

data class BayarResponse(
    val message: String,
    val data: HistoryItem?
)

data class DetailHistoryResponse(
    val data: HistoryItem
)

data class TambahWaktuRequest(
    @SerializedName("id_ps") val idPs: Int? = null,
    @SerializedName("menit_tambahan") val menitTambahan: Int
)

data class TambahProdukRequest(
    val produk: List<TambahProdukItem>
)

data class TambahProdukItem(
    @SerializedName("id_produk") val idProduk: Int,
    val qty: Int
)