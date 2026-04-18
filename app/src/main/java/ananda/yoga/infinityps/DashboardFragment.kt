package ananda.yoga.infinityps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

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

    private lateinit var cardBooking: LinearLayout
    private lateinit var cardMonitoring: LinearLayout
    private lateinit var cardRiwayat: LinearLayout
    private lateinit var cardProfil: LinearLayout

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout

    private var currentUserName: String = "Pelanggan"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvSubGreeting = view.findViewById(R.id.tvSubGreeting)

        tvTotalTransaksi = view.findViewById(R.id.tvTotalTransaksi)
        tvAktif = view.findViewById(R.id.tvAktif)
        tvSelesai = view.findViewById(R.id.tvSelesai)
        tvMenungguBayar = view.findViewById(R.id.tvMenungguBayar)

        tvPsTersedia = view.findViewById(R.id.tvPsTersedia)
        tvPsDipakai = view.findViewById(R.id.tvPsDipakai)
        tvInfoUtama = view.findViewById(R.id.tvInfoUtama)

        cardBooking = view.findViewById(R.id.cardBooking)
        cardMonitoring = view.findViewById(R.id.cardMonitoring)
        cardRiwayat = view.findViewById(R.id.cardRiwayat)
        cardProfil = view.findViewById(R.id.cardProfil)

        progressBar = view.findViewById(R.id.progressBar)
        contentLayout = view.findViewById(R.id.contentLayout)

        setupQuickMenu()
        loadSessionGreeting()
        fetchDashboardData()
    }

    override fun onResume() {
        super.onResume()
        fetchDashboardData()
    }

    private fun loadSessionGreeting() {
        val prefs = requireContext().getSharedPreferences("app_session", Context.MODE_PRIVATE)
        currentUserName = prefs.getString("name", "")?.takeIf { it.isNotBlank() } ?: "Pelanggan"

        tvGreeting.text = "Hai, $currentUserName 👋"
        tvSubGreeting.text = "Selamat datang kembali. Yuk cek status booking dan aktivitas kamu hari ini."
    }

    private fun setupQuickMenu() {
        cardBooking.setOnClickListener {
            Toast.makeText(requireContext(), "Silakan pilih PS dari Monitoring", Toast.LENGTH_SHORT).show()
        }

        cardMonitoring.setOnClickListener {
            (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
                ?.selectedItemId = R.id.itemHome
        }

        cardRiwayat.setOnClickListener {
            (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
                ?.selectedItemId = R.id.monitoring
        }

        cardProfil.setOnClickListener {
            (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
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
                    RetrofitClient.apiService.getMonitoring()
                }

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

        val transaksiAktif = items.firstOrNull {
            it.statusTransaksi.equals("aktif", true)
        }

        val transaksiMenungguValidasi = items.firstOrNull {
            it.pembayaran?.statusBayar?.equals("menunggu_validasi", true) == true
        }

        tvInfoUtama.text = when {
            transaksiMenungguValidasi != null ->
                "Pembayaran cash kamu sedang menunggu validasi admin."
            transaksiAktif != null ->
                "Kamu masih punya transaksi yang sedang berjalan."
            total == 0 ->
                "Belum ada transaksi. Yuk mulai booking PS."
            else ->
                "Semua aktivitasmu terlihat normal hari ini."
        }
    }

    private fun bindMonitoringInfo(items: List<PsMonitoringItem>) {
        val tersedia = items.count {
            val status = it.activeTransaksi?.statusTransaksi
            it.statusPs.equals("tersedia", true) &&
                    !status.equals("aktif", true) &&
                    !status.equals("waiting", true) &&
                    !status.equals("dijadwalkan", true) &&
                    !status.equals("menunggu_pembayaran", true)
        }

        val dipakai = items.count {
            it.statusPs.equals("digunakan", true) ||
                    it.activeTransaksi?.statusTransaksi.equals("aktif", true) ||
                    it.activeTransaksi?.statusTransaksi.equals("menunggu_pembayaran", true)
        }

        tvPsTersedia.text = tersedia.toString()
        tvPsDipakai.text = dipakai.toString()
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
}