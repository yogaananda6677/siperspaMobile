package ananda.yoga.infinityps

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class TambahPengaduanActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etJudulPengaduan: EditText
    private lateinit var spinnerKategoriAduan: Spinner
    private lateinit var etIsiPengaduan: EditText
    private lateinit var btnPilihFoto: Button
    private lateinit var tvFotoDipilih: TextView
    private lateinit var btnKirimAduan: Button
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri

        if (uri != null) {
            tvFotoDipilih.text = getFileName(uri) ?: "Foto bukti dipilih"
        } else {
            tvFotoDipilih.text = "Belum ada foto dipilih"
        }
    }

    private val kategoriLabels = listOf(
        "PS Rusak",
        "Pelayanan",
        "Kebersihan",
        "Pembayaran",
        "Fasilitas",
        "Lainnya"
    )

    private val kategoriValues = listOf(
        "ps_rusak",
        "pelayanan",
        "kebersihan",
        "pembayaran",
        "fasilitas",
        "lainnya"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_pengaduan)

        bindViews()
        setupSpinner()
        setupActions()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        etJudulPengaduan = findViewById(R.id.etJudulPengaduan)
        spinnerKategoriAduan = findViewById(R.id.spinnerKategoriAduan)
        etIsiPengaduan = findViewById(R.id.etIsiPengaduan)
        btnPilihFoto = findViewById(R.id.btnPilihFoto)
        tvFotoDipilih = findViewById(R.id.tvFotoDipilih)
        btnKirimAduan = findViewById(R.id.btnKirimAduan)
        progressBar = findViewById(R.id.progressBarTambahPengaduan)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kategoriLabels)
        spinnerKategoriAduan.adapter = adapter
    }

    private fun setupActions() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnPilihFoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnKirimAduan.setOnClickListener {
            submitPengaduan()
        }
    }

    private fun submitPengaduan() {
        val judul = etJudulPengaduan.text.toString().trim()
        val isi = etIsiPengaduan.text.toString().trim()
        val kategori = kategoriValues[spinnerKategoriAduan.selectedItemPosition]

        if (judul.isEmpty()) {
            etJudulPengaduan.error = "Judul wajib diisi"
            etJudulPengaduan.requestFocus()
            return
        }

        if (isi.isEmpty()) {
            etIsiPengaduan.error = "Isi aduan wajib diisi"
            etIsiPengaduan.requestFocus()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val token = getToken()

                val judulBody = judul.toRequestBody("text/plain".toMediaTypeOrNull())
                val kategoriBody = kategori.toRequestBody("text/plain".toMediaTypeOrNull())
                val isiBody = isi.toRequestBody("text/plain".toMediaTypeOrNull())
                val fotoPart = selectedImageUri?.let { uri ->
                    createImagePart(uri)
                }

                val response = RetrofitClient.apiService.createPengaduan(
                    "Bearer $token",
                    "application/json",
                    judulBody,
                    kategoriBody,
                    isiBody,
                    fotoPart
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@TambahPengaduanActivity,
                        response.body()?.message ?: "Aduan berhasil dikirim.",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                } else {
                    Toast.makeText(
                        this@TambahPengaduanActivity,
                        "Gagal mengirim aduan: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TambahPengaduanActivity,
                    "Gagal terhubung ke server: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun createImagePart(uri: Uri): MultipartBody.Part? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val fileName = getFileName(uri) ?: "foto_bukti.jpg"
        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { output ->
            inputStream.copyTo(output)
        }

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())

        return MultipartBody.Part.createFormData(
            "foto_bukti",
            file.name,
            requestFile
        )
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null

        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }

        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }

        return result
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnKirimAduan.isEnabled = !isLoading
        btnPilihFoto.isEnabled = !isLoading
        btnBack.isEnabled = !isLoading
        spinnerKategoriAduan.isEnabled = !isLoading

        btnKirimAduan.alpha = if (isLoading) 0.7f else 1f
    }

    private fun getToken(): String {
        val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
        return prefs.getString("token", "") ?: ""
    }
}