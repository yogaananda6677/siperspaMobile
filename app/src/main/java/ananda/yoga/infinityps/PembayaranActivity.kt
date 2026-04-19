package ananda.yoga.infinityps

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PembayaranActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvIdTransaksi: TextView
    private lateinit var tvTotalTagihan: TextView
    private lateinit var spinnerMetode: Spinner
    private lateinit var tvInfoPembayaran: TextView
    private lateinit var btnBayar: Button
    private lateinit var progressBar: ProgressBar

    private var idTransaksi: Int = 0
    private var totalHarga: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pembayaran)

        bindViews()
        readIntent()
        setupHeader()
        setupSpinner()
        setupActions()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        tvIdTransaksi = findViewById(R.id.tvIdTransaksi)
        tvTotalTagihan = findViewById(R.id.tvTotalTagihan)
        spinnerMetode = findViewById(R.id.spinnerMetodePembayaran)
        tvInfoPembayaran = findViewById(R.id.tvInfoPembayaran)
        btnBayar = findViewById(R.id.btnBayarSekarang)
        progressBar = findViewById(R.id.progressBarBayar)
    }

    private fun readIntent() {
        idTransaksi = intent.getIntExtra("id_transaksi", 0)
        totalHarga = intent.getDoubleExtra("total_harga", 0.0)
    }

    private fun setupHeader() {
        tvIdTransaksi.text = "#$idTransaksi"
        tvTotalTagihan.text = formatRupiah(totalHarga)
    }

    private fun setupActions() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnBayar.setOnClickListener {
            submitPembayaran()
        }
    }

    private fun setupSpinner() {
        val items = listOf("cash", "online")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        spinnerMetode.adapter = adapter

        spinnerMetode.setSelection(0)
        updateInfoText("cash")

        spinnerMetode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val metode = spinnerMetode.selectedItem?.toString()?.lowercase() ?: "cash"
                updateInfoText(metode)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun updateInfoText(metode: String) {
        tvInfoPembayaran.text = if (metode == "cash") {
            "Pembayaran cash hanya mengirim permintaan pembayaran. Admin akan memvalidasi saat kamu membayar di kasir."
        } else {
            "Pembayaran online akan langsung diproses oleh sistem setelah kamu kirim pembayaran."
        }
    }

    private fun submitPembayaran() {
        val metode = spinnerMetode.selectedItem?.toString()?.lowercase() ?: "cash"

        val request = BayarRequest(
            metodePembayaran = metode,
            totalBayar = null
        )

        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.bayarTransaksi(
                    "Bearer $token",
                    "application/json",
                    idTransaksi,
                    request
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val message = body?.message ?: if (metode == "cash") {
                        "Permintaan pembayaran cash berhasil dikirim. Menunggu validasi admin."
                    } else {
                        "Pembayaran online berhasil."
                    }

                    Toast.makeText(
                        this@PembayaranActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                } else {
                    Toast.makeText(
                        this@PembayaranActivity,
                        "Gagal memproses pembayaran: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PembayaranActivity,
                    "Gagal terhubung ke server: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnBayar.isEnabled = !isLoading
        btnBayar.alpha = if (isLoading) 0.7f else 1f
        spinnerMetode.isEnabled = !isLoading
        btnBack.isEnabled = !isLoading
    }

    private fun formatRupiah(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value)
    }
}