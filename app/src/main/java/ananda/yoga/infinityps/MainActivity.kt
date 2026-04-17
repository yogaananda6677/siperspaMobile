package ananda.yoga.infinityps

import ananda.yoga.infinityps.databinding.ActivityMainBinding
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(b.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, DashboardFragment())
                .commit()
        }

        b.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.itemHome -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frame_layout, DashboardFragment())
                        .commit()
                    true
                }

                R.id.monitoring -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frame_layout, MonitoringFragment())
                        .commit()
                    true
                }

                R.id.riwayat -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frame_layout, RiwayatFragment())
                        .commit()
                    true
                }

                R.id.profil -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frame_layout, ProfilFragment())
                        .commit()
                    true
                }

                else -> false
            }
        }
    }
}