package ananda.yoga.infinityps

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvSubGreeting: TextView

    private lateinit var tvTotalTransaksi: TextView
    private lateinit var tvAktif: TextView
    private lateinit var tvSelesai: TextView
    private lateinit var tvMenungguBayar: TextView

    private lateinit var tvHeroTransaksiAktif: TextView
    private lateinit var tvHeroTransaksiAktifInfo: TextView
    private lateinit var tvHeroPembayaran: TextView
    private lateinit var tvHeroPembayaranInfo: TextView

    private lateinit var tvInfoUtama: TextView
    private lateinit var tvInfoSecondary: TextView

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout

    private lateinit var layoutTipeContainer: LinearLayout
    private lateinit var sectionTipeWrapper: HorizontalScrollView
    private lateinit var layoutTransaksiSaya: LinearLayout

    private lateinit var layoutAduanShortcut: LinearLayout
    private lateinit var tvAduanStatus: TextView
    private lateinit var tvAduanJudul: TextView
    private lateinit var tvAduanInfo: TextView
    private lateinit var tvAduanCounter: TextView
    private lateinit var btnLihatAduan: TextView

    private var currentUserName: String = "Pelanggan"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupActions()
        loadSessionGreeting()
        fetchDashboardData()
    }

    override fun onResume() {
        super.onResume()
        fetchDashboardData()
    }

    private fun bindViews(view: View) {
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvSubGreeting = view.findViewById(R.id.tvSubGreeting)

        tvTotalTransaksi = view.findViewById(R.id.tvTotalTransaksi)
        tvAktif = view.findViewById(R.id.tvAktif)
        tvSelesai = view.findViewById(R.id.tvSelesai)
        tvMenungguBayar = view.findViewById(R.id.tvMenungguBayar)

        tvHeroTransaksiAktif = view.findViewById(R.id.tvHeroTransaksiAktif)
        tvHeroTransaksiAktifInfo = view.findViewById(R.id.tvHeroTransaksiAktifInfo)
        tvHeroPembayaran = view.findViewById(R.id.tvHeroPembayaran)
        tvHeroPembayaranInfo = view.findViewById(R.id.tvHeroPembayaranInfo)

        tvInfoUtama = view.findViewById(R.id.tvInfoUtama)
        tvInfoSecondary = view.findViewById(R.id.tvInfoSecondary)

        progressBar = view.findViewById(R.id.progressBar)
        contentLayout = view.findViewById(R.id.contentLayout)

        layoutTipeContainer = view.findViewById(R.id.layoutTipeContainer)
        sectionTipeWrapper = view.findViewById(R.id.sectionTipeWrapper)
        layoutTransaksiSaya = view.findViewById(R.id.layoutTransaksiSaya)

        layoutAduanShortcut = view.findViewById(R.id.layoutAduanShortcut)
        tvAduanStatus = view.findViewById(R.id.tvAduanStatus)
        tvAduanJudul = view.findViewById(R.id.tvAduanJudul)
        tvAduanInfo = view.findViewById(R.id.tvAduanInfo)
        tvAduanCounter = view.findViewById(R.id.tvAduanCounter)
        btnLihatAduan = view.findViewById(R.id.btnLihatAduan)
    }

    private fun setupActions() {
        layoutAduanShortcut.setOnClickListener {
            startActivity(Intent(requireContext(), PengaduanActivity::class.java))
        }

        btnLihatAduan.setOnClickListener {
            startActivity(Intent(requireContext(), PengaduanActivity::class.java))
        }
    }

    private fun loadSessionGreeting() {
        val prefs = requireContext().getSharedPreferences("app_session", Context.MODE_PRIVATE)
        currentUserName = prefs.getString("name", "")?.takeIf { it.isNotBlank() } ?: "Pelanggan"

        tvGreeting.text = "Halo, $currentUserName 👋"
        tvSubGreeting.text = "Lihat status transaksi, pembayaran, dan aduanmu dengan cepat."
    }

    private fun fetchDashboardData() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = requireContext().getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val transaksiDeferred = async {
                    RetrofitClient.apiService.getTransaksiSaya(
                        "Bearer $token",
                        "application/json"
                    )
                }

                val monitoringDeferred = async {
                    RetrofitClient.apiService.getMonitoring("Bearer $token")
                }

                val pengaduanDeferred = async {
                    RetrofitClient.apiService.getPengaduanSaya(
                        "Bearer $token",
                        "application/json"
                    )
                }

                val transaksiResponse = transaksiDeferred.await()
                val monitoringResponse = monitoringDeferred.await()
                val pengaduanResponse = pengaduanDeferred.await()

                val transaksiItems = if (transaksiResponse.isSuccessful) {
                    transaksiResponse.body()?.data ?: emptyList()
                } else {
                    emptyList()
                }

                val monitoringItems = if (monitoringResponse.isSuccessful) {
                    monitoringResponse.body()?.data ?: emptyList()
                } else {
                    emptyList()
                }

                val pengaduanItems = if (pengaduanResponse.isSuccessful) {
                    pengaduanResponse.body()?.data ?: emptyList()
                } else {
                    emptyList()
                }

                bindStatistikTransaksi(transaksiItems)
                bindMonitoringInfo(monitoringItems)
                bindTransaksiSaya(transaksiItems)
                bindPengaduanInfo(pengaduanItems)

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Gagal memuat dashboard: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun bindPengaduanInfo(items: List<PengaduanItem>) {
        val totalAduan = items.size
        val aktifCount = items.count {
            it.statusPengaduan == "pending" || it.statusPengaduan == "proses"
        }

        val latest = items.maxByOrNull { it.id }
        tvAduanCounter.text = aktifCount.toString()

        if (latest == null) {
            tvAduanStatus.text = "Belum Ada"
            tvAduanStatus.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_dashboard_chip_card)
            tvAduanStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )

            tvAduanJudul.text = "Belum ada aduan"
            tvAduanInfo.text =
                "Kalau ada kendala PS, pelayanan, pembayaran, atau fasilitas, kamu bisa langsung buat aduan dari sini."
            return
        }

        tvAduanStatus.text = statusPengaduanLabel(latest.statusPengaduan)
        applyAduanStatusStyle(latest.statusPengaduan)

        tvAduanJudul.text = "Kamu mengadukan: ${latest.judulPengaduan ?: "-"}"

        tvAduanInfo.text = when (latest.statusPengaduan) {
            "pending" ->
                "Aduan kamu sudah masuk dan sedang menunggu admin mengecek. Total aduan kamu: $totalAduan."
            "proses" ->
                "Aduan kamu sedang diproses admin. Cek detail untuk melihat catatan terbaru."
            "selesai" ->
                "Aduan terakhir kamu sudah selesai. Kamu tetap bisa melihat riwayat atau membuat aduan baru."
            "dibatalkan" ->
                "Aduan terakhir kamu dibatalkan. Buat aduan baru kalau kendala masih terjadi."
            else ->
                "Pantau perkembangan aduan kamu dari menu ini."
        }
    }

    private fun applyAduanStatusStyle(status: String?) {
        val ctx = requireContext()

        when (status?.lowercase()) {
            "pending" -> {
                tvAduanStatus.background =
                    ContextCompat.getDrawable(ctx, R.drawable.bg_badge_status_soft)
                tvAduanStatus.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_orange_dark))
            }
            "proses" -> {
                tvAduanStatus.background =
                    ContextCompat.getDrawable(ctx, R.drawable.bg_status_owned_active)
                tvAduanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_owned_active_text))
            }
            "selesai" -> {
                tvAduanStatus.background =
                    ContextCompat.getDrawable(ctx, R.drawable.bg_status_tersedia)
                tvAduanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_tersedia_text))
            }
            "dibatalkan" -> {
                tvAduanStatus.background =
                    ContextCompat.getDrawable(ctx, R.drawable.bg_status_danger)
                tvAduanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_danger_text))
            }
            else -> {
                tvAduanStatus.background =
                    ContextCompat.getDrawable(ctx, R.drawable.bg_dashboard_chip_card)
                tvAduanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            }
        }
    }

    private fun statusPengaduanLabel(status: String?): String {
        return when (status?.lowercase()) {
            "pending" -> "Pending"
            "proses" -> "Diproses"
            "selesai" -> "Selesai"
            "dibatalkan" -> "Dibatalkan"
            else -> "-"
        }
    }

    private fun normalizeStatusBayar(status: String?): String {
        val value = status?.trim()?.lowercase() ?: ""

        return when (value) {
            "paid", "success" -> "lunas"
            "pending", "menuggu" -> "menunggu"
            "menunggu_validasi" -> "menunggu validasi"
            "" -> "belum ada"
            else -> value
        }
    }

    private fun isStatusTransaksiMasihRelevan(status: String?): Boolean {
        val value = status?.trim()?.lowercase() ?: ""
        return value == "aktif" || value == "waiting" || value == "dijadwalkan"
    }

    private fun isPembayaranPerluDitindak(status: String?): Boolean {
        val value = normalizeStatusBayar(status)
        return value == "menunggu" || value == "menunggu validasi"
    }

    private fun isMasukBelumLunasCard(item: HistoryItem): Boolean {
        return isStatusTransaksiMasihRelevan(item.statusTransaksi) &&
                isPembayaranPerluDitindak(item.pembayaran?.statusBayar)
    }

    private fun bindStatistikTransaksi(items: List<HistoryItem>) {
        val total = items.size
        val aktif = items.count { it.statusTransaksi.equals("aktif", true) }
        val selesai = items.count { it.statusTransaksi.equals("selesai", true) }

        val belumLunasCount = items.count {
            isStatusTransaksiMasihRelevan(it.statusTransaksi) &&
                    normalizeStatusBayar(it.pembayaran?.statusBayar) == "menunggu"
        }

        val validasiCount = items.count {
            isStatusTransaksiMasihRelevan(it.statusTransaksi) &&
                    normalizeStatusBayar(it.pembayaran?.statusBayar) == "menunggu validasi"
        }

        val perluDitindakCount = belumLunasCount + validasiCount

        val aktifCount = items.count { it.statusTransaksi.equals("aktif", true) }
        val bookingCount = items.count {
            it.statusTransaksi.equals("waiting", true) ||
                    it.statusTransaksi.equals("dijadwalkan", true)
        }

        tvTotalTransaksi.text = total.toString()
        tvAktif.text = aktif.toString()
        tvSelesai.text = selesai.toString()
        tvMenungguBayar.text = perluDitindakCount.toString()

        tvHeroTransaksiAktif.text = aktifCount.toString()
        tvHeroTransaksiAktifInfo.text = when {
            aktifCount > 0 -> "$aktifCount transaksi sedang berjalan"
            bookingCount > 0 -> "$bookingCount booking sedang menunggu / dijadwalkan"
            else -> "Belum ada transaksi aktif"
        }

        tvHeroPembayaran.text = when {
            perluDitindakCount > 0 -> "Perlu Dicek"
            items.any {
                isStatusTransaksiMasihRelevan(it.statusTransaksi) &&
                        normalizeStatusBayar(it.pembayaran?.statusBayar) == "lunas"
            } -> "Aman"
            else -> "-"
        }

        tvHeroPembayaranInfo.text = when {
            perluDitindakCount > 0 ->
                "Belum lunas $belumLunasCount • Validasi $validasiCount"
            items.any {
                isStatusTransaksiMasihRelevan(it.statusTransaksi) &&
                        normalizeStatusBayar(it.pembayaran?.statusBayar) == "lunas"
            } -> "Pembayaran transaksi aktif aman"
            else -> "Belum ada pembayaran aktif"
        }

        tvInfoUtama.text = when {
            aktifCount > 0 && perluDitindakCount == 0 ->
                "Kamu punya transaksi aktif dan pembayarannya aman."
            aktifCount > 0 && perluDitindakCount > 0 ->
                "Ada transaksi aktif, cek pembayaran yang masih perlu diselesaikan."
            bookingCount > 0 && perluDitindakCount > 0 ->
                "Ada booking yang masih menunggu pembayaran atau validasi."
            bookingCount > 0 ->
                "Kamu punya booking yang sedang menunggu proses."
            total == 0 ->
                "Belum ada transaksi. Coba cek PS yang tersedia."
            else ->
                "Semua aktivitas kamu terlihat aman."
        }

        val transaksiTerbaru = items.maxByOrNull { it.idTransaksi }

        tvInfoSecondary.text = when {
            transaksiTerbaru != null ->
                "Transaksi terbaru #${transaksiTerbaru.idTransaksi} • total ${formatCurrency(transaksiTerbaru.totalHarga)}"
            else ->
                "Belum ada transaksi untuk ditampilkan."
        }
    }

    private fun bindTransaksiSaya(items: List<HistoryItem>) {
        layoutTransaksiSaya.removeAllViews()

        val filtered = items
            .filter { isMasukBelumLunasCard(it) }
            .sortedWith(
                compareByDescending<HistoryItem> {
                    when {
                        it.statusTransaksi.equals("aktif", true) -> 3
                        it.statusTransaksi.equals("waiting", true) -> 2
                        it.statusTransaksi.equals("dijadwalkan", true) -> 2
                        else -> 0
                    }
                }.thenByDescending { it.idTransaksi }
            )
            .take(3)

        if (filtered.isEmpty()) {
            val emptyCard = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(context, R.drawable.bg_dashboard_card)
                setPadding(dp(16), dp(16), dp(16), dp(16))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(requireContext()).apply {
                text = "Tidak ada transaksi yang perlu ditindak"
                textSize = 15f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                typeface = Typeface.DEFAULT_BOLD
            }

            val sub = TextView(requireContext()).apply {
                text = "Transaksi aktif yang belum lunas atau masih menunggu validasi akan muncul di sini."
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(0, dp(8), 0, 0)
            }

            emptyCard.addView(title)
            emptyCard.addView(sub)
            layoutTransaksiSaya.addView(emptyCard)
            return
        }

        filtered.forEachIndexed { index, item ->
            layoutTransaksiSaya.addView(createTransaksiCard(item))

            if (index < filtered.lastIndex) {
                val spacer = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(12)
                    )
                }
                layoutTransaksiSaya.addView(spacer)
            }
        }
    }

    private fun createTransaksiCard(item: HistoryItem): View {
        val context = requireContext()
        val paymentStatus = normalizeStatusBayar(item.pembayaran?.statusBayar)

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_dashboard_card)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val intent = Intent(requireContext(), DetailTransaksiActivity::class.java)
                intent.putExtra("id_transaksi", item.idTransaksi)
                startActivity(intent)
            }
        }

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val left = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvId = TextView(context).apply {
            text = "Transaksi #${item.idTransaksi}"
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            typeface = Typeface.DEFAULT_BOLD
        }

        val psName = item.detailSewa.firstOrNull()?.playstation?.nomorPs ?: "Tanpa PS"
        val tvPs = TextView(context).apply {
            text = psName
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.primary))
            setPadding(0, dp(6), 0, 0)
        }

        val tvTotal = TextView(context).apply {
            text = formatCurrency(item.totalHarga)
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, dp(6), 0, 0)
        }

        left.addView(tvId)
        left.addView(tvPs)
        left.addView(tvTotal)

        val right = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val tvStatus = TextView(context).apply {
            text = item.statusTransaksi.replaceFirstChar { it.uppercase() }
            textSize = 11f
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = ContextCompat.getDrawable(context, R.drawable.bg_dashboard_chip_card)
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }

        val tvBayar = TextView(context).apply {
            text = when (paymentStatus) {
                "menunggu validasi" -> "Menunggu Validasi"
                "menunggu" -> "Belum Lunas"
                "lunas" -> "Lunas"
                else -> "-"
            }
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, dp(8), 0, 0)
        }

        right.addView(tvStatus)
        right.addView(tvBayar)

        topRow.addView(left)
        topRow.addView(right)

        val tvHint = TextView(context).apply {
            text = "Tap untuk lihat detail transaksi"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, dp(12), 0, 0)
        }

        card.addView(topRow)
        card.addView(tvHint)

        return card
    }

    private fun bindMonitoringInfo(items: List<PsMonitoringItem>) {
        fun isSedangDipakai(item: PsMonitoringItem): Boolean {
            val statusTransaksi = item.activeTransaksi?.statusTransaksi?.lowercase().orEmpty()
            val statusPs = item.statusPs.lowercase()

            return statusPs == "digunakan" ||
                    statusTransaksi == "aktif" ||
                    statusTransaksi == "waiting" ||
                    statusTransaksi == "dijadwalkan"
        }

        fun isTersedia(item: PsMonitoringItem): Boolean {
            val statusPs = item.statusPs.lowercase()
            return statusPs == "tersedia" && !isSedangDipakai(item)
        }

        bindDynamicTipeCards(items, ::isTersedia)
    }

    private fun bindDynamicTipeCards(
        items: List<PsMonitoringItem>,
        isTersediaChecker: (PsMonitoringItem) -> Boolean
    ) {
        layoutTipeContainer.removeAllViews()

        val groupedByTipe = items
            .groupBy { item ->
                item.tipe?.namaTipe?.trim()?.ifBlank { "Lainnya" } ?: "Lainnya"
            }
            .toSortedMap(compareBy { it.uppercase() })

        if (groupedByTipe.isEmpty()) {
            sectionTipeWrapper.visibility = View.GONE
            return
        }

        sectionTipeWrapper.visibility = View.VISIBLE

        groupedByTipe.forEach { (namaTipe, list) ->
            val totalTipe = list.size
            val tersediaTipe = list.count { isTersediaChecker(it) }

            val card = createTipeCard(
                title = namaTipe,
                available = tersediaTipe,
                total = totalTipe
            )

            layoutTipeContainer.addView(card)
        }
    }

    private fun createTipeCard(title: String, available: Int, total: Int): View {
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_dashboard_chip_card)
            setPadding(dp(16), dp(16), dp(16), dp(16))

            layoutParams = LinearLayout.LayoutParams(
                dp(140),
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(12)
            }
        }

        val tvTitle = TextView(context).apply {
            text = title
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            typeface = Typeface.DEFAULT_BOLD
        }

        val tvAvailable = TextView(context).apply {
            text = "$available tersedia"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.status_tersedia_text))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(10), 0, 0)
        }

        val tvTotal = TextView(context).apply {
            text = "Total unit: $total"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, dp(6), 0, 0)
        }

        container.addView(tvTitle)
        container.addView(tvAvailable)
        container.addView(tvTotal)

        return container
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
}