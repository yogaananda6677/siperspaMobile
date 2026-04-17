package ananda.yoga.infinityps

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import org.json.JSONObject
class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPasswordConfirm: EditText
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvLogin: TextView
    private lateinit var tvBack: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etName            = findViewById(R.id.etName)
        etUsername        = findViewById(R.id.etUsername)
        etEmail           = findViewById(R.id.etEmail)
        etPassword        = findViewById(R.id.etPassword)
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm)
        btnRegister       = findViewById(R.id.btnRegister)
        progressBar       = findViewById(R.id.progressBar)
        tvLogin           = findViewById(R.id.tvLogin)
        tvBack            = findViewById(R.id.tvBack)

        btnRegister.setOnClickListener { attemptRegister() }
        tvBack.setOnClickListener { finish() }
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun attemptRegister() {
        val name     = etName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirm  = etPasswordConfirm.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Nama wajib diisi"; etName.requestFocus(); return
        }
        if (username.isEmpty()) {
            etUsername.error = "Username wajib diisi"; etUsername.requestFocus(); return
        }
        if (email.isEmpty()) {
            etEmail.error = "Email wajib diisi"; etEmail.requestFocus(); return
        }
        if (password.isEmpty()) {
            etPassword.error = "Password wajib diisi"; etPassword.requestFocus(); return
        }
        if (password.length < 8) {
            etPassword.error = "Password minimal 8 karakter"; etPassword.requestFocus(); return
        }
        if (confirm != password) {
            etPasswordConfirm.error = "Password tidak cocok"; etPasswordConfirm.requestFocus(); return
        }

        doRegister(name, username, email, password, confirm)
    }

    private fun doRegister(
        name: String, username: String, email: String,
        password: String, confirm: String
    ) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.register(
                    RegisterRequest(
                        name                  = name,
                        username              = username,
                        email                 = email,
                        password              = password,
                        password_confirmation = confirm
                        // role = "pelanggan" sudah default, tidak perlu ditulis
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Pendaftaran berhasil! Silakan masuk.",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Pendaftaran gagal. Cek kembali data kamu.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Gagal terhubung ke server",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnRegister.isEnabled  = !isLoading
        btnRegister.text       = if (isLoading) "" else "Daftar"
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}