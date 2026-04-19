package ananda.yoga.infinityps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MonitoringFragment : Fragment() {

    private lateinit var rvMonitoring: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvTotalPs: TextView
    private lateinit var tvTotalAktif: TextView
    private lateinit var tvTotalTersedia: TextView
    private lateinit var etSearch: EditText

    private lateinit var filterSemuaTipe: TextView
    private lateinit var filterPs3: TextView
    private lateinit var filterPs4: TextView
    private lateinit var filterPs5: TextView
    private lateinit var filterVip: TextView

    private lateinit var filterStatusSemua: TextView
    private lateinit var filterStatusTersedia: TextView
    private lateinit var filterStatusDipakai: TextView
    private lateinit var filterStatusPunyaku: TextView
    private lateinit var filterStatusBooking: TextView

    private lateinit var adapter: MonitoringAdapter
    private var currentUserId: Int = 0

    private var allItems: List<PsMonitoringItem> = emptyList()
    private var currentSearch: String = ""
    private var selectedTipe: String = "SEMUA"
    private var selectedStatus: String = "SEMUA"

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isAdded) {
                adapter.notifyDataSetChanged()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_monitoring, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("app_session", Context.MODE_PRIVATE)
        currentUserId = prefs.getInt("id_user", 0)

        rvMonitoring = view.findViewById(R.id.rvMonitoring)
        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        tvTotalPs = view.findViewById(R.id.tvTotalPs)
        tvTotalAktif = view.findViewById(R.id.tvTotalAktif)
        tvTotalTersedia = view.findViewById(R.id.tvTotalTersedia)
        etSearch = view.findViewById(R.id.etSearch)

        filterSemuaTipe = view.findViewById(R.id.filterSemuaTipe)
        filterPs3 = view.findViewById(R.id.filterPs3)
        filterPs4 = view.findViewById(R.id.filterPs4)
        filterPs5 = view.findViewById(R.id.filterPs5)
        filterVip = view.findViewById(R.id.filterVip)

        filterStatusSemua = view.findViewById(R.id.filterStatusSemua)
        filterStatusTersedia = view.findViewById(R.id.filterStatusTersedia)
        filterStatusDipakai = view.findViewById(R.id.filterStatusDipakai)
        filterStatusPunyaku = view.findViewById(R.id.filterStatusPunyaku)
        filterStatusBooking = view.findViewById(R.id.filterStatusBooking)

        adapter = MonitoringAdapter(
            currentUserId = currentUserId,
            onAvailableClick = { item ->
                openTransaksiPage(item)
            },
            onOwnedActiveClick = { item ->
                openDetailTransaksiPage(item)
            }
        )

        rvMonitoring.layoutManager = GridLayoutManager(requireContext(), 2)
        rvMonitoring.adapter = adapter

        setupSearch()
        setupFilters()
        updateTipeFilterUI()
        updateStatusFilterUI()

        fetchMonitoring()
    }

    override fun onResume() {
        super.onResume()
        fetchMonitoring()
        handler.post(timerRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timerRunnable)
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentSearch = s?.toString()?.trim().orEmpty()
                applyFilters()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

    private fun setupFilters() {
        filterSemuaTipe.setOnClickListener {
            selectedTipe = "SEMUA"
            updateTipeFilterUI()
            applyFilters()
        }
        filterPs3.setOnClickListener {
            selectedTipe = "PS3"
            updateTipeFilterUI()
            applyFilters()
        }
        filterPs4.setOnClickListener {
            selectedTipe = "PS4"
            updateTipeFilterUI()
            applyFilters()
        }
        filterPs5.setOnClickListener {
            selectedTipe = "PS5"
            updateTipeFilterUI()
            applyFilters()
        }
        filterVip.setOnClickListener {
            selectedTipe = "VIP"
            updateTipeFilterUI()
            applyFilters()
        }

        filterStatusSemua.setOnClickListener {
            selectedStatus = "SEMUA"
            updateStatusFilterUI()
            applyFilters()
        }
        filterStatusTersedia.setOnClickListener {
            selectedStatus = "TERSEDIA"
            updateStatusFilterUI()
            applyFilters()
        }
        filterStatusDipakai.setOnClickListener {
            selectedStatus = "DIPAKAI"
            updateStatusFilterUI()
            applyFilters()
        }
        filterStatusPunyaku.setOnClickListener {
            selectedStatus = "PUNYAKU"
            updateStatusFilterUI()
            applyFilters()
        }
        filterStatusBooking.setOnClickListener {
            selectedStatus = "BOOKING"
            updateStatusFilterUI()
            applyFilters()
        }
    }

    private fun updateTipeFilterUI() {
        setChipState(filterSemuaTipe, selectedTipe == "SEMUA")
        setChipState(filterPs3, selectedTipe == "PS3")
        setChipState(filterPs4, selectedTipe == "PS4")
        setChipState(filterPs5, selectedTipe == "PS5")
        setChipState(filterVip, selectedTipe == "VIP")
    }

    private fun updateStatusFilterUI() {
        setChipState(filterStatusSemua, selectedStatus == "SEMUA")
        setChipState(filterStatusTersedia, selectedStatus == "TERSEDIA")
        setChipState(filterStatusDipakai, selectedStatus == "DIPAKAI")
        setChipState(filterStatusPunyaku, selectedStatus == "PUNYAKU")
        setChipState(filterStatusBooking, selectedStatus == "BOOKING")
    }

    private fun setChipState(view: TextView, selected: Boolean) {
        val ctx = requireContext()
        if (selected) {
            view.setBackgroundResource(R.drawable.bg_status_owned_active)
            view.setTextColor(ContextCompat.getColor(ctx, R.color.status_owned_active_text))
        } else {
            view.setBackgroundResource(R.drawable.bg_form_card)
            view.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
        }
    }

    private fun fetchMonitoring() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMonitoring()

                Log.d("MONITORING", "CODE: ${response.code()}")

                if (response.isSuccessful) {
                    val items = response.body()?.data ?: emptyList()
                    allItems = items

                    items.forEach {
                        Log.d(
                            "MONITORING_DEBUG",
                            "ps=${it.nomorPs}, statusPs=${it.statusPs}, " +
                                    "statusTransaksi=${it.activeTransaksi?.statusTransaksi}, " +
                                    "statusBayar=${it.activeTransaksi?.pembayaran?.statusBayar}, " +
                                    "userId=${it.activeTransaksi?.user?.idUser}, currentUserId=$currentUserId"
                        )
                    }

                    applyFilters()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(
                        requireContext(),
                        "Gagal memuat data: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("MONITORING", "ERROR: $errorBody")
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Gagal terhubung ke server",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("MONITORING", "EXCEPTION", e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun applyFilters() {
        val filtered = allItems.filter { item ->
            matchesSearch(item) && matchesTipe(item) && matchesStatus(item)
        }

        adapter.submitList(filtered)

        tvTotalPs.text = allItems.size.toString()
        tvTotalAktif.text = allItems.count { isPsSedangDipakai(it) }.toString()
        tvTotalTersedia.text = allItems.count { isPsBisaDibooking(it) }.toString()

        layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvMonitoring.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun matchesSearch(item: PsMonitoringItem): Boolean {
        if (currentSearch.isBlank()) return true

        val keyword = currentSearch.lowercase()
        val nomor = item.nomorPs.lowercase()
        val tipe = item.tipe?.namaTipe?.lowercase().orEmpty()
        val status = item.statusPs.lowercase()

        return nomor.contains(keyword) || tipe.contains(keyword) || status.contains(keyword)
    }

    private fun matchesTipe(item: PsMonitoringItem): Boolean {
        if (selectedTipe == "SEMUA") return true
        return item.tipe?.namaTipe.equals(selectedTipe, true)
    }

    private fun matchesStatus(item: PsMonitoringItem): Boolean {
        return when (selectedStatus) {
            "TERSEDIA" -> isPsBisaDibooking(item)
            "DIPAKAI" -> isPsSedangDipakai(item)
            "PUNYAKU" -> isOwnedActive(item)
            "BOOKING" -> {
                val status = item.activeTransaksi?.statusTransaksi
                status.equals("waiting", true) ||
                        status.equals("dijadwalkan", true) ||
                        status.equals("menunggu_pembayaran", true)
            }
            else -> true
        }
    }

    private fun isPsSedangDipakai(item: PsMonitoringItem): Boolean {
        val transaksiStatus = item.activeTransaksi?.statusTransaksi
        return item.statusPs.equals("digunakan", true) ||
                transaksiStatus.equals("aktif", true) ||
                transaksiStatus.equals("menunggu_pembayaran", true)
    }

    private fun isPsBisaDibooking(item: PsMonitoringItem): Boolean {
        val transaksiStatus = item.activeTransaksi?.statusTransaksi
        return item.statusPs.equals("tersedia", true) &&
                !transaksiStatus.equals("dijadwalkan", true) &&
                !transaksiStatus.equals("waiting", true) &&
                !transaksiStatus.equals("aktif", true) &&
                !transaksiStatus.equals("menunggu_pembayaran", true)
    }

    private fun isOwnedActive(item: PsMonitoringItem): Boolean {
        val transaksi = item.activeTransaksi ?: return false
        val status = transaksi.statusTransaksi ?: return false

        return (status.equals("aktif", true) ||
                status.equals("menunggu_pembayaran", true)) &&
                transaksi.user?.idUser == currentUserId
    }

    private fun openTransaksiPage(item: PsMonitoringItem) {
        if (!isPsBisaDibooking(item)) return

        val intent = Intent(requireContext(), TransaksiActivity::class.java).apply {
            putExtra("id_ps", item.idPs)
            putExtra("nomor_ps", item.nomorPs)
            putExtra("id_tipe", item.tipe?.idTipe ?: 0)
            putExtra("nama_tipe", item.tipe?.namaTipe ?: "")
            putExtra("harga_sewa", item.tipe?.hargaSewa ?: 0L)
        }
        startActivity(intent)
    }

    private fun openDetailTransaksiPage(item: PsMonitoringItem) {
        if (!isOwnedActive(item)) return

        val transaksi = item.activeTransaksi ?: return

        val intent = Intent(requireContext(), DetailTransaksiActivity::class.java).apply {
            putExtra("id_transaksi", transaksi.idTransaksi)
        }
        startActivity(intent)
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            rvMonitoring.visibility = View.GONE
            layoutEmpty.visibility = View.GONE
        }
    }
}