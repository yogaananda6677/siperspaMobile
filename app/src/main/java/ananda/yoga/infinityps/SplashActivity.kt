package ananda.yoga.infinityps

import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences

    private lateinit var loadingContainer: LinearLayout
    private lateinit var errorContainer: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        preferences = getSharedPreferences("app_session", MODE_PRIVATE)

        loadingContainer = findViewById(R.id.loadingContainer)
        errorContainer = findViewById(R.id.errorContainer)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnRetry = findViewById(R.id.btnRetry)

        btnRetry.setOnClickListener {
            showLoadingState()
            startFlow()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startFlow()
        }, 1200)
    }

    // =========================
    // MAIN FLOW
    // =========================
    private fun startFlow() {
        if (!isInternetAvailable()) {
            showErrorState("Tidak ada koneksi internet.\nCek WiFi / data seluler.")
            return
        }

        val isOnboardingDone = preferences.getBoolean("is_onboarding_done", false)
        val isLoggedIn = preferences.getBoolean("is_logged_in", false)
        val token = preferences.getString("token", null)

        // 1. Onboarding dulu
        if (!isOnboardingDone) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        // 2. Kalau sudah login → validasi ke server
        if (isLoggedIn && !token.isNullOrEmpty()) {
            checkUserFromApi(token)
        } else {
            // 3. Belum login → ke welcome
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    // =========================
    // API CHECK
    // =========================
    private fun checkUserFromApi(token: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMe("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                } else {
                    clearSession()
                    startActivity(Intent(this@SplashActivity, WelcomeActivity::class.java))
                }

                finish()

            } catch (e: Exception) {
                showErrorState(
                    "Gagal terhubung ke server.\n" +
                            "${e.javaClass.simpleName}: ${e.message}\n\n" +
                            "Pastikan:\n" +
                            "- Server aktif\n" +
                            "- Satu jaringan WiFi\n" +
                            "- IP benar (192.168.1.32)"
                )
            }
        }
    }
    // =========================
    // INTERNET CHECK
    // =========================
    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // =========================
    // UI STATE
    // =========================
    private fun showLoadingState() {
        loadingContainer.visibility = View.VISIBLE
        errorContainer.visibility = View.GONE
    }

    private fun showErrorState(message: String) {
        loadingContainer.visibility = View.GONE
        errorContainer.visibility = View.VISIBLE
        tvErrorMessage.text = message
    }

    private fun clearSession() {
        preferences.edit().clear().apply()
    }
}