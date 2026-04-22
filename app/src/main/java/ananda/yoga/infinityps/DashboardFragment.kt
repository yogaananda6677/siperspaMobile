package ananda.yoga.infinityps

import android.content.Context
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
import com.google.android.material.bottomnavigation.BottomNavigationView
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

    private lateinit var tvPsTersedia: TextView
    private lateinit var tvPsDipakai: TextView
    private lateinit var tvInfoUtama: TextView
    private lateinit var tvInfoSecondary: TextView

    private lateinit var cardMonitoring: LinearLayout
    private lateinit var cardRiwayat: LinearLayout
    private lateinit var cardProfil: LinearLayout

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout

    private lateinit var layoutTipeContainer: LinearLayout
    private lateinit var sectionTipeWrapper: HorizontalScrollView

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
        setupQuickMenu()
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

        tvPsTersedia = view.findViewById(R.id.tvPsTersedia)
        tvPsDipakai = view.findViewById(R.id.tvPsDipakai)
        tvInfoUtama = view.findViewById(R.id.tvInfoUtama)
        tvInfoSecondary = view.findViewById(R.id.tvInfoSecondary)

        cardMonitoring = view.findViewById(R.id.cardMonitoring)
        cardRiwayat = view.findViewById(R.id.cardRiwayat)
        cardProfil = view.findViewById(R.id.cardProfil)

        progressBar = view.findViewById(R.id.progressBar)
        contentLayout = view.findViewById(R.id.contentLayout)

        layoutTipeContainer = view.findViewById(R.id.layoutTipeContainer)
        sectionTipeWrapper = view.findViewById(R.id.sectionTipeWrapper)
    }

    private fun loadSessionGreeting() {
        val prefs = requireContext().getSharedPreferences("app_session", Context.MODE_PRIVATE)
        currentUserName = prefs.getString("name", "")?.takeIf { it.isNotBlank() } ?: "Pelanggan"

        tvGreeting.text = "Halo, $currentUserName 👋"
        tvSubGreeting.text = "Pantau booking, status PlayStation, dan aktivitas terbaru kamu dengan cepat."
    }

    private fun setupQuickMenu() {
        cardMonitoring.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                ?.selectedItemId = R.id.monitoring
        }

        cardRiwayat.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                ?.selectedItemId = R.id.riwayat
        }

        cardProfil.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                ?.selectedItemId = R.id.profil
        }
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
                    RetrofitClient.apiService.getMonitoring("Bearer $token")                }

                val transaksiResponse = transaksiDeferred.await()
                val monitoringResponse = monitoringDeferred.await()

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

                bindStatistikTransaksi(transaksiItems)
                bindMonitoringInfo(monitoringItems)

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

    private fun bindStatistikTransaksi(items: List<HistoryItem>) {
        val total = items.size
        val aktif = items.count { it.statusTransaksi.equals("aktif", true) }
        val selesai = items.count { it.statusTransaksi.equals("selesai", true) }
        val menungguBayar = items.count {
            val statusTransaksi = it.statusTransaksi.lowercase()
            val statusBayar = it.pembayaran?.statusBayar?.lowercase() ?: "menunggu"

            (statusTransaksi == "aktif" || statusTransaksi == "menunggu_pembayaran") &&
                    statusBayar != "lunas"
        }

        tvTotalTransaksi.text = total.toString()
        tvAktif.text = aktif.toString()
        tvSelesai.text = selesai.toString()
        tvMenungguBayar.text = menungguBayar.toString()

        val transaksiAktif = items.firstOrNull { it.statusTransaksi.equals("aktif", true) }
        val transaksiMenungguValidasi = items.firstOrNull {
            it.pembayaran?.statusBayar?.equals("menunggu_validasi", true) == true
        }

        val transaksiTerakhir = items.maxByOrNull { it.totalHarga }

        tvInfoUtama.text = when {
            transaksiMenungguValidasi != null ->
                "Pembayaran kamu masih menunggu validasi admin."
            transaksiAktif != null ->
                "Ada transaksi aktif yang sedang berjalan sekarang."
            total == 0 ->
                "Belum ada transaksi. Coba cek PlayStation yang tersedia."
            else ->
                "Semua aktivitas terlihat aman dan terkendali."
        }

        tvInfoSecondary.text = when {
            transaksiTerakhir != null ->
                "Total transaksi tertinggi: ${formatCurrency(transaksiTerakhir.totalHarga)}"
            else ->
                "Belum ada ringkasan transaksi untuk ditampilkan."
        }
    }

    private fun bindMonitoringInfo(items: List<PsMonitoringItem>) {
        fun isSedangDipakai(item: PsMonitoringItem): Boolean {
            val statusTransaksi = item.activeTransaksi?.statusTransaksi?.lowercase().orEmpty()
            val statusPs = item.statusPs.lowercase()

            return statusPs == "digunakan" ||
                    statusTransaksi == "aktif" ||
                    statusTransaksi == "menunggu_pembayaran" ||
                    statusTransaksi == "waiting" ||
                    statusTransaksi == "dijadwalkan"
        }

        fun isTersedia(item: PsMonitoringItem): Boolean {
            val statusPs = item.statusPs.lowercase()
            return statusPs == "tersedia" && !isSedangDipakai(item)
        }

        val tersedia = items.count { isTersedia(it) }
        val dipakai = items.count { isSedangDipakai(it) }

        tvPsTersedia.text = tersedia.toString()
        tvPsDipakai.text = dipakai.toString()

        bindDynamicTipeCards(items, ::isTersedia)
    }

    /**
     * Logika tipe yang benar:
     * - ambil dari relasi database: ps -> tipe -> namaTipe
     * - jangan jadikan PS3/PS4/PS5/VIP sebagai sumber kebenaran
     * - UI mengikuti hasil grouping dari API
     */
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
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val tvAvailable = TextView(context).apply {
            text = "$available tersedia"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.status_tersedia_text))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
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