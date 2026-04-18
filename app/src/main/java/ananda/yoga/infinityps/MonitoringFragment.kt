package ananda.yoga.infinityps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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

    private lateinit var adapter: MonitoringAdapter
    private var currentUserId: Int = 0

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

    private fun fetchMonitoring() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMonitoring()

                Log.d("MONITORING", "CODE: ${response.code()}")

                if (response.isSuccessful) {
                    val items = response.body()?.data ?: emptyList()

                    items.forEach {
                        Log.d(
                            "MONITORING_DEBUG",
                            "ps=${it.nomorPs}, statusPs=${it.statusPs}, " +
                                    "statusTransaksi=${it.activeTransaksi?.statusTransaksi}, " +
                                    "statusBayar=${it.activeTransaksi?.pembayaran?.statusBayar}, " +
                                    "userId=${it.activeTransaksi?.user?.idUser}, currentUserId=$currentUserId"
                        )
                    }

                    adapter.submitList(items)

                    val total = items.size
                    val aktif = items.count { isPsSedangDipakai(it) }
                    val tersedia = items.count { isPsBisaDibooking(it) }

                    tvTotalPs.text = total.toString()
                    tvTotalAktif.text = aktif.toString()
                    tvTotalTersedia.text = tersedia.toString()

                    layoutEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
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
        rvMonitoring.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
}