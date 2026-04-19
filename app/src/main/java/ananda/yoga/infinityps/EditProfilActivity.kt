package ananda.yoga.infinityps

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class EditProfilActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnSimpan: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profil)

        etName = findViewById(R.id.etName)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        btnSimpan = findViewById(R.id.btnSimpanProfile)
        progressBar = findViewById(R.id.progressBarEditProfile)

        loadCurrentData()

        btnSimpan.setOnClickListener {
            submitUpdateProfile()
        }
    }

    private fun loadCurrentData() {
        val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
        etName.setText(prefs.getString("user_name", "") ?: "")
        etUsername.setText(prefs.getString("user_username", "") ?: "")
        etEmail.setText(prefs.getString("user_email", "") ?: "")
    }

    private fun submitUpdateProfile() {
        val name = etName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()

        if (name.isBlank() || username.isBlank() || email.isBlank()) {
            Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val request = UpdateProfileRequest(
            name = name,
            username = username,
            email = email
        )

        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("app_session", Context.MODE_PRIVATE)
                val token = prefs.getString("token", "") ?: ""

                val response = RetrofitClient.apiService.updateProfile(
                    "Bearer $token",
                    "application/json",
                    request
                )

                if (response.isSuccessful) {
                    val user = response.body()?.user
                    if (user != null) {
                        prefs.edit()
                            .putString("user_name", user.name)
                            .putString("user_username", user.username ?: "")
                            .putString("user_email", user.email ?: "")
                            .putString("user_role", user.role ?: "")
                            .apply()
                    }

                    Toast.makeText(this@EditProfilActivity, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@EditProfilActivity,
                        "Gagal update profil: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditProfilActivity,
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
    }
}