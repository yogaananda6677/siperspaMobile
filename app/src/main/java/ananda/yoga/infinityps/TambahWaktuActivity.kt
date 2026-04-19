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
import java.util.Locale

class TambahWaktuActivity : AppCompatActivity() {

    private lateinit var tvIdTransaksi: TextView
    private lateinit var tvNomorPs: TextView
    private lateinit var tvNamaTipe: TextView
    private lateinit var tvHargaPerJam: TextView
    private lateinit var tvEstimasiTambahan: TextView

    private lateinit var spinnerMenit: Spinner
    private lateinit var btnSimpan: Button
    private lateinit var progressBar: ProgressBar

    private var idTransaksi: Int = 0
    private var idPs: Int = 0
    private var nomorPs: String = "-"
    private var namaTipe: String = "-"
    private var hargaSewa: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_waktu)

        tvIdTransaksi = findViewById(R.id.tvIdTransaksi)
        tvNomorPs = findViewById(R.id.tvNomorPs)
        tvNamaTipe = findViewById(R.id.tvNamaTipe)
        tvHargaPerJam = findViewById(R.id.tvHargaPerJam)
        tvEstimasiTambahan = findViewById(R.id.tvEstimasiTambahan)

        spinnerMenit = findViewById(R.id.spinnerMenitTambahan)
        btnSimpan = findViewById(R.id.btnSimpanTambahWaktu)
        progressBar = findViewById(R.id.progressBarTambahWaktu)

        idTransaksi = intent.getIntExtra("id_transaksi", 0)
        idPs = intent.getIntExtra("id_ps", 0)
        nomorPs = intent.getStringExtra("nomor_ps") ?: "-"
        namaTipe = intent.getStringExtra("nama_tipe") ?: "-"
        hargaSewa = intent.getLongExtra("harga_sewa", 0L)

        tvIdTransaksi.text = "#$idTransaksi"
        tvNomorPs.text = nomorPs
        tvNamaTipe.text = namaTipe
        tvHargaPerJam.text = "${formatRupiah(hargaSewa)}/jam"

        setupSpinner()
        updateEstimasi()

        spinnerMenit.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                updateEstimasi()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

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

    private fun updateEstimasi() {
        val menit = getMenitTambahan()
        val estimasi = ((hargaSewa / 60.0) * menit).toLong()
        tvEstimasiTambahan.text = formatRupiah(estimasi)
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

    private fun formatRupiah(value: Long): String {
        return "Rp " + "%,d".format(Locale("id", "ID"), value).replace(',', '.')
    }
}