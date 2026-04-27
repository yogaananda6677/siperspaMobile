package ananda.yoga.infinityps

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DetailPengaduanActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvJudul: TextView
    private lateinit var tvKategori: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvTanggal: TextView
    private lateinit var tvIsi: TextView
    private lateinit var tvCatatanAdmin: TextView
    private lateinit var tvAdmin: TextView
    private lateinit var tvDitangani: TextView
    private lateinit var tvSelesai: TextView
    private lateinit var tvFotoBukti: TextView

    private var idPengaduan: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_pengaduan)

        bindViews()
        readIntent()
        setupActions()
        loadDetail()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBarDetailPengaduan)
        tvJudul = findViewById(R.id.tvDetailJudulPengaduan)
        tvKategori = findViewById(R.id.tvDetailKategoriPengaduan)
        tvStatus = findViewById(R.id.tvDetailStatusPengaduan)
        tvTanggal = findViewById(R.id.tvDetailTanggalPengaduan)
        tvIsi = findViewById(R.id.tvDetailIsiPengaduan)
        tvCatatanAdmin = findViewById(R.id.tvDetailCatatanAdmin)
        tvAdmin = findViewById(R.id.tvDetailAdmin)
        tvDitangani = findViewById(R.id.tvDetailDitangani)
        tvSelesai = findViewById(R.id.tvDetailSelesai)
        tvFotoBukti = findViewById(R.id.tvDetailFotoBukti)
    }

    private fun readIntent() {
        idPengaduan = intent.getIntExtra("id_pengaduan", 0)
    }

    private fun setupActions() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadDetail() {
        if (idPengaduan == 0) {
            Toast.makeText(this, "ID pengaduan tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val token = getToken()

                val response = RetrofitClient.apiService.getDetailPengaduan(
                    "Bearer $token",
                    "application/json",
                    idPengaduan
                )

                if (response.isSuccessful) {
                    val item = response.body()?.data

                    if (item != null) {
                        renderDetail(item)
                    } else {
                        Toast.makeText(
                            this@DetailPengaduanActivity,
                            "Data pengaduan tidak ditemukan",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@DetailPengaduanActivity,
                        "Gagal memuat detail: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@DetailPengaduanActivity,
                    "Gagal terhubung ke server: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun renderDetail(item: PengaduanItem) {
        tvJudul.text = item.judulPengaduan ?: "-"
        tvKategori.text = kategoriLabel(item.kategoriAduan)
        tvStatus.text = statusLabel(item.statusPengaduan)
        tvTanggal.text = item.createdAt ?: "-"
        tvIsi.text = item.isiPengaduan ?: "-"
        tvCatatanAdmin.text = item.catatanAdmin ?: "Belum ada catatan dari admin."
        tvAdmin.text = item.admin?.name ?: "Belum ditangani"
        tvDitangani.text = item.ditanganiPada ?: "-"
        tvSelesai.text = item.diselesaikanPada ?: "-"
        tvFotoBukti.text = if (item.fotoBukti.isNullOrBlank()) {
            "Tidak ada foto bukti."
        } else {
            "Foto bukti: ${item.fotoBukti}"
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnBack.isEnabled = !isLoading
    }

    private fun getToken(): String {
        val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
        return prefs.getString("token", "") ?: ""
    }

    private fun kategoriLabel(value: String?): String {
        return when (value) {
            "ps_rusak" -> "PS Rusak"
            "pelayanan" -> "Pelayanan"
            "kebersihan" -> "Kebersihan"
            "pembayaran" -> "Pembayaran"
            "fasilitas" -> "Fasilitas"
            "lainnya" -> "Lainnya"
            else -> "-"
        }
    }

    private fun statusLabel(value: String?): String {
        return when (value) {
            "pending" -> "Pending"
            "proses" -> "Diproses"
            "selesai" -> "Selesai"
            "dibatalkan" -> "Dibatalkan"
            else -> "-"
        }
    }
}