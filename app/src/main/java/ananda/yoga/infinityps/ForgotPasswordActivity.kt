package ananda.yoga.infinityps

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ananda.yoga.infinityps.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvBack.setOnClickListener { finish() }
        binding.tvBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.btnKirimOtp.setOnClickListener { doSendOtp() }
    }

    private fun doSendOtp() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            binding.etEmail.error = "Email wajib diisi"
            binding.etEmail.requestFocus()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.forgotPassword(
                    ForgotPasswordRequest(email = email)
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Kode OTP sudah dikirim ke email!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Pindah ke ResetOtpActivity
                    val intent = Intent(this@ForgotPasswordActivity, ResetOtpActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)

                } else {
                    val errorMsg = when (response.code()) {
                        404 -> "Email tidak ditemukan"
                        else -> "Gagal mengirim OTP, coba lagi"
                    }
                    Toast.makeText(this@ForgotPasswordActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ForgotPasswordActivity,
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
        binding.btnKirimOtp.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
    }
}