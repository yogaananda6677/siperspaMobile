package ananda.yoga.infinityps

import com.google.gson.annotations.SerializedName

data class PengaduanListResponse(
    @SerializedName("data")
    val data: List<PengaduanItem> = emptyList(),

    @SerializedName("current_page")
    val currentPage: Int? = null,

    @SerializedName("last_page")
    val lastPage: Int? = null,

    @SerializedName("per_page")
    val perPage: Int? = null,

    @SerializedName("total")
    val total: Int? = null
)

data class PengaduanDetailResponse(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: PengaduanItem? = null
)

data class BaseMessageResponse(
    @SerializedName("message")
    val message: String? = null
)

data class PengaduanUser(
    @SerializedName("id_user")
    val idUser: Int? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("role")
    val role: String? = null
)

data class PengaduanItem(
    @SerializedName("id")
    val id: Int,

    @SerializedName("id_pengadu")
    val idPengadu: Int? = null,

    @SerializedName("id_admin")
    val idAdmin: Int? = null,

    @SerializedName("judul_pengaduan")
    val judulPengaduan: String? = null,

    @SerializedName("kategori_aduan")
    val kategoriAduan: String? = null,

    @SerializedName("isi_pengaduan")
    val isiPengaduan: String? = null,

    @SerializedName("foto_bukti")
    val fotoBukti: String? = null,

    @SerializedName("status_pengaduan")
    val statusPengaduan: String? = null,

    @SerializedName("catatan_admin")
    val catatanAdmin: String? = null,

    @SerializedName("ditangani_pada")
    val ditanganiPada: String? = null,

    @SerializedName("diselesaikan_pada")
    val diselesaikanPada: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    @SerializedName("pengadu")
    val pengadu: PengaduanUser? = null,

    @SerializedName("admin")
    val admin: PengaduanUser? = null
)

data class UpdateStatusPengaduanRequest(
    @SerializedName("status_pengaduan")
    val statusPengaduan: String,

    @SerializedName("catatan_admin")
    val catatanAdmin: String? = null
)