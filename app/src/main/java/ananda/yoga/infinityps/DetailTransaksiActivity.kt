package ananda.yoga.infinityps

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout

    private var idTransaksi: Int = 0
    private var currentData: HistoryItem? = null

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
        progressBar = findViewById(R.id.progressBar)
        contentLayout = findViewById(R.id.contentLayout)

        btnBayar.setOnClickListener {
            val data = currentData ?: return@setOnClickListener
            val intent = Intent(this, PembayaranActivity::class.java)
            intent.putExtra("id_transaksi", data.idTransaksi)
            intent.putExtra("total_harga", data.totalHarga.toDouble())
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

                if (response.isSuccessful) {
                    val item = response.body()?.data
                    if (item != null) {
                        currentData = item
                        bindData(item)
                    }
                } else {
                    Toast.makeText(this@DetailTransaksiActivity, "Gagal memuat detail", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DetailTransaksiActivity, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun bindData(item: HistoryItem) {
        tvId.text = "#${item.idTransaksi}"
        tvStatusTransaksi.text = item.statusTransaksi
        tvStatusBayar.text = item.pembayaran?.statusBayar ?: "menunggu"
        tvTanggal.text = item.tanggal
        tvTotal.text = formatRupiah(item.totalHarga)

        tvSewa.text = if (item.detailSewa.isEmpty()) {
            "Tidak ada item sewa"
        } else {
            item.detailSewa.joinToString("\n") {
                "${it.playstation?.nomorPs ?: "-"} • ${it.playstation?.tipe?.namaTipe ?: "-"} • ${it.durasiMenit ?: 0} menit"
            }
        }

        tvProduk.text = if (item.detailProduk.isEmpty()) {
            "Tidak ada produk"
        } else {
            item.detailProduk.joinToString("\n") {
                "${it.produk?.nama ?: "-"} x${it.qty}"
            }
        }

        val statusBayar = item.pembayaran?.statusBayar?.lowercase() ?: "menunggu"
        btnBayar.visibility = if (statusBayar != "lunas") View.VISIBLE else View.GONE
    }

    private fun formatRupiah(value: Long): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value)
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
}