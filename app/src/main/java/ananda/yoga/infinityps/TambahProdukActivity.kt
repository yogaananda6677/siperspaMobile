package ananda.yoga.infinityps

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class TambahProdukActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvIdTransaksi: TextView
    private lateinit var tvNomorPs: TextView
    private lateinit var tvNamaTipe: TextView

    private lateinit var rvProduk: RecyclerView
    private lateinit var btnSimpan: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout

    private var idTransaksi: Int = 0
    private var nomorPs: String = "-"
    private var namaTipe: String = "-"

    private val produkItems = mutableListOf<CartProdukItem>()
    private lateinit var produkAdapter: ProdukCustomerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_produk)

        bindViews()
        readIntent()
        setupHeader()
        setupList()
        setupActions()
        loadProduk()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        tvIdTransaksi = findViewById(R.id.tvIdTransaksi)
        tvNomorPs = findViewById(R.id.tvNomorPs)
        tvNamaTipe = findViewById(R.id.tvNamaTipe)

        rvProduk = findViewById(R.id.rvProdukTambah)
        btnSimpan = findViewById(R.id.btnSimpanTambahProduk)
        progressBar = findViewById(R.id.progressBarTambahProduk)
        layoutEmpty = findViewById(R.id.layoutEmptyProduk)
    }

    private fun readIntent() {
        idTransaksi = intent.getIntExtra("id_transaksi", 0)
        nomorPs = intent.getStringExtra("nomor_ps") ?: "-"
        namaTipe = intent.getStringExtra("nama_tipe") ?: "-"
    }

    private fun setupHeader() {
        tvIdTransaksi.text = "#$idTransaksi"
        tvNomorPs.text = "PS $nomorPs"
        tvNamaTipe.text = namaTipe
    }

    private fun setupList() {
        produkAdapter = ProdukCustomerAdapter(produkItems) {}

        rvProduk.layoutManager = LinearLayoutManager(this)
        rvProduk.adapter = produkAdapter
    }

    private fun setupActions() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnSimpan.setOnClickListener {
            submitTambahProduk()
        }
    }

    private fun loadProduk() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.getProduk(
                    "Bearer $token",
                    "application/json"
                )

                if (response.isSuccessful) {
                    val data = response.body()?.data ?: emptyList()
                    produkItems.clear()
                    produkItems.addAll(data.map { CartProdukItem(it, 0) })
                    produkAdapter.notifyDataSetChanged()

                    layoutEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                    rvProduk.visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
                } else {
                    Toast.makeText(
                        this@TambahProdukActivity,
                        "Gagal memuat produk",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TambahProdukActivity,
                    "Gagal terhubung ke server: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun submitTambahProduk() {
        val selected = produkItems
            .filter { it.qty > 0 }
            .map {
                TambahProdukItem(
                    idProduk = it.produk.idProduk,
                    qty = it.qty
                )
            }

        if (selected.isEmpty()) {
            Toast.makeText(this, "Pilih minimal 1 produk", Toast.LENGTH_SHORT).show()
            return
        }

        val request = TambahProdukRequest(produk = selected)

        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.tambahProduk(
                    "Bearer $token",
                    "application/json",
                    idTransaksi,
                    request
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@TambahProdukActivity,
                        "Produk berhasil ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@TambahProdukActivity,
                        "Gagal tambah produk: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TambahProdukActivity,
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
        btnSimpan.isEnabled = !isLoading
        btnSimpan.alpha = if (isLoading) 0.7f else 1f
        btnBack.isEnabled = !isLoading

        if (isLoading) {
            rvProduk.visibility = View.GONE
            layoutEmpty.visibility = View.GONE
        }
    }
}