package ananda.yoga.infinityps

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvRegister: TextView
    private lateinit var tvBack: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        preferences = getSharedPreferences("app_session", MODE_PRIVATE)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvRegister = findViewById(R.id.tvRegisterL)
        tvBack = findViewById(R.id.tvBack)

        btnLogin.setOnClickListener {
            attemptLogin()
        }

        tvBack.setOnClickListener {
            finish()
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty()) {
            etUsername.error = "Username wajib diisi"
            etUsername.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password wajib diisi"
            etPassword.requestFocus()
            return
        }

        doLogin(username, password)
    }

    private fun doLogin(username: String, password: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(
                    LoginRequest(
                        username = username,
                        password = password
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    Log.d("LOGIN", "TOKEN = ${loginResponse.token}")
                    Log.d("LOGIN", "ID_USER = ${loginResponse.user.idUser}")
                    Log.d("LOGIN", "ROLE = ${loginResponse.user.role}")

                    if (loginResponse.user.role != "pelanggan") {
                        Toast.makeText(
                            this@LoginActivity,
                            "Akses ditolak. Aplikasi ini hanya untuk pelanggan.",
                            Toast.LENGTH_SHORT
                        ).show()
                        setLoading(false)
                        return@launch
                    }

                    preferences.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("token", loginResponse.token)
                        .putInt("id_user", loginResponse.user.idUser)
                        .putString("user_name", loginResponse.user.name)
                        .putString("user_username", loginResponse.user.username ?: "")
                        .putString("user_email", loginResponse.user.email ?: "")
                        .putString("user_role", loginResponse.user.role ?: "")
                        .apply()

                    Log.d("LOGIN", "SESSION SAVED ID_USER = ${preferences.getInt("id_user", 0)}")

                    Toast.makeText(
                        this@LoginActivity,
                        loginResponse.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Username atau password salah",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("LOGIN", "EXCEPTION", e)
                Toast.makeText(
                    this@LoginActivity,
                    "Gagal terhubung ke server",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnLogin.isEnabled = !isLoading
        progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        btnLogin.text = if (isLoading) "" else "Masuk"
    }
}