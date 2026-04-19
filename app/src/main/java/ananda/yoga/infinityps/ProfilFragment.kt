package ananda.yoga.infinityps

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class ProfilFragment : Fragment() {

    private lateinit var preferences: SharedPreferences

    private lateinit var tvInitial: TextView
    private lateinit var tvName: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvEmailValue: TextView
    private lateinit var tvRoleValue: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferences = requireActivity().getSharedPreferences(
            "app_session", android.content.Context.MODE_PRIVATE
        )

        tvInitial = view.findViewById(R.id.tvInitial)
        tvName = view.findViewById(R.id.tvName)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvEmailValue = view.findViewById(R.id.tvEmailValue)
        tvRoleValue = view.findViewById(R.id.tvRoleValue)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnLogout = view.findViewById(R.id.btnLogout)

        loadUserData()

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfilActivity::class.java))
        }

        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val name = preferences.getString("user_name", "-") ?: "-"
        val username = preferences.getString("user_username", "-") ?: "-"
        val email = preferences.getString("user_email", "-") ?: "-"
        val role = preferences.getString("user_role", "-") ?: "-"

        val initial = name
            .trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }

        tvInitial.text = initial.ifEmpty { "?" }
        tvName.text = name
        tvUsername.text = "@$username"
        tvEmailValue.text = email
        tvRoleValue.text = role.replaceFirstChar { it.uppercaseChar() }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Keluar")
            .setMessage("Apakah kamu yakin ingin keluar dari akun ini?")
            .setPositiveButton("Keluar") { _, _ -> doLogout() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun doLogout() {
        preferences.edit().clear().apply()

        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}