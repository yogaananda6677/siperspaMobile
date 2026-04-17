package ananda.yoga.infinityps

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

        rvMonitoring = view.findViewById(R.id.rvMonitoring)
        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        tvTotalPs = view.findViewById(R.id.tvTotalPs)
        tvTotalAktif = view.findViewById(R.id.tvTotalAktif)
        tvTotalTersedia = view.findViewById(R.id.tvTotalTersedia)

        adapter = MonitoringAdapter { item ->
            openTransaksiPage(item)
        }

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
        return item.statusPs.equals("digunakan", true) ||
                item.activeTransaksi?.statusTransaksi.equals("aktif", true)
    }

    private fun isPsBisaDibooking(item: PsMonitoringItem): Boolean {
        val transaksiStatus = item.activeTransaksi?.statusTransaksi

        return item.statusPs.equals("tersedia", true) &&
                !transaksiStatus.equals("dijadwalkan", true) &&
                !transaksiStatus.equals("waiting", true) &&
                !transaksiStatus.equals("aktif", true)
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

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        rvMonitoring.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
}