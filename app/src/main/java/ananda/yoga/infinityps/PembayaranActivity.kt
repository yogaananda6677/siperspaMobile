package ananda.yoga.infinityps

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
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

    private lateinit var tvTotal: TextView
    private lateinit var spinnerMetode: Spinner
    private lateinit var etNominal: EditText
    private lateinit var btnBayar: Button
    private lateinit var progressBar: ProgressBar

    private var idTransaksi: Int = 0
    private var totalHarga: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pembayaran)

        idTransaksi = intent.getIntExtra("id_transaksi", 0)
        totalHarga = intent.getDoubleExtra("total_harga", 0.0)

        tvTotal = findViewById(R.id.tvTotalBayar)
        spinnerMetode = findViewById(R.id.spinnerMetode)
        etNominal = findViewById(R.id.etNominalBayar)
        btnBayar = findViewById(R.id.btnKonfirmasiBayar)
        progressBar = findViewById(R.id.progressBar)

        tvTotal.text = formatRupiah(totalHarga)

        val metode = listOf("cash", "online")
        spinnerMetode.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, metode)

        btnBayar.setOnClickListener {
            submitBayar()
        }
    }

    private fun submitBayar() {
        val metode = spinnerMetode.selectedItem.toString()
        val nominal = etNominal.text.toString().trim()

        val totalBayar = if (metode == "cash") {
            if (nominal.isEmpty()) {
                etNominal.error = "Nominal wajib diisi"
                return
            }
            nominal.toDoubleOrNull()
        } else {
            totalHarga
        }

        if (totalBayar == null) {
            etNominal.error = "Nominal tidak valid"
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.bayarTransaksi(
                    "Bearer $token",
                    "application/json",
                    idTransaksi,
                    BayarRequest(
                        metodePembayaran = metode,
                        totalBayar = totalBayar
                    )
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@PembayaranActivity,
                        "Pembayaran berhasil",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val error = response.errorBody()?.string()
                    Toast.makeText(
                        this@PembayaranActivity,
                        "Gagal bayar: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PembayaranActivity, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun formatRupiah(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value)
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnBayar.isEnabled = !isLoading
    }
}