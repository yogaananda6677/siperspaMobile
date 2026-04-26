package ananda.yoga.infinityps

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    private lateinit var layoutQrisResult: View
    private lateinit var tvQrisStatus: TextView
    private lateinit var tvQrisProviderStatus: TextView
    private lateinit var tvQrisExpiredAt: TextView
    private lateinit var tvQrisUrl: TextView
    private lateinit var btnBukaQris: Button
    private lateinit var btnRefreshStatus: Button

    private var idTransaksi: Int = 0
    private var totalHarga: Double = 0.0
    private var currentQrisUrl: String? = null
    private var pollingJob: Job? = null

    companion object {
        private const val POLLING_INTERVAL_MS = 5000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pembayaran)

        bindViews()
        readIntent()
        setupHeader()
        setupSpinner()
        setupActions()
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        super.onDestroy()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        tvIdTransaksi = findViewById(R.id.tvIdTransaksi)
        tvTotalTagihan = findViewById(R.id.tvTotalTagihan)
        spinnerMetode = findViewById(R.id.spinnerMetodePembayaran)
        tvInfoPembayaran = findViewById(R.id.tvInfoPembayaran)
        btnBayar = findViewById(R.id.btnBayarSekarang)
        progressBar = findViewById(R.id.progressBarBayar)

        layoutQrisResult = findViewById(R.id.layoutQrisResult)
        tvQrisStatus = findViewById(R.id.tvQrisStatus)
        tvQrisProviderStatus = findViewById(R.id.tvQrisProviderStatus)
        tvQrisExpiredAt = findViewById(R.id.tvQrisExpiredAt)
        tvQrisUrl = findViewById(R.id.tvQrisUrl)
        btnBukaQris = findViewById(R.id.btnBukaQris)
        btnRefreshStatus = findViewById(R.id.btnRefreshStatus)
    }

    private fun readIntent() {
        idTransaksi = intent.getIntExtra("id_transaksi", 0)
        totalHarga = intent.getDoubleExtra("total_harga", 0.0)
    }

    private fun setupHeader() {
        tvIdTransaksi.text = "#$idTransaksi"
        tvTotalTagihan.text = formatRupiah(totalHarga)
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

    private fun setupActions() {
        btnBack.setOnClickListener {
            pollingJob?.cancel()
            onBackPressedDispatcher.onBackPressed()
        }

        btnBayar.setOnClickListener {
            val metode = spinnerMetode.selectedItem?.toString()?.lowercase() ?: "cash"
            if (metode == "cash") {
                submitCashPayment()
            } else {
                createQrisPayment()
            }
        }

        btnBukaQris.setOnClickListener {
            val qrisUrl = currentQrisUrl
            if (qrisUrl.isNullOrBlank()) {
                Toast.makeText(this, "Link QRIS tidak tersedia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(qrisUrl)))
            } catch (e: Exception) {
                Toast.makeText(this, "Tidak bisa membuka link QRIS", Toast.LENGTH_SHORT).show()
            }
        }

        btnRefreshStatus.setOnClickListener {
            checkQrisStatus(showToast = true)
        }
    }

    private fun updateInfoText(metode: String) {
        layoutQrisResult.visibility = View.GONE
        pollingJob?.cancel()

        tvInfoPembayaran.text = if (metode == "cash") {
            "Pembayaran cash akan dikirim sebagai permintaan validasi ke admin."
        } else {
            "Pembayaran online akan membuat QRIS Midtrans. Status akan dicek otomatis setiap 5 detik."
        }
    }

    private fun submitCashPayment() {
        pollingJob?.cancel()

        val request = BayarRequest(
            metodePembayaran = "cash",
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
                    Toast.makeText(
                        this@PembayaranActivity,
                        response.body()?.message ?: "Permintaan pembayaran cash berhasil dikirim.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@PembayaranActivity,
                        "Gagal memproses pembayaran cash: ${response.code()}",
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

    private fun createQrisPayment() {
        pollingJob?.cancel()
        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.createQrisPayment(
                    "Bearer $token",
                    "application/json",
                    idTransaksi
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val payment = body?.data?.payment
                    val midtrans = body?.data?.midtrans

                    val qrisUrl = payment?.qrUrl
                        ?: midtrans?.actions?.firstOrNull { it.name == "generate-qr-code" }?.url

                    currentQrisUrl = qrisUrl

                    layoutQrisResult.visibility = View.VISIBLE
                    tvQrisStatus.text = "Status bayar: ${payment?.statusBayar ?: "-"}"
                    tvQrisProviderStatus.text =
                        "Status provider: ${payment?.providerTransactionStatus ?: midtrans?.transactionStatus ?: "-"}"
                    tvQrisExpiredAt.text = "Berlaku sampai: ${payment?.expiredAt ?: "-"}"
                    tvQrisUrl.text = qrisUrl ?: "URL QRIS tidak tersedia"

                    Toast.makeText(
                        this@PembayaranActivity,
                        body?.message ?: "QRIS berhasil dibuat.",
                        Toast.LENGTH_LONG
                    ).show()

                    startPollingQrisStatus()
                } else {
                    Toast.makeText(
                        this@PembayaranActivity,
                        "Gagal membuat QRIS: ${response.code()}",
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

    private fun startPollingQrisStatus() {
        pollingJob?.cancel()

        pollingJob = lifecycleScope.launch {
            while (isActive) {
                delay(POLLING_INTERVAL_MS)
                checkQrisStatus(showToast = false)
            }
        }
    }

    private fun checkQrisStatus(showToast: Boolean) {
        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.checkQrisPaymentStatus(
                    "Bearer $token",
                    "application/json",
                    idTransaksi
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val payment = body?.data?.payment
                    val midtrans = body?.data?.midtrans

                    val qrisUrl = payment?.qrUrl
                        ?: midtrans?.actions?.firstOrNull { it.name == "generate-qr-code" }?.url

                    currentQrisUrl = qrisUrl

                    layoutQrisResult.visibility = View.VISIBLE
                    tvQrisStatus.text = "Status bayar: ${payment?.statusBayar ?: "-"}"
                    tvQrisProviderStatus.text =
                        "Status provider: ${payment?.providerTransactionStatus ?: midtrans?.transactionStatus ?: "-"}"
                    tvQrisExpiredAt.text = "Berlaku sampai: ${payment?.expiredAt ?: "-"}"
                    tvQrisUrl.text = qrisUrl ?: "URL QRIS tidak tersedia"

                    val statusBayar = payment?.statusBayar?.lowercase() ?: ""
                    val providerStatus = payment?.providerTransactionStatus?.lowercase() ?: ""

                    if (statusBayar == "lunas" || providerStatus == "settlement" || providerStatus == "capture") {
                        pollingJob?.cancel()
                        Toast.makeText(
                            this@PembayaranActivity,
                            "Pembayaran berhasil dan sudah lunas.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        return@launch
                    }

                    if (statusBayar == "gagal" || providerStatus in listOf("deny", "cancel", "expire", "failure")) {
                        pollingJob?.cancel()
                        Toast.makeText(
                            this@PembayaranActivity,
                            "Pembayaran gagal atau kadaluarsa.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    if (showToast) {
                        Toast.makeText(
                            this@PembayaranActivity,
                            body?.message ?: "Status pembayaran diperbarui.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    if (showToast) {
                        Toast.makeText(
                            this@PembayaranActivity,
                            "Gagal cek status pembayaran: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                if (showToast) {
                    Toast.makeText(
                        this@PembayaranActivity,
                        "Gagal terhubung ke server: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnBayar.isEnabled = !isLoading
        btnBayar.alpha = if (isLoading) 0.7f else 1f
        spinnerMetode.isEnabled = !isLoading
        btnBack.isEnabled = !isLoading
        btnBukaQris.isEnabled = !isLoading
        btnRefreshStatus.isEnabled = !isLoading
    }

    private fun formatRupiah(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value)
    }
}