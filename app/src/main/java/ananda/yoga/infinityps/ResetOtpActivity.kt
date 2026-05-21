package ananda.yoga.infinityps

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ananda.yoga.infinityps.databinding.ActivityResetOtpBinding
import kotlinx.coroutines.launch

class ResetOtpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetOtpBinding
    private var email: String = ""
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        email = intent.getStringExtra("email") ?: ""
        binding.tvEmail.text = email

        binding.tvBack.setOnClickListener { finish() }
        binding.btnVerifikasi.setOnClickListener { doVerify() }
        binding.tvResendOtp.setOnClickListener { doResend() }
    }

    private fun doVerify() {
        val otp = binding.etOtp.text.toString().trim()

        if (otp.isEmpty()) {
            Toast.makeText(this, "Kode OTP tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }
        if (otp.length < 6) {
            Toast.makeText(this, "Masukkan kode OTP 6 digit", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.verifyResetOtp(
                    VerifyResetOtpRequest(email = email, otp = otp)
                )

                if (response.isSuccessful) {
                    // Pindah ke ResetPasswordActivity, bawa email & otp
                    val intent = Intent(this@ResetOtpActivity, ResetPasswordActivity::class.java)
                    intent.putExtra("email", email)
                    intent.putExtra("otp", otp)
                    startActivity(intent)

                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Kode OTP salah atau sudah kedaluwarsa"
                        404 -> "Email tidak ditemukan"
                        else -> "Verifikasi gagal, coba lagi"
                    }
                    Toast.makeText(this@ResetOtpActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ResetOtpActivity,
                    "Gagal terhubung ke server",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun doResend() {
        setResendLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.forgotPassword(
                    ForgotPasswordRequest(email = email)
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ResetOtpActivity,
                        "Kode OTP baru sudah dikirim ke email",
                        Toast.LENGTH_SHORT
                    ).show()
                    startCountdown()
                } else {
                    Toast.makeText(this@ResetOtpActivity, "Gagal kirim ulang OTP", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ResetOtpActivity,
                    "Gagal terhubung ke server",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setResendLoading(false)
            }
        }
    }

    private fun startCountdown() {
        binding.tvResendOtp.isEnabled = false
        binding.tvResendOtp.setTextColor(getColor(R.color.text_secondary))
        binding.tvCountdown.visibility = View.VISIBLE

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvCountdown.text = "Kirim ulang dalam ${millisUntilFinished / 1000}s"
            }
            override fun onFinish() {
                binding.tvCountdown.visibility = View.GONE
                binding.tvResendOtp.isEnabled = true
                binding.tvResendOtp.setTextColor(getColor(R.color.primary))
            }
        }.start()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnVerifikasi.isEnabled = !isLoading
        binding.etOtp.isEnabled = !isLoading
    }

    private fun setResendLoading(isLoading: Boolean) {
        binding.tvResendOtp.isEnabled = !isLoading
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}