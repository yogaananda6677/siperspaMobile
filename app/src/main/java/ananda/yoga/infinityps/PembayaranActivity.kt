package ananda.yoga.infinityps

import android.content.Context
import android.graphics.drawable.Drawable
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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
    private lateinit var imgQrisCode: ImageView
    private lateinit var progressQris: ProgressBar
    private lateinit var btnRefreshStatus: Button

    private var idTransaksi: Int = 0
    private var totalHarga: Double = 0.0
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
        imgQrisCode = findViewById(R.id.imgQrisCode)
        progressQris = findViewById(R.id.progressQris)
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
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
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
            if (metode == "cash") submitCashPayment() else createQrisPayment()
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

    private fun tampilkanQrisImage(qrisUrl: String?) {
        if (!qrisUrl.isNullOrBlank()) {
            progressQris.visibility = View.VISIBLE
            imgQrisCode.visibility = View.GONE

            Glide.with(this)
                .load(qrisUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressQris.visibility = View.GONE
                        imgQrisCode.visibility = View.VISIBLE
                        Toast.makeText(
                            this@PembayaranActivity,
                            "Gagal memuat gambar QR Code",
                            Toast.LENGTH_SHORT
                        ).show()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressQris.visibility = View.GONE
                        imgQrisCode.visibility = View.VISIBLE
                        return false
                    }
                })
                .into(imgQrisCode)
        } else {
            imgQrisCode.visibility = View.GONE
            progressQris.visibility = View.GONE
        }
    }

    private fun submitCashPayment() {
        pollingJob?.cancel()
        setLoading(true)

        val request = BayarRequest(metodePembayaran = "cash", totalBayar = null)

        lifecycleScope.launch {
            try {
                val token = getToken()

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
                    showErrorToast(response.errorBody()?.string(), response.code())
                }
            } catch (e: Exception) {
                showConnectionError(e)
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
                val token = getToken()

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

                    layoutQrisResult.visibility = View.VISIBLE
                    tvQrisStatus.text = "Status bayar: ${payment?.statusBayar ?: "-"}"
                    tvQrisProviderStatus.text = "Status provider: ${payment?.providerTransactionStatus ?: midtrans?.transactionStatus ?: "-"}"
                    tvQrisExpiredAt.text = "Berlaku sampai: ${payment?.expiredAt ?: "-"}"
                    tampilkanQrisImage(qrisUrl)

                    Toast.makeText(
                        this@PembayaranActivity,
                        body?.message ?: "QRIS berhasil dibuat.",
                        Toast.LENGTH_LONG
                    ).show()

                    startPollingQrisStatus()
                } else {
                    showErrorToast(response.errorBody()?.string(), response.code())
                }
            } catch (e: Exception) {
                showConnectionError(e)
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
                val token = getToken()

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

                    layoutQrisResult.visibility = View.VISIBLE
                    tvQrisStatus.text = "Status bayar: ${payment?.statusBayar ?: "-"}"
                    tvQrisProviderStatus.text = "Status provider: ${payment?.providerTransactionStatus ?: midtrans?.transactionStatus ?: "-"}"
                    tvQrisExpiredAt.text = "Berlaku sampai: ${payment?.expiredAt ?: "-"}"
                    tampilkanQrisImage(qrisUrl)

                    val statusBayar = payment?.statusBayar?.lowercase() ?: ""
                    val providerStatus = payment?.providerTransactionStatus?.lowercase() ?: ""

                    when {
                        statusBayar == "lunas" || providerStatus in listOf("settlement", "capture") -> {
                            pollingJob?.cancel()
                            Toast.makeText(this@PembayaranActivity, "Pembayaran berhasil dan sudah lunas.", Toast.LENGTH_LONG).show()
                            finish()
                            return@launch
                        }
                        statusBayar == "gagal" || providerStatus in listOf("deny", "cancel", "expire", "failure") -> {
                            pollingJob?.cancel()
                            Toast.makeText(this@PembayaranActivity, "Pembayaran gagal atau kadaluarsa.", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        showToast -> {
                            Toast.makeText(this@PembayaranActivity, body?.message ?: "Status pembayaran diperbarui.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (showToast) {
                    showErrorToast(response.errorBody()?.string(), response.code())
                }
            } catch (e: Exception) {
                if (showToast) showConnectionError(e)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnBayar.isEnabled = !isLoading
        btnBayar.alpha = if (isLoading) 0.7f else 1f
        spinnerMetode.isEnabled = !isLoading
        btnBack.isEnabled = !isLoading
        btnRefreshStatus.isEnabled = !isLoading
    }

    private fun getToken(): String {
        return getSharedPreferences("app_session", Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
    }

    private fun showErrorToast(errorBody: String?, code: Int) {
        val msg = try {
            org.json.JSONObject(errorBody).getString("message")
        } catch (e: Exception) {
            "Error $code"
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showConnectionError(e: Exception) {
        Toast.makeText(this, "Gagal terhubung ke server: ${e.message}", Toast.LENGTH_SHORT).show()
    }

    private fun formatRupiah(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value)
    }
}