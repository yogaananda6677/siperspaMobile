package ananda.yoga.infinityps

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class DetailTransaksiActivity : AppCompatActivity() {

    private lateinit var tvId: TextView
    private lateinit var tvStatusTransaksi: TextView
    private lateinit var tvStatusBayar: TextView
    private lateinit var tvTanggal: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvSewa: TextView
    private lateinit var tvProduk: TextView

    private lateinit var btnBayar: Button
    private lateinit var btnTambahWaktu: Button
    private lateinit var btnTambahProduk: Button

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout
    private lateinit var layoutActionButtons: LinearLayout
    private lateinit var layoutSecondaryActions: LinearLayout

    private var idTransaksi: Int = 0
    private var currentData: HistoryItem? = null

    companion object {
        private const val TAG = "DETAIL_TRANSAKSI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_transaksi)

        idTransaksi = intent.getIntExtra("id_transaksi", 0)

        tvId = findViewById(R.id.tvIdTransaksi)
        tvStatusTransaksi = findViewById(R.id.tvStatusTransaksi)
        tvStatusBayar = findViewById(R.id.tvStatusBayar)
        tvTanggal = findViewById(R.id.tvTanggal)
        tvTotal = findViewById(R.id.tvTotal)
        tvSewa = findViewById(R.id.tvDetailSewa)
        tvProduk = findViewById(R.id.tvDetailProduk)

        btnBayar = findViewById(R.id.btnBayar)
        btnTambahWaktu = findViewById(R.id.btnTambahWaktu)
        btnTambahProduk = findViewById(R.id.btnTambahProduk)

        progressBar = findViewById(R.id.progressBar)
        contentLayout = findViewById(R.id.contentLayout)
        layoutActionButtons = findViewById(R.id.layoutActionButtons)
        layoutSecondaryActions = findViewById(R.id.layoutSecondaryActions)

        btnBayar.setOnClickListener {
            val data = currentData ?: return@setOnClickListener
            val intent = Intent(this, PembayaranActivity::class.java)
            intent.putExtra("id_transaksi", data.idTransaksi)
            intent.putExtra("total_harga", data.totalHarga)
            startActivity(intent)
        }

        btnTambahWaktu.setOnClickListener {
            val data = currentData ?: return@setOnClickListener
            val detailSewa = data.detailSewa.firstOrNull()
            val psId = detailSewa?.idPs ?: 0
            val nomorPs = detailSewa?.playstation?.nomorPs ?: "-"
            val namaTipe = detailSewa?.playstation?.tipe?.namaTipe ?: "-"
            val hargaSewa = 0L

            val intent = Intent(this, TambahWaktuActivity::class.java)
            intent.putExtra("id_transaksi", data.idTransaksi)
            intent.putExtra("id_ps", psId)
            intent.putExtra("nomor_ps", nomorPs)
            intent.putExtra("nama_tipe", namaTipe)
            intent.putExtra("harga_sewa", hargaSewa)
            startActivity(intent)
        }

        btnTambahProduk.setOnClickListener {
            val data = currentData ?: return@setOnClickListener
            val detailSewa = data.detailSewa.firstOrNull()
            val nomorPs = detailSewa?.playstation?.nomorPs ?: "-"
            val namaTipe = detailSewa?.playstation?.tipe?.namaTipe ?: "-"

            val intent = Intent(this, TambahProdukActivity::class.java)
            intent.putExtra("id_transaksi", data.idTransaksi)
            intent.putExtra("nomor_ps", nomorPs)
            intent.putExtra("nama_tipe", namaTipe)
            startActivity(intent)
        }

        fetchDetail()
    }

    override fun onResume() {
        super.onResume()
        fetchDetail()
    }

    private fun fetchDetail() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.getDetailTransaksi(
                    "Bearer $token",
                    "application/json",
                    idTransaksi
                )

                Log.d(TAG, "response code = ${response.code()}")
                Log.d(TAG, "response message = ${response.message()}")

                if (response.isSuccessful) {
                    val item = response.body()?.data
                    if (item != null) {
                        currentData = item
                        bindData(item)
                    } else {
                        Toast.makeText(
                            this@DetailTransaksiActivity,
                            "Detail transaksi kosong",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "errorBody = $errorBody")
                    Toast.makeText(
                        this@DetailTransaksiActivity,
                        "Gagal memuat detail: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "exception fetch detail", e)
                Toast.makeText(
                    this@DetailTransaksiActivity,
                    "Gagal terhubung ke server: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun bindData(item: HistoryItem) {
        val statusTransaksi = item.statusTransaksi.lowercase()
        val statusBayar = item.pembayaran?.statusBayar?.lowercase() ?: "menunggu"

        tvId.text = "#${item.idTransaksi}"
        tvTanggal.text = formatDate(item.tanggal)
        tvTotal.text = formatRupiah(item.totalHarga)

        setupStatusTransaksi(statusTransaksi)
        setupStatusBayar(statusBayar)

        tvSewa.text = if (item.detailSewa.isEmpty()) {
            "Tidak ada item sewa"
        } else {
            item.detailSewa.joinToString("\n") {
                val namaPs = it.playstation?.nomorPs ?: "-"
                val tipePs = it.playstation?.tipe?.namaTipe ?: "-"
                val durasi = it.durasiMenit ?: 0
                "$namaPs • $tipePs • $durasi menit"
            }
        }

        tvProduk.text = if (item.detailProduk.isEmpty()) {
            "Tidak ada produk"
        } else {
            item.detailProduk.joinToString("\n") {
                "${it.produk?.nama ?: "-"} x${it.qty}"
            }
        }

        val bolehUbah = statusTransaksi == "aktif" &&
                statusBayar != "lunas" &&
                statusBayar != "menunggu_validasi"

        val bolehBayar = when (statusTransaksi) {
            "aktif", "menunggu_pembayaran" -> {
                statusBayar != "lunas" && statusBayar != "menunggu_validasi"
            }
            else -> false
        }

        layoutActionButtons.visibility = if (bolehBayar || bolehUbah) View.VISIBLE else View.GONE

        btnBayar.visibility = if (bolehBayar) View.VISIBLE else View.GONE
        btnBayar.isEnabled = bolehBayar

        btnTambahWaktu.visibility = if (bolehUbah) View.VISIBLE else View.GONE
        btnTambahWaktu.isEnabled = bolehUbah

        btnTambahProduk.visibility = if (bolehUbah) View.VISIBLE else View.GONE
        btnTambahProduk.isEnabled = bolehUbah

        layoutSecondaryActions.visibility =
            if (btnTambahWaktu.visibility == View.VISIBLE || btnTambahProduk.visibility == View.VISIBLE) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun setupStatusTransaksi(status: String) {
        when (status) {
            "aktif" -> {
                tvStatusTransaksi.text = "Sedang Berjalan"
                setBadge(tvStatusTransaksi, "#DCC8FF", "#4B2A7B")
            }
            "menunggu_pembayaran" -> {
                tvStatusTransaksi.text = "Menunggu Pembayaran"
                setBadge(tvStatusTransaksi, "#FFE7A8", "#6B4E00")
            }
            "waiting" -> {
                tvStatusTransaksi.text = "Menunggu Approval"
                setBadge(tvStatusTransaksi, "#FFE7A8", "#6B4E00")
            }
            "dijadwalkan" -> {
                tvStatusTransaksi.text = "Dijadwalkan"
                setBadge(tvStatusTransaksi, "#FFD8B0", "#7A3E00")
            }
            "selesai" -> {
                tvStatusTransaksi.text = "Selesai"
                setBadge(tvStatusTransaksi, "#C7F9D4", "#14532D")
            }
            "dibatalkan" -> {
                tvStatusTransaksi.text = "Dibatalkan"
                setBadge(tvStatusTransaksi, "#FFD1D1", "#7F1D1D")
            }
            "ditolak" -> {
                tvStatusTransaksi.text = "Ditolak"
                setBadge(tvStatusTransaksi, "#F8D0D0", "#5B1720")
            }
            else -> {
                tvStatusTransaksi.text = status.replaceFirstChar { it.uppercase() }
                setBadge(tvStatusTransaksi, "#E5E7EB", "#374151")
            }
        }
    }

    private fun setupStatusBayar(status: String) {
        when (status) {
            "lunas" -> {
                tvStatusBayar.text = "Lunas"
                setBadge(tvStatusBayar, "#C7F9D4", "#14532D")
            }
            "gagal" -> {
                tvStatusBayar.text = "Gagal"
                setBadge(tvStatusBayar, "#FFD1D1", "#7F1D1D")
            }
            "menunggu_validasi" -> {
                tvStatusBayar.text = "Menunggu Validasi"
                setBadge(tvStatusBayar, "#FDE68A", "#92400E")
            }
            "menunggu" -> {
                tvStatusBayar.text = "Menunggu"
                setBadge(tvStatusBayar, "#FFE7A8", "#6B4E00")
            }
            else -> {
                tvStatusBayar.text = status.replaceFirstChar { it.uppercase() }
                setBadge(tvStatusBayar, "#E5E7EB", "#374151")
            }
        }
    }

    private fun setBadge(textView: TextView, bgColor: String, textColor: String) {
        textView.setBackgroundColor(Color.parseColor(bgColor))
        textView.setTextColor(Color.parseColor(textColor))
        textView.setPadding(24, 12, 24, 12)
    }

    private fun formatRupiah(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value)
    }

    private fun formatDate(value: String): String {
        return try {
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            )

            val output = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))

            for (format in formats) {
                try {
                    val parsed = format.parse(value)
                    if (parsed != null) return output.format(parsed)
                } catch (_: Exception) {
                }
            }

            value
        } catch (_: Exception) {
            value
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
}