package ananda.yoga.infinityps

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
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

    // =========================
    // View Components
    // =========================
    private lateinit var btnBack: ImageView
    private lateinit var etJudulPengaduan: EditText
    private lateinit var spinnerKategoriAduan: Spinner
    private lateinit var etIsiPengaduan: EditText
    private lateinit var btnPilihFoto: Button
    private lateinit var tvFotoDipilih: TextView
    private lateinit var btnKirimAduan: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var ivPreviewFoto: ImageView
    private lateinit var tvTapHint: TextView

    // =========================
    // Data
    // =========================
    private var selectedImageUri: Uri? = null

    private val kategoriLabels = listOf(
        "PS Rusak", "Pelayanan", "Kebersihan",
        "Pembayaran", "Fasilitas", "Lainnya"
    )

    private val kategoriValues = listOf(
        "ps_rusak", "pelayanan", "kebersihan",
        "pembayaran", "fasilitas", "lainnya"
    )

    // =========================
    // ✅ DIGANTI: Pakai PickVisualMedia
    // Lebih reliable dibanding GetContent untuk buka foto+video sekaligus
    // Support Android 13+ dan backport ke Android 4.4+ via AndroidX
    // =========================
    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            handleMediaSelected(uri)
        }

    // =========================
    // Lifecycle
    // =========================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_pengaduan)

        bindViews()
        setupSpinner()
        setupActions()
    }

    // =========================
    // Setup UI
    // =========================
    private fun bindViews() {
        btnBack              = findViewById(R.id.btnBack)
        etJudulPengaduan     = findViewById(R.id.etJudulPengaduan)
        spinnerKategoriAduan = findViewById(R.id.spinnerKategoriAduan)
        etIsiPengaduan       = findViewById(R.id.etIsiPengaduan)
        btnPilihFoto         = findViewById(R.id.btnPilihFoto)
        tvFotoDipilih        = findViewById(R.id.tvFotoDipilih)
        btnKirimAduan        = findViewById(R.id.btnKirimAduan)
        progressBar          = findViewById(R.id.progressBarTambahPengaduan)
        ivPreviewFoto        = findViewById(R.id.ivPreviewFoto)
        tvTapHint            = findViewById(R.id.tvTapHint)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            kategoriLabels
        )
        spinnerKategoriAduan.adapter = adapter
    }

    private fun setupActions() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ✅ DIGANTI: Langsung buka galeri foto+video tanpa dialog pilihan dulu
        btnPilihFoto.setOnClickListener {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                )
            )
        }

        ivPreviewFoto.setOnClickListener {
            selectedImageUri?.let { uri ->
                showFullscreenMedia(uri)
            }
        }

        btnKirimAduan.setOnClickListener {
            submitPengaduan()
        }
    }

    // =========================
    // Handler Media Selected
    // =========================
    private fun handleMediaSelected(uri: Uri?) {
        selectedImageUri = uri

        if (uri == null) {
            tvFotoDipilih.text = "Belum ada file dipilih"
            ivPreviewFoto.visibility = View.GONE
            tvTapHint.visibility = View.GONE
            return
        }

        val mimeType = contentResolver.getType(uri) ?: ""
        tvFotoDipilih.text = getFileName(uri) ?: "File dipilih"

        ivPreviewFoto.visibility = View.VISIBLE
        tvTapHint.visibility = View.VISIBLE

        if (mimeType.startsWith("video/")) {
            showVideoPreview(uri)
        } else {
            showImagePreview(uri)
        }
    }

    // =========================
    // Preview Media
    // =========================
    private fun showImagePreview(uri: Uri) {
        ivPreviewFoto.setImageURI(uri)
        tvTapHint.text = "Tap gambar untuk melihat penuh"
    }

    private fun showVideoPreview(uri: Uri) {
        tvTapHint.text = "Tap untuk putar video"

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // API 29+ pakai Uri langsung
                val thumbnail = contentResolver.loadThumbnail(
                    uri, android.util.Size(640, 360), null
                )
                ivPreviewFoto.setImageBitmap(thumbnail)
            } else {
                // API < 29 pakai path
                val path = getRealPathFromUri(uri)
                val thumbnail = path?.let {
                    android.media.ThumbnailUtils.createVideoThumbnail(
                        it,
                        android.provider.MediaStore.Images.Thumbnails.MINI_KIND
                    )
                }
                if (thumbnail != null) {
                    ivPreviewFoto.setImageBitmap(thumbnail)
                } else {
                    ivPreviewFoto.setImageResource(android.R.drawable.ic_media_play)
                }
            }
        } catch (e: Exception) {
            ivPreviewFoto.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun showFullscreenMedia(uri: Uri) {
        val mimeType = contentResolver.getType(uri) ?: ""

        if (mimeType.startsWith("video/")) {
            // Buka video player eksternal
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } else {
            showFullscreenImage(uri)
        }
    }

    private fun showFullscreenImage(uri: Uri) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        val imageView = ImageView(this).apply {
            setImageURI(uri)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            setOnClickListener { dialog.dismiss() }
        }

        dialog.setContentView(imageView)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.show()
    }

    // =========================
    // Submit Pengaduan
    // =========================
    private fun submitPengaduan() {
        val judul    = etJudulPengaduan.text.toString().trim()
        val isi      = etIsiPengaduan.text.toString().trim()
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
                val token        = getToken()
                val judulBody    = judul.toRequestBody("text/plain".toMediaTypeOrNull())
                val kategoriBody = kategori.toRequestBody("text/plain".toMediaTypeOrNull())
                val isiBody      = isi.toRequestBody("text/plain".toMediaTypeOrNull())
                val fotoPart     = selectedImageUri?.let { createMediaPart(it) }

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
                        response.body()?.message ?: "Aduan berhasil dikirim",
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

    // =========================
    // File Handling
    // =========================
    private fun createMediaPart(uri: Uri): MultipartBody.Part? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val fileName    = getFileName(uri) ?: "bukti_file"
        val mimeType    = contentResolver.getType(uri) ?: "*/*"
        val file        = File(cacheDir, fileName)

        FileOutputStream(file).use { output ->
            inputStream.copyTo(output)
        }

        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("foto_bukti", file.name, requestFile)
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null

        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = it.getString(index)
                }
            }
        }

        return result ?: uri.path?.substringAfterLast('/')
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        val projection = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndexOrThrow(
                    android.provider.MediaStore.MediaColumns.DATA
                )
                return it.getString(index)
            }
        }

        return null
    }

    // =========================
    // Utility
    // =========================
    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility         = if (isLoading) View.VISIBLE else View.GONE
        btnKirimAduan.isEnabled        = !isLoading
        btnPilihFoto.isEnabled         = !isLoading
        btnBack.isEnabled              = !isLoading
        spinnerKategoriAduan.isEnabled = !isLoading
        btnKirimAduan.alpha            = if (isLoading) 0.7f else 1f
    }

    private fun getToken(): String {
        val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
        return prefs.getString("token", "") ?: ""
    }
}