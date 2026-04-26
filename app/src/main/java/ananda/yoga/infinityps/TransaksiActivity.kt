package ananda.yoga.infinityps

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TransaksiActivity : AppCompatActivity() {

    companion object {
        private const val MIN_OFFSET_MINUTES = 30
        private const val MAX_OFFSET_MINUTES = 180
        private const val SLOT_STEP_MINUTES = 30
        private const val MAX_BOOKING_MINUTES = 180
    }

    private lateinit var tvNomorPs: TextView
    private lateinit var tvNamaTipe: TextView
    private lateinit var tvHargaPerJam: TextView

    private lateinit var rgBookingMode: RadioGroup
    private lateinit var rbBookingSekarang: RadioButton
    private lateinit var rbBookingNanti: RadioButton
    private lateinit var tvJamMulai: TextView
    private lateinit var btnPilihJam: Button

    private lateinit var spinnerDurasi: Spinner
    private lateinit var tvJamSelesai: TextView
    private lateinit var tvSubtotalSewa: TextView

    private lateinit var etCariProduk: EditText
    private lateinit var spinnerKategoriProduk: Spinner
    private lateinit var rvProduk: RecyclerView
    private lateinit var tvSubtotalProduk: TextView

    private lateinit var tvSubtotalSewaRincian: TextView
    private lateinit var tvSubtotalProdukRincian: TextView
    private lateinit var tvGrandTotal: TextView

    private lateinit var btnSimpan: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageView

    private var idPs: Int = 0
    private var nomorPs: String = "-"
    private var namaTipe: String = "-"
    private var hargaSewa: Long = 0L
    private var idUser: Int = 0

    private var isReservasi: Boolean = false
    private var activeJamSelesaiRaw: String = ""

    private val semuaProdukItems = mutableListOf<CartProdukItem>()
    private lateinit var produkAdapter: ProdukCustomerAdapter

    private var subtotalSewa: Long = 0L
    private var subtotalProduk: Long = 0L

    private var bookingMode: String = "sekarang"
    private var selectedStartCalendar: Calendar = Calendar.getInstance()

    private var currentKeyword: String = ""
    private var currentKategori: String = "semua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaksi)

        bindViews()
        readIntent()
        setupHeader()
        setupSpinner()
        setupBookingMode()
        setupProdukList()
        setupProdukFilter()
        setupActions()
        loadProduk()
        hitungSewa()
    }

    private fun bindViews() {
        tvNomorPs = findViewById(R.id.tvNomorPs)
        tvNamaTipe = findViewById(R.id.tvNamaTipe)
        tvHargaPerJam = findViewById(R.id.tvHargaPerJam)

        rgBookingMode = findViewById(R.id.rgBookingMode)
        rbBookingSekarang = findViewById(R.id.rbBookingSekarang)
        rbBookingNanti = findViewById(R.id.rbBookingNanti)
        tvJamMulai = findViewById(R.id.tvJamMulai)
        btnPilihJam = findViewById(R.id.btnPilihJam)

        spinnerDurasi = findViewById(R.id.spinnerDurasi)
        tvJamSelesai = findViewById(R.id.tvJamSelesai)
        tvSubtotalSewa = findViewById(R.id.tvSubtotalSewa)

        etCariProduk = findViewById(R.id.etCariProduk)
        spinnerKategoriProduk = findViewById(R.id.spinnerKategoriProduk)
        rvProduk = findViewById(R.id.rvProduk)
        tvSubtotalProduk = findViewById(R.id.tvSubtotalProduk)

        tvSubtotalSewaRincian = findViewById(R.id.tvSubtotalSewaRincian)
        tvSubtotalProdukRincian = findViewById(R.id.tvSubtotalProdukRincian)
        tvGrandTotal = findViewById(R.id.tvGrandTotal)

        btnSimpan = findViewById(R.id.btnSimpanTransaksi)
        progressBar = findViewById(R.id.progressBarTransaksi)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun readIntent() {
        idPs = intent.getIntExtra("id_ps", 0)
        nomorPs = intent.getStringExtra("nomor_ps") ?: "-"
        namaTipe = intent.getStringExtra("nama_tipe") ?: "-"
        hargaSewa = intent.getLongExtra("harga_sewa", 0L)
        isReservasi = intent.getBooleanExtra("is_reservasi", false)
        activeJamSelesaiRaw = intent.getStringExtra("active_jam_selesai") ?: ""

        val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
        idUser = prefs.getInt("id_user", 0)
    }

    private fun setupHeader() {
        tvNomorPs.text = "PS $nomorPs"
        tvNamaTipe.text = namaTipe
        tvHargaPerJam.text = "${formatRupiah(hargaSewa)}/jam"
    }

    private fun setupSpinner() {
        val durasiList = listOf("30 menit", "60 menit", "90 menit", "120 menit", "150 menit", "180 menit")
        val spinnerAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, durasiList)
        spinnerDurasi.adapter = spinnerAdapter

        spinnerDurasi.setSelection(1, false)
        spinnerDurasi.onItemSelectedListener = SimpleItemSelectedListener {
            hitungSewa()
        }
    }

    private fun setupBookingMode() {
        if (isReservasi) {
            rgBookingMode.visibility = View.GONE
            rbBookingSekarang.visibility = View.GONE
            rbBookingNanti.visibility = View.GONE

            bookingMode = "nanti"
            selectedStartCalendar = buildMinimumReservasiCalendar()
            updateJamMulaiLabel()
        } else {
            rgBookingMode.visibility = View.VISIBLE
            rbBookingSekarang.visibility = View.VISIBLE
            rbBookingNanti.visibility = View.VISIBLE

            bookingMode = "sekarang"
            selectedStartCalendar = Calendar.getInstance()
            updateJamMulaiLabel()

            rgBookingMode.setOnCheckedChangeListener { _, checkedId ->
                bookingMode = if (checkedId == R.id.rbBookingNanti) "nanti" else "sekarang"

                if (bookingMode == "sekarang") {
                    selectedStartCalendar = Calendar.getInstance()
                } else {
                    selectedStartCalendar = buildMinimumFutureCalendar()
                }

                updateJamMulaiLabel()
                hitungSewa()
            }
        }

        btnPilihJam.setOnClickListener {
            if (isReservasi) {
                showReservasiSlotDialog()
            } else {
                if (bookingMode == "nanti") {
                    showFutureSlotDialog()
                } else {
                    Toast.makeText(this, "Mode sekarang tidak perlu pilih jam", Toast.LENGTH_SHORT).show()
                }
            }
        }

        hitungSewa()
    }

    private fun buildMinimumReservasiCalendar(): Calendar {
        val endCalendar = parseServerDateToCalendar(activeJamSelesaiRaw) ?: Calendar.getInstance()
        endCalendar.add(Calendar.MINUTE, MIN_OFFSET_MINUTES)
        normalizeMinuteSlot(endCalendar)
        return endCalendar
    }

    private fun buildMaximumReservasiCalendar(): Calendar {
        val endCalendar = parseServerDateToCalendar(activeJamSelesaiRaw) ?: Calendar.getInstance()
        endCalendar.add(Calendar.MINUTE, MAX_OFFSET_MINUTES)
        normalizeMinuteSlot(endCalendar)
        return endCalendar
    }

    private fun buildMinimumFutureCalendar(): Calendar {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, MIN_OFFSET_MINUTES)
        normalizeMinuteSlot(cal)
        return cal
    }

    private fun buildMaximumFutureCalendar(): Calendar {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, MAX_OFFSET_MINUTES)
        normalizeMinuteSlot(cal)
        return cal
    }

    private fun normalizeMinuteSlot(calendar: Calendar) {
        val minute = calendar.get(Calendar.MINUTE)
        val roundedUp = ((minute + SLOT_STEP_MINUTES - 1) / SLOT_STEP_MINUTES) * SLOT_STEP_MINUTES

        if (roundedUp >= 60) {
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            calendar.set(Calendar.MINUTE, 0)
        } else {
            calendar.set(Calendar.MINUTE, roundedUp)
        }

        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    private fun showReservasiSlotDialog() {
        val min = buildMinimumReservasiCalendar()
        val max = buildMaximumReservasiCalendar()
        showSlotDialog(min, max, "Pilih Jam Reservasi")
    }

    private fun showFutureSlotDialog() {
        val min = buildMinimumFutureCalendar()
        val max = buildMaximumFutureCalendar()
        showSlotDialog(min, max, "Pilih Jam Booking")
    }

    private fun showSlotDialog(minCalendar: Calendar, maxCalendar: Calendar, title: String) {
        val options = mutableListOf<String>()
        val optionCalendars = mutableListOf<Calendar>()

        val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))

        val cursor = minCalendar.clone() as Calendar
        while (!cursor.after(maxCalendar)) {
            options.add(sdf.format(cursor.time))
            optionCalendars.add(cursor.clone() as Calendar)
            cursor.add(Calendar.MINUTE, SLOT_STEP_MINUTES)
        }

        if (options.isEmpty()) {
            Toast.makeText(this, "Tidak ada slot waktu tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options.toTypedArray()) { _, which ->
                selectedStartCalendar = optionCalendars[which]
                updateJamMulaiLabel()
                hitungSewa()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateJamMulaiLabel() {
        tvJamMulai.text = if (!isReservasi && bookingMode == "sekarang") {
            "Sekarang"
        } else {
            val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))
            sdf.format(selectedStartCalendar.time)
        }
    }

    private fun setupProdukList() {
        produkAdapter = ProdukCustomerAdapter {
            updateSubtotalProduk()
            updateGrandTotal()
        }

        rvProduk.layoutManager = LinearLayoutManager(this)
        rvProduk.adapter = produkAdapter
        rvProduk.setHasFixedSize(false)
        rvProduk.itemAnimator = null
    }

    private fun setupProdukFilter() {
        val kategoriList = listOf("Semua", "Makanan", "Minuman", "Snack")
        val kategoriAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kategoriList)
        spinnerKategoriProduk.adapter = kategoriAdapter

        spinnerKategoriProduk.onItemSelectedListener = SimpleItemSelectedListener {
            currentKategori = when (spinnerKategoriProduk.selectedItemPosition) {
                1 -> "makanan"
                2 -> "minuman"
                3 -> "snack"
                else -> "semua"
            }
            applyProdukFilter()
        }

        etCariProduk.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentKeyword = s?.toString()?.trim().orEmpty()
                applyProdukFilter()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

    private fun setupActions() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnSimpan.setOnClickListener {
            submitTransaksi()
        }
    }

    private fun loadProduk() {
        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.getProduk("Bearer $token")
                if (response.isSuccessful) {
                    val data: List<CustomerProduk> = response.body()?.data ?: emptyList()
                    semuaProdukItems.clear()
                    semuaProdukItems.addAll(data.map { CartProdukItem(it, 0) })

                    applyProdukFilter()
                    updateSubtotalProduk()
                    updateGrandTotal()
                } else {
                    Toast.makeText(
                        this@TransaksiActivity,
                        "Gagal memuat produk",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TransaksiActivity,
                    "Gagal terhubung ke server: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun applyProdukFilter() {
        val filtered = semuaProdukItems.filter { item ->
            val nama = item.produk.nama.lowercase()
            val kategori = item.produk.jenis.trim().lowercase()

            val matchKeyword = currentKeyword.isBlank() || nama.contains(currentKeyword.lowercase())
            val matchKategori = currentKategori == "semua" || kategori == currentKategori

            matchKeyword && matchKategori
        }

        produkAdapter.submitFilteredList(filtered)
        updateProdukRecyclerHeight(filtered.size)
    }

    private fun updateProdukRecyclerHeight(count: Int) {
        val params = rvProduk.layoutParams
        if (count > 5) {
            params.height = dp(360)
            rvProduk.isNestedScrollingEnabled = true
        } else {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            rvProduk.isNestedScrollingEnabled = false
        }
        rvProduk.layoutParams = params
    }

    private fun hitungSewa() {
        val durasiMenit = getSelectedDurasiMenit()
        subtotalSewa = ((hargaSewa / 60.0) * durasiMenit).toLong()

        val mulaiMillis = if (!isReservasi && bookingMode == "sekarang") {
            System.currentTimeMillis()
        } else {
            selectedStartCalendar.timeInMillis
        }

        val selesai = Date(mulaiMillis + (durasiMenit * 60 * 1000L))
        val sdf = SimpleDateFormat("HH:mm", Locale("id", "ID"))

        tvJamSelesai.text = sdf.format(selesai)
        tvSubtotalSewa.text = formatRupiah(subtotalSewa)
        tvSubtotalSewaRincian.text = formatRupiah(subtotalSewa)

        updateGrandTotal()
    }

    private fun updateSubtotalProduk() {
        subtotalProduk = produkAdapter.getSelectedCartItems().sumOf { it.produk.harga * it.qty }
        tvSubtotalProduk.text = formatRupiah(subtotalProduk)
        tvSubtotalProdukRincian.text = formatRupiah(subtotalProduk)
    }

    private fun updateGrandTotal() {
        val total = subtotalSewa + subtotalProduk
        tvGrandTotal.text = formatRupiah(total)
    }

    private fun getSelectedDurasiMenit(): Int {
        return when (spinnerDurasi.selectedItemPosition) {
            0 -> 30
            1 -> 60
            2 -> 90
            3 -> 120
            4 -> 150
            5 -> 180
            else -> 60
        }.coerceAtMost(MAX_BOOKING_MINUTES)
    }

    private fun selectedJamMulaiForApi(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return if (!isReservasi && bookingMode == "sekarang") {
            sdf.format(Date())
        } else {
            sdf.format(selectedStartCalendar.time)
        }
    }

    private fun submitTransaksi() {
        if (idUser == 0) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        val sewaPayload = listOf(
            SewaRequest(
                idPs = idPs,
                jamMulai = selectedJamMulaiForApi(),
                durasiMenit = getSelectedDurasiMenit()
            )
        )

        val produkPayload = produkAdapter.getSelectedProduk().ifEmpty { null }

        val request = CreateTransaksiRequest(
            idUser = idUser,
            sumberTransaksi = "aplikasi",
            sewa = sewaPayload,
            produk = produkPayload
        )

        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.createTransaksi(
                    "Bearer $token",
                    "application/json",
                    request
                )

                if (response.isSuccessful) {
                    val msg = if (isReservasi) {
                        "Reservasi berhasil dibuat."
                    } else if (bookingMode == "nanti") {
                        "Booking terjadwal berhasil dibuat."
                    } else {
                        "Booking berhasil dibuat."
                    }

                    Toast.makeText(this@TransaksiActivity, msg, Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@TransaksiActivity,
                        "Gagal menyimpan transaksi: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TransaksiActivity,
                    "Gagal terhubung ke server: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun parseServerDateToCalendar(raw: String?): Calendar? {
        if (raw.isNullOrBlank()) return null

        val candidates = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )

        candidates.forEach { pattern ->
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.timeZone = if (pattern.contains("'Z'")) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
                val date = sdf.parse(raw)
                if (date != null) {
                    return Calendar.getInstance().apply { time = date }
                }
            } catch (_: Exception) {
            }
        }

        return null
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSimpan.isEnabled = !isLoading
        btnSimpan.alpha = if (isLoading) 0.7f else 1f
    }

    private fun formatRupiah(value: Long): String {
        return "Rp " + "%,d".format(Locale("id", "ID"), value).replace(',', '.')
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}