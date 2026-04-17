package ananda.yoga.infinityps

import com.google.gson.annotations.SerializedName

data class MonitoringResponse(
    val data: List<PsMonitoringItem>
)

data class PsMonitoringItem(
    @SerializedName("id_ps") val idPs: Int,
    @SerializedName("nomor_ps") val nomorPs: String,
    @SerializedName("status_ps") val statusPs: String,
    val tipe: TipePs?,
    @SerializedName("active_transaksi") val activeTransaksi: TransaksiAktif?
)

data class TipePs(
    @SerializedName("id_tipe") val idTipe: Int,
    @SerializedName("nama_tipe") val namaTipe: String,
    @SerializedName("harga_sewa") val hargaSewa: Long = 0L
)

data class TransaksiAktif(
    @SerializedName("id_transaksi") val idTransaksi: Int,
    @SerializedName("status_transaksi") val statusTransaksi: String? = null,
    @SerializedName("detail_sewa") val detailSewa: List<DetailSewa> = emptyList(),
    @SerializedName("detail_produk") val detailProduk: List<DetailProduk> = emptyList()
)

data class DetailSewa(
    @SerializedName("id_detail_sewa") val idDetailSewa: Int,
    @SerializedName("id_ps") val idPs: Int,
    @SerializedName("jam_mulai") val jamMulai: String,
    @SerializedName("jam_selesai") val jamSelesai: String?,
    @SerializedName("durasi_menit") val durasiMenit: Int?,
    @SerializedName("sisa_detik") val sisaDetik: Long = 0L
)

data class DetailProduk(
    @SerializedName("id_detail_produk") val idDetailProduk: Int,
    val qty: Int,
    val produk: Produk
)

data class Produk(
    @SerializedName("id_produk") val idProduk: Int,
    @SerializedName("nama_produk") val namaProduk: String,
    val harga: Long
)