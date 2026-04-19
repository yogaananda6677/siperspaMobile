package ananda.yoga.infinityps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RiwayatFragment : Fragment() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: HistoryAdapter

    private lateinit var etSearchHistory: EditText
    private lateinit var spinnerFilterTanggal: Spinner
    private lateinit var spinnerFilterBayar: Spinner
    private lateinit var spinnerFilterTransaksi: Spinner

    private var allItems: List<HistoryItem> = emptyList()
    private var searchQuery: String = ""

    companion object {
        private const val TAG = "RIWAYAT"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_riwayat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvHistory = view.findViewById(R.id.rvHistory)
        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)

        etSearchHistory = view.findViewById(R.id.etSearchHistory)
        spinnerFilterTanggal = view.findViewById(R.id.spinnerFilterTanggal)
        spinnerFilterBayar = view.findViewById(R.id.spinnerFilterBayar)
        spinnerFilterTransaksi = view.findViewById(R.id.spinnerFilterTransaksi)

        adapter = HistoryAdapter { item ->
            val intent = Intent(requireContext(), DetailTransaksiActivity::class.java)
            intent.putExtra("id_transaksi", item.idTransaksi)
            startActivity(intent)
        }

        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = adapter
        rvHistory.setHasFixedSize(true)

        setupFilters()
        setupSearch()

        fetchHistory()
    }

    override fun onResume() {
        super.onResume()
        fetchHistory()
    }

    private fun setupFilters() {
        val tanggalOptions = listOf(
            "Semua Tanggal",
            "Hari Ini",
            "7 Hari Terakhir",
            "30 Hari Terakhir"
        )

        val bayarOptions = listOf(
            "Semua Status Bayar",
            "Lunas",
            "Menunggu",
            "Menunggu Validasi",
            "Gagal"
        )

        val transaksiOptions = listOf(
            "Semua Status Transaksi",
            "Berjalan",
            "Menunggu Approval",
            "Dijadwalkan",
            "Selesai",
            "Dibatalkan",
        )

        spinnerFilterTanggal.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            tanggalOptions
        )

        spinnerFilterBayar.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            bayarOptions
        )

        spinnerFilterTransaksi.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            transaksiOptions
        )

        spinnerFilterTanggal.setSelection(0)
        spinnerFilterBayar.setSelection(0)
        spinnerFilterTransaksi.setSelection(0)

        val listener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                applyFilters()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        spinnerFilterTanggal.onItemSelectedListener = listener
        spinnerFilterBayar.onItemSelectedListener = listener
        spinnerFilterTransaksi.onItemSelectedListener = listener
    }

    private fun setupSearch() {
        etSearchHistory.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim()?.lowercase().orEmpty()
                applyFilters()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

    private fun fetchHistory() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = requireActivity()
                    .getSharedPreferences("app_session", Context.MODE_PRIVATE)

                val token = prefs.getString("token", "") ?: ""
                val idUser = prefs.getInt("id_user", 0)
                val isLoggedIn = prefs.getBoolean("is_logged_in", false)

                Log.d(TAG, "=== FETCH HISTORY START ===")
                Log.d(TAG, "is_logged_in = $isLoggedIn")
                Log.d(TAG, "id_user = $idUser")
                Log.d(TAG, "token kosong? = ${token.isBlank()}")
                Log.d(TAG, "token = $token")

                val response = RetrofitClient.apiService.getTransaksiSaya("Bearer $token")

                Log.d(TAG, "response code = ${response.code()}")
                Log.d(TAG, "response message = ${response.message()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    val items = body?.data ?: emptyList()

                    Log.d(TAG, "body = $body")
                    Log.d(TAG, "jumlah history = ${items.size}")

                    allItems = items.sortedByDescending { parseDateToMillis(it.tanggal) ?: 0L }
                    applyFilters()

                    if (items.isEmpty()) {
                        Log.d(TAG, "history kosong")
                    } else {
                        items.forEachIndexed { index, item ->
                            Log.d(
                                TAG,
                                "item[$index] id=${item.idTransaksi}, status=${item.statusTransaksi}, total=${item.totalHarga}"
                            )
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()

                    Log.e(TAG, "gagal memuat history")
                    Log.e(TAG, "code = ${response.code()}")
                    Log.e(TAG, "message = ${response.message()}")
                    Log.e(TAG, "errorBody = $errorBody")

                    Toast.makeText(
                        requireContext(),
                        "Gagal memuat history: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "exception saat fetch history", e)
                Log.e(TAG, "exception message = ${e.message}")

                Toast.makeText(
                    requireContext(),
                    "Gagal terhubung ke server: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

            } finally {
                Log.d(TAG, "=== FETCH HISTORY END ===")
                setLoading(false)
            }
        }
    }

    private fun applyFilters() {
        val tanggalFilter = spinnerFilterTanggal.selectedItem?.toString().orEmpty()
        val bayarFilter = spinnerFilterBayar.selectedItem?.toString().orEmpty()
        val transaksiFilter = spinnerFilterTransaksi.selectedItem?.toString().orEmpty()

        val filtered = allItems.filter { item ->
            matchesSearch(item) &&
                    matchesTanggal(item, tanggalFilter) &&
                    matchesBayar(item, bayarFilter) &&
                    matchesTransaksi(item, transaksiFilter)
        }

        adapter.submitList(filtered)
        layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvHistory.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun matchesSearch(item: HistoryItem): Boolean {
        if (searchQuery.isBlank()) return true

        val psName = item.detailSewa.firstOrNull()?.playstation?.nomorPs.orEmpty().lowercase()
        val produkText = item.detailProduk.joinToString(" ") { it.produk?.nama.orEmpty() }.lowercase()
        val tanggal = formatDate(item.tanggal).lowercase()
        val idTransaksi = item.idTransaksi.toString()
        val statusTransaksi = formatStatusTransaksi(item.statusTransaksi).lowercase()
        val statusBayar = formatStatusBayar(item.pembayaran?.statusBayar).lowercase()

        return idTransaksi.contains(searchQuery) ||
                psName.contains(searchQuery) ||
                produkText.contains(searchQuery) ||
                tanggal.contains(searchQuery) ||
                statusTransaksi.contains(searchQuery) ||
                statusBayar.contains(searchQuery)
    }

    private fun matchesTanggal(item: HistoryItem, filter: String): Boolean {
        if (filter == "Semua Tanggal") return true

        val itemMillis = parseDateToMillis(item.tanggal) ?: return false
        val now = Calendar.getInstance()

        return when (filter) {
            "Hari Ini" -> {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                itemMillis >= startOfDay.timeInMillis
            }
            "7 Hari Terakhir" -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                }
                itemMillis >= start.timeInMillis
            }
            "30 Hari Terakhir" -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                }
                itemMillis >= start.timeInMillis
            }
            else -> true
        }
    }

    private fun matchesBayar(item: HistoryItem, filter: String): Boolean {
        if (filter == "Semua Status Bayar") return true
        return formatStatusBayar(item.pembayaran?.statusBayar).equals(filter, true)
    }

    private fun matchesTransaksi(item: HistoryItem, filter: String): Boolean {
        if (filter == "Semua Status Transaksi") return true
        return formatStatusTransaksi(item.statusTransaksi).equals(filter, true)
    }

    private fun formatStatusTransaksi(value: String): String {
        return when (value.lowercase()) {
            "aktif" -> "Berjalan"
            "menunggu_pembayaran" -> "Menunggu Pembayaran"
            "waiting" -> "Menunggu Approval"
            "dijadwalkan" -> "Dijadwalkan"
            "selesai" -> "Selesai"
            "dibatalkan" -> "Dibatalkan"
            "ditolak" -> "Ditolak"
            else -> value.replaceFirstChar { it.uppercase() }
        }
    }

    private fun formatStatusBayar(value: String?): String {
        return when ((value ?: "menunggu").lowercase()) {
            "menunggu_validasi" -> "Menunggu Validasi"
            "menunggu" -> "Menunggu"
            "lunas" -> "Lunas"
            "gagal" -> "Gagal"
            else -> value?.replaceFirstChar { it.uppercase() } ?: "Menunggu"
        }
    }

    private fun formatDate(value: String): String {
        return try {
            val inputFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            )
            val output = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))

            for (format in inputFormats) {
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

    private fun parseDateToMillis(value: String): Long? {
        return try {
            val inputFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            )

            for (format in inputFormats) {
                try {
                    val parsed = format.parse(value)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            rvHistory.visibility = View.GONE
            layoutEmpty.visibility = View.GONE
        }
    }
}