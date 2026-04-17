package ananda.yoga.infinityps

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OnboardingActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        preferences = getSharedPreferences("app_session", MODE_PRIVATE)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val tvSkip = findViewById<TextView>(R.id.tvSkip)

        btnStart.setOnClickListener {
            finishOnboarding()
        }

        tvSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        preferences.edit()
            .putBoolean("is_onboarding_done", true)
            .apply()

        startActivity(Intent(this, WelcomeActivity::class.java))
        finish()
    }
}