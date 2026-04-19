package ananda.yoga.infinityps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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

    private lateinit var chipTipeContainer: LinearLayout
    private lateinit var chipStatusContainer: LinearLayout
    private lateinit var scrollTipe: HorizontalScrollView

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

        bindViews(view)
        setupRecyclerView()
        setupSearch()
        setupStatusFilters()
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

    private fun bindViews(view: View) {
        rvMonitoring = view.findViewById(R.id.rvMonitoring)
        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)

        tvTotalPs = view.findViewById(R.id.tvTotalPs)
        tvTotalAktif = view.findViewById(R.id.tvTotalAktif)
        tvTotalTersedia = view.findViewById(R.id.tvTotalTersedia)
        etSearch = view.findViewById(R.id.etSearch)

        chipTipeContainer = view.findViewById(R.id.chipTipeContainer)
        chipStatusContainer = view.findViewById(R.id.chipStatusContainer)
        scrollTipe = view.findViewById(R.id.scrollTipe)
    }

    private fun setupRecyclerView() {
        adapter = MonitoringAdapter(
            currentUserId = currentUserId,
            onAvailableClick = { item -> openTransaksiPage(item) },
            onOwnedActiveClick = { item -> openDetailTransaksiPage(item) }
        )

        rvMonitoring.layoutManager = LinearLayoutManager(requireContext())
        rvMonitoring.adapter = adapter
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

    private fun setupStatusFilters() {
        renderStatusChips()
    }

    private fun renderStatusChips() {
        chipStatusContainer.removeAllViews()

        val statuses = listOf(
            "SEMUA" to "Semua",
            "TERSEDIA" to "Tersedia",
            "DIPAKAI" to "Dipakai",
            "PUNYAKU" to "Punyaku",
            "BOOKING" to "Booking",
            "VALIDASI_CASH" to "Validasi Cash",
            "MAINTENANCE" to "Maintenance"
        )

        statuses.forEach { (value, label) ->
            chipStatusContainer.addView(
                createChip(
                    text = label,
                    isSelected = selectedStatus == value
                ) {
                    selectedStatus = value
                    renderStatusChips()
                    applyFilters()
                }
            )
        }
    }

    private fun renderDynamicTipeChips(items: List<PsMonitoringItem>) {
        chipTipeContainer.removeAllViews()

        val tipeList = items.mapNotNull { it.tipe?.namaTipe?.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.uppercase() }

        chipTipeContainer.addView(
            createChip(
                text = "Semua Tipe",
                isSelected = selectedTipe == "SEMUA"
            ) {
                selectedTipe = "SEMUA"
                renderDynamicTipeChips(allItems)
                applyFilters()
            }
        )

        tipeList.forEach { tipeName ->
            chipTipeContainer.addView(
                createChip(
                    text = tipeName,
                    isSelected = selectedTipe.equals(tipeName, true)
                ) {
                    selectedTipe = tipeName
                    renderDynamicTipeChips(allItems)
                    applyFilters()
                }
            )
        }

        scrollTipe.visibility = View.VISIBLE
    }

    private fun createChip(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 12f
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = ContextCompat.getDrawable(
                context,
                if (isSelected) R.drawable.bg_status_owned_active else R.drawable.bg_form_card
            )
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isSelected) R.color.status_owned_active_text else R.color.text_secondary
                )
            )
            if (isSelected) {
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(8)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun fetchMonitoring() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMonitoring()

                if (response.isSuccessful) {
                    val items = response.body()?.data ?: emptyList()
                    allItems = items
                    renderDynamicTipeChips(items)
                    renderStatusChips()
                    applyFilters()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Gagal memuat data: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Gagal terhubung ke server",
                    Toast.LENGTH_SHORT
                ).show()
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
        val statusPs = item.statusPs.lowercase()
        val user = item.activeTransaksi?.user?.name?.lowercase().orEmpty()
        val username = item.activeTransaksi?.user?.username?.lowercase().orEmpty()
        val statusTransaksi = item.activeTransaksi?.statusTransaksi?.lowercase().orEmpty()
        val statusBayar = item.activeTransaksi?.pembayaran?.statusBayar?.lowercase().orEmpty()

        return nomor.contains(keyword) ||
                tipe.contains(keyword) ||
                statusPs.contains(keyword) ||
                user.contains(keyword) ||
                username.contains(keyword) ||
                statusTransaksi.contains(keyword) ||
                statusBayar.contains(keyword)
    }

    private fun matchesTipe(item: PsMonitoringItem): Boolean {
        if (selectedTipe == "SEMUA") return true
        return item.tipe?.namaTipe.equals(selectedTipe, true)
    }

    private fun matchesStatus(item: PsMonitoringItem): Boolean {
        val transaksi = item.activeTransaksi
        val status = transaksi?.statusTransaksi
        val statusBayar = transaksi?.pembayaran?.statusBayar

        return when (selectedStatus) {
            "TERSEDIA" -> isPsBisaDibooking(item)
            "DIPAKAI" -> isPsSedangDipakai(item)
            "PUNYAKU" -> isOwnedActive(item)
            "BOOKING" -> {
                status.equals("waiting", true) ||
                        status.equals("dijadwalkan", true)
            }
            "VALIDASI_CASH" -> statusBayar.equals("menunggu_validasi", true)
            "MAINTENANCE" -> item.statusPs.equals("maintenance", true)
            else -> true
        }
    }

    private fun isPsSedangDipakai(item: PsMonitoringItem): Boolean {
        val transaksiStatus = item.activeTransaksi?.statusTransaksi
        return item.statusPs.equals("digunakan", true) ||
                transaksiStatus.equals("aktif", true)
    }

    private fun isPsBisaDibooking(item: PsMonitoringItem): Boolean {
        val transaksiStatus = item.activeTransaksi?.statusTransaksi
        return item.statusPs.equals("tersedia", true) &&
                !transaksiStatus.equals("dijadwalkan", true) &&
                !transaksiStatus.equals("waiting", true) &&
                !transaksiStatus.equals("aktif", true)
    }

    private fun isOwnedActive(item: PsMonitoringItem): Boolean {
        val transaksi = item.activeTransaksi ?: return false
        val status = transaksi.statusTransaksi ?: return false

        return status.equals("aktif", true) &&
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}