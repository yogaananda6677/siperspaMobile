package ananda.yoga.infinityps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts

class DetailTransaksiActivity : AppCompatActivity() {

    companion object {
        private const val MINIMUM_EDIT_MINUTES = 30
    }

    private lateinit var tvId: TextView
    private lateinit var tvStatusTransaksi: TextView
    private lateinit var tvStatusBayar: TextView
    private lateinit var tvTanggal: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvInfoRingkas: TextView

    private lateinit var tvDetailSewa: TextView
    private lateinit var tvDetailProduk: TextView

    private lateinit var btnBayar: Button
    private lateinit var btnTambahWaktu: Button
    private lateinit var btnTambahProduk: Button

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout
    private lateinit var layoutActionButtons: LinearLayout
    private lateinit var layoutSecondaryActions: LinearLayout
    private lateinit var btnBack: ImageView

    private var idTransaksi: Int = 0
    private var currentData: HistoryItem? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_transaksi)

        idTransaksi = intent.getIntExtra("id_transaksi", 0)

        bindViews()
        setupActions()
        ensureNotificationPermission()
        fetchDetail()
    }

    override fun onResume() {
        super.onResume()
        fetchDetail()
    }

    private fun bindViews() {
        tvId = findViewById(R.id.tvIdTransaksi)
        tvStatusTransaksi = findViewById(R.id.tvStatusTransaksi)
        tvStatusBayar = findViewById(R.id.tvStatusBayar)
        tvTanggal = findViewById(R.id.tvTanggal)
        tvTotal = findViewById(R.id.tvTotal)
        tvInfoRingkas = findViewById(R.id.tvInfoRingkas)

        tvDetailSewa = findViewById(R.id.tvDetailSewa)
        tvDetailProduk = findViewById(R.id.tvDetailProduk)

        btnBayar = findViewById(R.id.btnBayar)
        btnTambahWaktu = findViewById(R.id.btnTambahWaktu)
        btnTambahProduk = findViewById(R.id.btnTambahProduk)

        progressBar = findViewById(R.id.progressBar)
        contentLayout = findViewById(R.id.contentLayout)
        layoutActionButtons = findViewById(R.id.layoutActionButtons)
        layoutSecondaryActions = findViewById(R.id.layoutSecondaryActions)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun ensureNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    private fun setupActions() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnBayar.setOnClickListener {
            val data = currentData ?: return@setOnClickListener
            val intent = Intent(this, PembayaranActivity::class.java).apply {
                putExtra("id_transaksi", data.idTransaksi)
                putExtra("total_harga", data.totalHarga)
            }
            startActivity(intent)
        }

        btnTambahWaktu.setOnClickListener {
            val data = currentData ?: return@setOnClickListener

            if (isLockedByRemainingTime(data)) {
                Toast.makeText(
                    this,
                    "Sisa waktu kurang dari 30 menit. Tambah waktu tidak tersedia.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val detailSewa = data.detailSewa.firstOrNull()
            val psId = detailSewa?.idPs ?: 0
            val nomorPs = detailSewa?.playstation?.nomorPs ?: "-"
            val namaTipe = detailSewa?.playstation?.tipe?.namaTipe ?: "-"
            val hargaSewa = detailSewa?.playstation?.tipe?.hargaSewa ?: 0L

            val intent = Intent(this, TambahWaktuActivity::class.java).apply {
                putExtra("id_transaksi", data.idTransaksi)
                putExtra("id_ps", psId)
                putExtra("nomor_ps", nomorPs)
                putExtra("nama_tipe", namaTipe)
                putExtra("harga_sewa", hargaSewa)
            }
            startActivity(intent)
        }

        btnTambahProduk.setOnClickListener {
            val data = currentData ?: return@setOnClickListener

            if (isLockedByRemainingTime(data)) {
                Toast.makeText(
                    this,
                    "Sisa waktu kurang dari 30 menit. Tambah produk tidak tersedia.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val detailSewa = data.detailSewa.firstOrNull()
            val nomorPs = detailSewa?.playstation?.nomorPs ?: "-"
            val namaTipe = detailSewa?.playstation?.tipe?.namaTipe ?: "-"

            val intent = Intent(this, TambahProdukActivity::class.java).apply {
                putExtra("id_transaksi", data.idTransaksi)
                putExtra("nomor_ps", nomorPs)
                putExtra("nama_tipe", namaTipe)
            }
            startActivity(intent)
        }
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
                        TransaksiReminderScheduler.scheduleReminders(this@DetailTransaksiActivity, item)
                    } else {
                        Toast.makeText(
                            this@DetailTransaksiActivity,
                            "Detail transaksi kosong",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@DetailTransaksiActivity,
                        "Gagal memuat detail: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
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
        val terkunciKarenaWaktu = isLockedByRemainingTime(item)
        val sisaMenitAktif = getSmallestRemainingMinutes(item)

        tvId.text = "Transaksi #${item.idTransaksi}"
        tvTanggal.text = formatDate(item.tanggal)
        tvTotal.text = formatRupiah(item.totalHarga)

        setupStatusTransaksi(statusTransaksi)
        setupStatusBayar(statusBayar)

        tvInfoRingkas.text = when {
            statusBayar == "menunggu_validasi" ->
                "Pembayaran sedang menunggu validasi admin."
            statusTransaksi == "aktif" && terkunciKarenaWaktu ->
                "Sisa waktu kurang dari 30 menit, tambah waktu dan produk sudah dikunci."
            statusTransaksi == "aktif" && statusBayar != "lunas" ->
                "Transaksi masih berjalan dan belum lunas."
            statusTransaksi == "selesai" ->
                "Transaksi sudah selesai."
            else ->
                "Cek detail sewa dan produk di bawah."
        }

        tvDetailSewa.text = if (item.detailSewa.isEmpty()) {
            "Belum ada item sewa."
        } else {
            item.detailSewa.joinToString("\n\n") {
                val namaPs = it.playstation?.nomorPs ?: "-"
                val tipePs = it.playstation?.tipe?.namaTipe ?: "-"
                val durasi = it.durasiMenit ?: 0
                val subtotal = it.subtotal ?: 0.0
                val sisaInfo = getRemainingMinutesFromDate(it.jamSelesai)?.let { menit ->
                    if (menit > 0) "\nSisa: $menit menit" else "\nSisa: habis"
                } ?: ""

                "PS $namaPs\n" +
                        "Tipe: $tipePs\n" +
                        "Durasi: $durasi menit\n" +
                        "Mulai: ${formatShortDateTime(it.jamMulai)}\n" +
                        "Selesai: ${formatShortDateTime(it.jamSelesai)}$sisaInfo\n" +
                        "Subtotal: ${formatRupiah(subtotal)}"
            }
        }

        tvDetailProduk.text = if (item.detailProduk.isEmpty()) {
            "Belum ada produk tambahan."
        } else {
            item.detailProduk.joinToString("\n\n") {
                val namaProduk = it.produk?.nama ?: "-"
                val harga = it.produk?.harga ?: 0L
                val subtotal = it.subtotal ?: 0.0

                "$namaProduk\n" +
                        "Qty: ${it.qty}\n" +
                        "Harga: ${formatRupiah(harga.toDouble())}\n" +
                        "Subtotal: ${formatRupiah(subtotal)}"
            }
        }

        val bolehUbahBase = statusTransaksi == "aktif" &&
                statusBayar != "lunas" &&
                statusBayar != "menunggu_validasi"

        val bolehUbah = bolehUbahBase && !terkunciKarenaWaktu

        val bolehBayar = when (statusTransaksi) {
            "aktif", "menunggu_pembayaran" -> {
                statusBayar != "lunas" && statusBayar != "menunggu_validasi"
            }
            else -> false
        }

        layoutActionButtons.visibility = if (bolehBayar || bolehUbah) View.VISIBLE else View.GONE

        btnBayar.visibility = if (bolehBayar) View.VISIBLE else View.GONE
        btnTambahWaktu.visibility = if (bolehUbah) View.VISIBLE else View.GONE
        btnTambahProduk.visibility = if (bolehUbah) View.VISIBLE else View.GONE

        layoutSecondaryActions.visibility =
            if (btnTambahWaktu.visibility == View.VISIBLE || btnTambahProduk.visibility == View.VISIBLE) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (statusTransaksi == "aktif" && bolehUbahBase && terkunciKarenaWaktu) {
            tvInfoRingkas.text =
                "Sisa waktu ${sisaMenitAktif ?: 0} menit. Tambah waktu dan tambah produk dinonaktifkan."
        }
    }

    private fun isLockedByRemainingTime(item: HistoryItem): Boolean {
        val statusTransaksi = item.statusTransaksi.lowercase()
        if (statusTransaksi != "aktif") return false

        val smallestRemaining = getSmallestRemainingMinutes(item) ?: return false
        return smallestRemaining < MINIMUM_EDIT_MINUTES
    }

    private fun getSmallestRemainingMinutes(item: HistoryItem): Long? {
        val remainingList = item.detailSewa.mapNotNull { getRemainingMinutesFromDate(it.jamSelesai) }
        if (remainingList.isEmpty()) return null
        return remainingList.minOrNull()
    }

    private fun getRemainingMinutesFromDate(raw: String?): Long? {
        val endMillis = parseDateToMillis(raw) ?: return null
        val diffMillis = endMillis - System.currentTimeMillis()
        return diffMillis / 60000
    }

    private fun parseDateToMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null

        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
        )

        for (format in formats) {
            try {
                val parsed = format.parse(raw)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }

        return null
    }

    private fun setupStatusTransaksi(status: String) {
        when (status) {
            "aktif" -> {
                tvStatusTransaksi.text = "Sedang Berjalan"
                applyBadge(tvStatusTransaksi, R.drawable.bg_status_owned_active, R.color.status_owned_active_text)
            }
            "menunggu_pembayaran" -> {
                tvStatusTransaksi.text = "Menunggu Pembayaran"
                applyBadge(tvStatusTransaksi, R.drawable.bg_status_booking, R.color.status_booking_text)
            }
            "waiting" -> {
                tvStatusTransaksi.text = "Menunggu Approval"
                applyBadge(tvStatusTransaksi, R.drawable.bg_status_booking, R.color.status_booking_text)
            }
            "dijadwalkan" -> {
                tvStatusTransaksi.text = "Dijadwalkan"
                applyBadge(tvStatusTransaksi, R.drawable.bg_status_booking, R.color.status_booking_text)
            }
            "selesai" -> {
                tvStatusTransaksi.text = "Selesai"
                applyBadge(tvStatusTransaksi, R.drawable.bg_status_tersedia, R.color.status_tersedia_text)
            }
            "dibatalkan" -> {
                tvStatusTransaksi.text = "Dibatalkan"
                applyBadge(tvStatusTransaksi, R.drawable.bg_status_danger, R.color.status_danger_text)
            }
            "ditolak" -> {
                tvStatusTransaksi.text = "Ditolak"
                applyBadge(tvStatusTransaksi, R.drawable.bg_status_danger, R.color.status_danger_text)
            }
            else -> {
                tvStatusTransaksi.text = status.replaceFirstChar { it.uppercase() }
                applyBadge(tvStatusTransaksi, R.drawable.bg_status_neutral, R.color.text_secondary)
            }
        }
    }

    private fun setupStatusBayar(status: String) {
        when (status) {
            "lunas" -> {
                tvStatusBayar.text = "Lunas"
                applyBadge(tvStatusBayar, R.drawable.bg_status_tersedia, R.color.status_tersedia_text)
            }
            "gagal" -> {
                tvStatusBayar.text = "Gagal"
                applyBadge(tvStatusBayar, R.drawable.bg_status_danger, R.color.status_danger_text)
            }
            "menunggu_validasi" -> {
                tvStatusBayar.text = "Menunggu Validasi"
                applyBadge(tvStatusBayar, R.drawable.bg_status_booking, R.color.status_booking_text)
            }
            "menunggu" -> {
                tvStatusBayar.text = "Menunggu"
                applyBadge(tvStatusBayar, R.drawable.bg_status_booking, R.color.status_booking_text)
            }
            else -> {
                tvStatusBayar.text = status.replaceFirstChar { it.uppercase() }
                applyBadge(tvStatusBayar, R.drawable.bg_status_neutral, R.color.text_secondary)
            }
        }
    }

    private fun applyBadge(textView: TextView, backgroundRes: Int, textColorRes: Int) {
        textView.setBackgroundResource(backgroundRes)
        textView.setTextColor(ContextCompat.getColor(this, textColorRes))
    }

    private fun formatRupiah(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value)
    }

    private fun formatDate(value: String): String {
        return try {
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                },
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                },
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }
            )
            val output = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))

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

    private fun formatShortDateTime(value: String?): String {
        if (value.isNullOrBlank()) return "-"
        return try {
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                },
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                },
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }
            )
            val output = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))

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