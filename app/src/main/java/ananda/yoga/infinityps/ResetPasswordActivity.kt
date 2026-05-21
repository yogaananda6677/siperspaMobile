package ananda.yoga.infinityps

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ananda.yoga.infinityps.databinding.ActivityResetPasswordBinding
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private var email: String = ""
    private var otp: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil email & otp dari Intent
        email = intent.getStringExtra("email") ?: ""
        otp   = intent.getStringExtra("otp") ?: ""

        binding.tvBack.setOnClickListener { finish() }
        binding.btnResetPassword.setOnClickListener { doReset() }
    }

    private fun doReset() {
        val password = binding.etPassword.text.toString().trim()
        val confirm  = binding.etPasswordConfirm.text.toString().trim()

        if (password.isEmpty()) {
            binding.etPassword.error = "Password wajib diisi"
            binding.etPassword.requestFocus()
            return
        }
        if (password.length < 8) {
            binding.etPassword.error = "Password minimal 8 karakter"
            binding.etPassword.requestFocus()
            return
        }
        if (confirm != password) {
            binding.etPasswordConfirm.error = "Password tidak cocok"
            binding.etPasswordConfirm.requestFocus()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.resetPassword(
                    ResetPasswordRequest(
                        email                 = email,
                        otp                   = otp,
                        password              = password,
                        password_confirmation = confirm
                    )
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ResetPasswordActivity,
                        "Password berhasil direset! Silakan login.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Kembali ke LoginActivity, hapus semua stack
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)

                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Kode OTP salah atau sudah kedaluwarsa"
                        404 -> "Email tidak ditemukan"
                        else -> "Gagal reset password, coba lagi"
                    }
                    Toast.makeText(this@ResetPasswordActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ResetPasswordActivity,
                    "Gagal terhubung ke server",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnResetPassword.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etPasswordConfirm.isEnabled = !isLoading
    }
}