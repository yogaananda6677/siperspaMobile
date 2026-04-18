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

class TambahWaktuActivity : AppCompatActivity() {

    private lateinit var tvIdTransaksi: TextView
    private lateinit var spinnerMenit: Spinner
    private lateinit var btnSimpan: Button
    private lateinit var progressBar: ProgressBar

    private var idTransaksi: Int = 0
    private var idPs: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_waktu)

        tvIdTransaksi = findViewById(R.id.tvIdTransaksi)
        spinnerMenit = findViewById(R.id.spinnerMenitTambahan)
        btnSimpan = findViewById(R.id.btnSimpanTambahWaktu)
        progressBar = findViewById(R.id.progressBarTambahWaktu)

        idTransaksi = intent.getIntExtra("id_transaksi", 0)
        idPs = intent.getIntExtra("id_ps", 0)

        tvIdTransaksi.text = "#$idTransaksi"

        setupSpinner()

        btnSimpan.setOnClickListener {
            submitTambahWaktu()
        }
    }

    private fun setupSpinner() {
        val items = listOf("30 menit", "60 menit", "90 menit", "120 menit")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        spinnerMenit.adapter = adapter
    }

    private fun getMenitTambahan(): Int {
        return when (spinnerMenit.selectedItemPosition) {
            0 -> 30
            1 -> 60
            2 -> 90
            3 -> 120
            else -> 30
        }
    }

    private fun submitTambahWaktu() {
        val request = TambahWaktuRequest(
            idPs = if (idPs == 0) null else idPs,
            menitTambahan = getMenitTambahan()
        )

        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.tambahWaktu(
                    "Bearer $token",
                    "application/json",
                    idTransaksi,
                    request
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@TambahWaktuActivity,
                        "Waktu berhasil ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(
                        this@TambahWaktuActivity,
                        "Gagal tambah waktu: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TambahWaktuActivity,
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
        btnSimpan.isEnabled = !isLoading
        spinnerMenit.isEnabled = !isLoading
    }
}