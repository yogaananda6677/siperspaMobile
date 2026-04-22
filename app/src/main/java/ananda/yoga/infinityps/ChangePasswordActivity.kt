package ananda.yoga.infinityps

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSimpanPassword: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        bindViews()
        setupActions()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSimpanPassword = findViewById(R.id.btnSimpanPassword)
        progressBar = findViewById(R.id.progressBarChangePassword)
    }

    private fun setupActions() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnSimpanPassword.setOnClickListener {
            submitChangePassword()
        }
    }

    private fun submitChangePassword() {
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 8) {
            Toast.makeText(this, "Password baru minimal 8 karakter", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
            return
        }

        val request = ChangePasswordRequest(
            current_password = currentPassword,
            password = newPassword,
            password_confirmation = confirmPassword
        )

        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.updatePassword(
                    authorization = "Bearer $token",
                    request = request
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        body?.message ?: "Password berhasil diubah",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "Gagal ubah password: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ChangePasswordActivity,
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
        btnSimpanPassword.isEnabled = !isLoading
        btnSimpanPassword.alpha = if (isLoading) 0.7f else 1f
        btnBack.isEnabled = !isLoading
        etCurrentPassword.isEnabled = !isLoading
        etNewPassword.isEnabled = !isLoading
        etConfirmPassword.isEnabled = !isLoading
    }
}