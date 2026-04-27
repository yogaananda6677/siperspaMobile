package ananda.yoga.infinityps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class PengaduanActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnTambahAduan: Button
    private lateinit var recyclerPengaduan: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotalAduan: TextView
    private lateinit var tvProsesAduan: TextView
    private lateinit var tvSelesaiAduan: TextView

    private lateinit var adapter: PengaduanAdapter
    private val pengaduanItems = mutableListOf<PengaduanItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengaduan)

        bindViews()
        setupRecycler()
        setupActions()
        loadPengaduan()
    }

    override fun onResume() {
        super.onResume()
        loadPengaduan()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        btnTambahAduan = findViewById(R.id.btnTambahAduan)
        recyclerPengaduan = findViewById(R.id.recyclerPengaduan)
        progressBar = findViewById(R.id.progressBarPengaduan)
        tvEmpty = findViewById(R.id.tvEmptyPengaduan)
        tvTotalAduan = findViewById(R.id.tvTotalAduan)
        tvProsesAduan = findViewById(R.id.tvProsesAduan)
        tvSelesaiAduan = findViewById(R.id.tvSelesaiAduan)
    }

    private fun setupRecycler() {
        adapter = PengaduanAdapter(
            pengaduanItems,
            onDetailClick = { item ->
                val intent = Intent(this, DetailPengaduanActivity::class.java)
                intent.putExtra("id_pengaduan", item.id)
                startActivity(intent)
            },
            onCancelClick = { item ->
                showCancelDialog(item)
            }
        )

        recyclerPengaduan.layoutManager = LinearLayoutManager(this)
        recyclerPengaduan.adapter = adapter
    }

    private fun setupActions() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnTambahAduan.setOnClickListener {
            startActivity(Intent(this, TambahPengaduanActivity::class.java))
        }
    }

    private fun loadPengaduan() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val token = getToken()

                val response = RetrofitClient.apiService.getPengaduanSaya(
                    "Bearer $token",
                    "application/json"
                )

                if (response.isSuccessful) {
                    val rows = response.body()?.data ?: emptyList()

                    adapter.setData(rows)
                    updateStats(rows)

                    tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                    recyclerPengaduan.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
                } else {
                    Toast.makeText(
                        this@PengaduanActivity,
                        "Gagal memuat pengaduan: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PengaduanActivity,
                    "Gagal terhubung ke server: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun updateStats(rows: List<PengaduanItem>) {
        tvTotalAduan.text = rows.size.toString()
        tvProsesAduan.text = rows.count { it.statusPengaduan == "pending" || it.statusPengaduan == "proses" }.toString()
        tvSelesaiAduan.text = rows.count { it.statusPengaduan == "selesai" }.toString()
    }

    private fun showCancelDialog(item: PengaduanItem) {
        AlertDialog.Builder(this)
            .setTitle("Batalkan Aduan")
            .setMessage("Yakin ingin membatalkan aduan \"${item.judulPengaduan}\"?")
            .setNegativeButton("Tidak", null)
            .setPositiveButton("Ya, Batalkan") { _, _ ->
                cancelPengaduan(item)
            }
            .show()
    }

    private fun cancelPengaduan(item: PengaduanItem) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val token = getToken()

                val response = RetrofitClient.apiService.cancelPengaduan(
                    "Bearer $token",
                    "application/json",
                    item.id
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@PengaduanActivity,
                        response.body()?.message ?: "Pengaduan berhasil dibatalkan.",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadPengaduan()
                } else {
                    Toast.makeText(
                        this@PengaduanActivity,
                        "Gagal membatalkan pengaduan: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PengaduanActivity,
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
        btnTambahAduan.isEnabled = !isLoading
        btnBack.isEnabled = !isLoading
    }

    private fun getToken(): String {
        val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
        return prefs.getString("token", "") ?: ""
    }
}