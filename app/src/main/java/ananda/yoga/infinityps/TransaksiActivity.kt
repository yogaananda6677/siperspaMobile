package ananda.yoga.infinityps

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransaksiActivity : AppCompatActivity() {

    private lateinit var tvNomorPs: TextView
    private lateinit var tvNamaTipe: TextView
    private lateinit var tvHargaPerJam: TextView

    private lateinit var rgBookingMode: RadioGroup
    private lateinit var rbBookingSekarang: RadioButton
    private lateinit var rbBookingNanti: RadioButton
    private lateinit var tvJamMulai: TextView
    private lateinit var btnPilihJam: Button

    private lateinit var spinnerDurasi: Spinner
    private lateinit var btnHitungSewa: Button
    private lateinit var tvJamSelesai: TextView
    private lateinit var tvSubtotalSewa: TextView

    private lateinit var rvProduk: RecyclerView
    private lateinit var btnHitungProduk: Button
    private lateinit var tvSubtotalProduk: TextView

    private lateinit var tvSubtotalSewaRincian: TextView
    private lateinit var tvSubtotalProdukRincian: TextView
    private lateinit var tvGrandTotal: TextView

    private lateinit var btnSimpan: Button
    private lateinit var progressBar: ProgressBar

    private var idPs: Int = 0
    private var nomorPs: String = "-"
    private var namaTipe: String = "-"
    private var hargaSewa: Long = 0L
    private var idUser: Int = 0

    private val produkItems = mutableListOf<CartProdukItem>()
    private lateinit var produkAdapter: ProdukCustomerAdapter

    private var subtotalSewa: Long = 0L
    private var subtotalProduk: Long = 0L

    private var bookingMode: String = "sekarang"
    private var selectedStartCalendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaksi)

        bindViews()
        readIntent()
        setupHeader()
        setupSpinner()
        setupBookingMode()
        setupProdukList()
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
        btnHitungSewa = findViewById(R.id.btnHitungSewa)
        tvJamSelesai = findViewById(R.id.tvJamSelesai)
        tvSubtotalSewa = findViewById(R.id.tvSubtotalSewa)

        rvProduk = findViewById(R.id.rvProduk)
        btnHitungProduk = findViewById(R.id.btnHitungProduk)
        tvSubtotalProduk = findViewById(R.id.tvSubtotalProduk)

        tvSubtotalSewaRincian = findViewById(R.id.tvSubtotalSewaRincian)
        tvSubtotalProdukRincian = findViewById(R.id.tvSubtotalProdukRincian)
        tvGrandTotal = findViewById(R.id.tvGrandTotal)

        btnSimpan = findViewById(R.id.btnSimpanTransaksi)
        progressBar = findViewById(R.id.progressBarTransaksi)
    }

    private fun readIntent() {
        idPs = intent.getIntExtra("id_ps", 0)
        nomorPs = intent.getStringExtra("nomor_ps") ?: "-"
        namaTipe = intent.getStringExtra("nama_tipe") ?: "-"
        hargaSewa = intent.getLongExtra("harga_sewa", 0L)

        val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
        idUser = prefs.getInt("id_user", 0)
    }

    private fun setupHeader() {
        tvNomorPs.text = nomorPs
        tvNamaTipe.text = namaTipe
        tvHargaPerJam.text = "${formatRupiah(hargaSewa)}/jam"
    }

    private fun setupSpinner() {
        val durasiList = listOf("30 menit", "60 menit", "90 menit", "120 menit", "180 menit")
        val spinnerAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, durasiList)
        spinnerDurasi.adapter = spinnerAdapter
    }

    private fun setupBookingMode() {
        bookingMode = "sekarang"
        rbBookingSekarang.isChecked = true
        btnPilihJam.visibility = View.GONE
        selectedStartCalendar = Calendar.getInstance()

        updateJamMulaiLabel()

        rgBookingMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbBookingSekarang -> {
                    bookingMode = "sekarang"
                    btnPilihJam.visibility = View.GONE
                    selectedStartCalendar = Calendar.getInstance()
                    updateJamMulaiLabel()
                    hitungSewa()
                }

                R.id.rbBookingNanti -> {
                    bookingMode = "nanti"
                    btnPilihJam.visibility = View.VISIBLE

                    val cal = Calendar.getInstance()
                    cal.add(Calendar.MINUTE, 30)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    selectedStartCalendar = cal

                    updateJamMulaiLabel()
                    hitungSewa()
                }
            }
        }

        btnPilihJam.setOnClickListener {
            showTimePicker()
        }
    }

    private fun showTimePicker() {
        val hour = selectedStartCalendar.get(Calendar.HOUR_OF_DAY)
        val minute = selectedStartCalendar.get(Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val now = Calendar.getInstance()
                val picked = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (picked.before(now)) {
                    picked.add(Calendar.DAY_OF_MONTH, 1)
                }

                selectedStartCalendar = picked
                updateJamMulaiLabel()
                hitungSewa()
            },
            hour,
            minute,
            true
        ).show()
    }

    private fun updateJamMulaiLabel() {
        tvJamMulai.text = if (bookingMode == "sekarang") {
            "Sekarang"
        } else {
            val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))
            sdf.format(selectedStartCalendar.time)
        }
    }

    private fun setupProdukList() {
        produkAdapter = ProdukCustomerAdapter(produkItems) {
            updateSubtotalProduk()
            updateGrandTotal()
        }

        rvProduk.layoutManager = LinearLayoutManager(this)
        rvProduk.adapter = produkAdapter
    }

    private fun setupActions() {
        btnHitungSewa.setOnClickListener {
            hitungSewa()
            Toast.makeText(this, "Subtotal sewa diperbarui", Toast.LENGTH_SHORT).show()
        }

        btnHitungProduk.setOnClickListener {
            updateSubtotalProduk()
            updateGrandTotal()
            Toast.makeText(this, "Subtotal produk diperbarui", Toast.LENGTH_SHORT).show()
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
                    produkItems.clear()
                    produkItems.addAll(data.map { CartProdukItem(it, 0) })
                    produkAdapter.notifyDataSetChanged()
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

    private fun hitungSewa() {
        val durasiMenit = getSelectedDurasiMenit()
        subtotalSewa = ((hargaSewa / 60.0) * durasiMenit).toLong()

        val mulaiMillis = if (bookingMode == "sekarang") {
            System.currentTimeMillis()
        } else {
            selectedStartCalendar.timeInMillis
        }

        val selesai = Date(mulaiMillis + (durasiMenit * 60 * 1000L))
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        tvJamSelesai.text = sdf.format(selesai)
        tvSubtotalSewa.text = formatRupiah(subtotalSewa)
        tvSubtotalSewaRincian.text = formatRupiah(subtotalSewa)

        updateGrandTotal()
    }

    private fun updateSubtotalProduk() {
        subtotalProduk = produkItems.sumOf { it.produk.harga * it.qty }
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
            4 -> 180
            else -> 60
        }
    }

    private fun selectedJamMulaiForApi(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return if (bookingMode == "sekarang") {
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

        val produkPayload = produkItems
            .filter { it.qty > 0 }
            .map {
                ProdukRequest(
                    idProduk = it.produk.idProduk,
                    qty = it.qty
                )
            }
            .ifEmpty { null }

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

                Log.d("BOOKING", "TOKEN=$token")
                Log.d("BOOKING", "ID_USER=$idUser")
                Log.d("BOOKING", "ID_PS=$idPs")
                Log.d("BOOKING", "JAM_MULAI=${selectedJamMulaiForApi()}")
                Log.d("BOOKING", "REQUEST=$request")

                val response = RetrofitClient.apiService.createTransaksi(
                    "Bearer $token",
                    "application/json",
                    request
                )

                if (response.isSuccessful) {
                    val msg = if (bookingMode == "sekarang") {
                        "Booking sekarang berhasil dibuat."
                    } else {
                        "Booking untuk nanti berhasil dibuat."
                    }

                    Toast.makeText(this@TransaksiActivity, msg, Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("BOOKING", "CODE=${response.code()}")
                    Log.e("BOOKING", "ERROR=$errorBody")

                    Toast.makeText(
                        this@TransaksiActivity,
                        "Gagal menyimpan transaksi: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("BOOKING", "EXCEPTION", e)
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

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSimpan.isEnabled = !isLoading
    }

    private fun formatRupiah(value: Long): String {
        return "Rp " + "%,d".format(Locale("id", "ID"), value).replace(',', '.')
    }
}