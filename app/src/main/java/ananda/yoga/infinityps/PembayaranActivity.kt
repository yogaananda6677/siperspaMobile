package ananda.yoga.infinityps

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
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

        tvIdTransaksi = findViewById(R.id.tvIdTransaksi)
        tvTotalTagihan = findViewById(R.id.tvTotalTagihan)
        spinnerMetode = findViewById(R.id.spinnerMetodePembayaran)
        tvInfoPembayaran = findViewById(R.id.tvInfoPembayaran)
        btnBayar = findViewById(R.id.btnBayarSekarang)
        progressBar = findViewById(R.id.progressBarBayar)

        idTransaksi = intent.getIntExtra("id_transaksi", 0)
        totalHarga = intent.getDoubleExtra("total_harga", 0.0)

        tvIdTransaksi.text = "#$idTransaksi"
        tvTotalTagihan.text = formatRupiah(totalHarga)

        setupSpinner()

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

        spinnerMetode.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val metode = spinnerMetode.selectedItem?.toString()?.lowercase() ?: "cash"
                updateInfoText(metode)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }

    private fun updateInfoText(metode: String) {
        tvInfoPembayaran.text = if (metode == "cash") {
            "Pembayaran cash hanya mengirim permintaan pembayaran. Admin akan memvalidasi pembayaran saat kamu membayar di kasir."
        } else {
            "Pembayaran online akan langsung diproses oleh sistem."
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
                    val errorBody = response.errorBody()?.string()
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
        spinnerMetode.isEnabled = !isLoading
    }

    private fun formatRupiah(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value)
    }
}