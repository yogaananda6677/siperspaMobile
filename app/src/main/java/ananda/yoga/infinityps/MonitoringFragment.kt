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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

    private lateinit var adapter: MonitoringZoneAdapter
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
        return inflater.inflate(R.layout.fragment_monitoring_zone, container, false)
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
        adapter = MonitoringZoneAdapter(
            currentUserId = currentUserId,
            onAvailableClick = { item -> openTransaksiPage(item) },
            onOwnedTransactionClick = { item -> openDetailTransaksiPage(item) }
        )

        val spanCount = 5
        val layoutManager = GridLayoutManager(requireContext(), spanCount)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.getItemViewType(position)) {
                    MonitoringZoneAdapter.VIEW_TYPE_HEADER -> spanCount
                    else -> 1
                }
            }
        }

        rvMonitoring.layoutManager = layoutManager
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
            "RESERVASI" to "Reservasi",
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
            .sortedBy { it.uppercase(Locale.getDefault()) }

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
            if (isSelected) setTypeface(typeface, android.graphics.Typeface.BOLD)
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
                val prefs = requireContext().getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.getMonitoring("Bearer $token")

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
                    "Gagal terhubung ke server: ${e.message}",
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

        adapter.submitList(buildZoneItems(filtered))

        tvTotalPs.text = allItems.size.toString()
        tvTotalAktif.text = allItems.count { isPsSedangDipakai(it) }.toString()
        tvTotalTersedia.text = allItems.count { canCustomerBook(item = it) }.toString()

        layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvMonitoring.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun buildZoneItems(items: List<PsMonitoringItem>): List<MonitoringZoneListItem> {
        val grouped = items
            .sortedWith(compareBy({ extractZoneKey(it.nomorPs) }, { it.nomorPs }))
            .groupBy { extractZoneKey(it.nomorPs) }

        val result = mutableListOf<MonitoringZoneListItem>()

        grouped.toSortedMap().forEach { (zone, zoneItems) ->
            result.add(MonitoringZoneListItem.Header(zone))
            zoneItems.sortedBy { it.nomorPs }.forEach { ps ->
                result.add(MonitoringZoneListItem.PsItem(ps))
            }
        }

        return result
    }

    private fun extractZoneKey(nomorPs: String): String {
        val clean = nomorPs.trim()
        val prefix = clean.takeWhile { it.isLetter() }.uppercase(Locale.getDefault())
        return if (prefix.isBlank()) "Zona Lainnya" else "Zona $prefix"
    }

    private fun matchesSearch(item: PsMonitoringItem): Boolean {
        if (currentSearch.isBlank()) return true

        val keyword = currentSearch.lowercase()
        val nomor = item.nomorPs.lowercase()
        val tipe = item.tipe?.namaTipe?.lowercase().orEmpty()
        val statusPs = item.statusPs.lowercase()
        val statusTransaksi = item.activeTransaksi?.statusTransaksi?.lowercase().orEmpty()
        val statusBayar = item.activeTransaksi?.pembayaran?.statusBayar?.lowercase().orEmpty()

        return nomor.contains(keyword) ||
                tipe.contains(keyword) ||
                statusPs.contains(keyword) ||
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
            "TERSEDIA" -> item.statusPs.equals("tersedia", true)
            "RESERVASI" -> isReservableSoon(item)
            "DIPAKAI" -> isPsSedangDipakai(item)
            "PUNYAKU" -> isOwnedTransaction(item)
            "BOOKING" -> status.equals("waiting", true) || status.equals("dijadwalkan", true)
            "VALIDASI_CASH" -> statusBayar.equals("menunggu_validasi", true)
            "MAINTENANCE" -> item.statusPs.equals("maintenance", true)
            else -> true
        }
    }

    private fun isPsSedangDipakai(item: PsMonitoringItem): Boolean {
        val transaksiStatus = item.activeTransaksi?.statusTransaksi
        return item.statusPs.equals("digunakan", true) || transaksiStatus.equals("aktif", true)
    }

    private fun isOwnedTransaction(item: PsMonitoringItem): Boolean {
        val transaksi = item.activeTransaksi ?: return false
        val status = transaksi.statusTransaksi ?: return false

        return (
                status.equals("waiting", true) ||
                        status.equals("dijadwalkan", true) ||
                        status.equals("aktif", true)
                ) && transaksi.user?.idUser == currentUserId
    }

    private fun canCustomerBook(item: PsMonitoringItem): Boolean {
        val transaksiStatus = item.activeTransaksi?.statusTransaksi

        if (item.statusPs.equals("maintenance", true)) return false
        if (transaksiStatus.equals("waiting", true) || transaksiStatus.equals("dijadwalkan", true)) return false

        return item.statusPs.equals("tersedia", true) || isReservableSoon(item)
    }

    private fun isReservableSoon(item: PsMonitoringItem): Boolean {
        val transaksi = item.activeTransaksi ?: return false
        if (!transaksi.statusTransaksi.equals("aktif", true)) return false

        val sewa = transaksi.detailSewa.firstOrNull { it.idPs == item.idPs } ?: return false
        val remaining = getRemainingSeconds(sewa)

        return remaining in 1..1800
    }

    private fun getRemainingSeconds(sewa: DetailSewa): Long {
        if (sewa.sisaDetik > 0) return sewa.sisaDetik
        return parseServerDateToMillis(sewa.jamSelesai)?.let { end ->
            maxOf(0L, (end - System.currentTimeMillis()) / 1000)
        } ?: 0L
    }

    private fun parseServerDateToMillis(raw: String?): Long? {
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
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }

        return null
    }

    private fun getRemainingSecondsForItem(item: PsMonitoringItem): Long {
        val sewa = item.activeTransaksi?.detailSewa?.firstOrNull { it.idPs == item.idPs } ?: return 0L
        return if (sewa.sisaDetik > 0) sewa.sisaDetik else 0L
    }

    private fun openTransaksiPage(item: PsMonitoringItem) {
        val transaksi = item.activeTransaksi
        val sewa = transaksi?.detailSewa?.firstOrNull { it.idPs == item.idPs }

        val isReservasi =
            transaksi?.statusTransaksi.equals("aktif", true) &&
                    item.statusPs.equals("digunakan", true) &&
                    sewa != null &&
                    getRemainingSecondsForItem(item) in 1..(50 * 60)

        val intent = Intent(requireContext(), TransaksiActivity::class.java).apply {
            putExtra("id_ps", item.idPs)
            putExtra("nomor_ps", item.nomorPs)
            putExtra("id_tipe", item.tipe?.idTipe ?: 0)
            putExtra("nama_tipe", item.tipe?.namaTipe ?: "")
            putExtra("harga_sewa", item.tipe?.hargaSewa ?: 0L)

            putExtra("is_reservasi", isReservasi)
            putExtra("active_jam_selesai", sewa?.jamSelesai ?: "")
        }
        startActivity(intent)
    }

    private fun openDetailTransaksiPage(item: PsMonitoringItem) {
        if (!isOwnedTransaction(item)) return

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