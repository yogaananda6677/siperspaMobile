package ananda.yoga.infinityps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class RiwayatFragment : Fragment() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: HistoryAdapter

    companion object {
        private const val TAG = "RIWAYAT"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_riwayat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvHistory = view.findViewById(R.id.rvHistory)
        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)

        adapter = HistoryAdapter { item ->
            val intent = Intent(requireContext(), DetailTransaksiActivity::class.java)
            intent.putExtra("id_transaksi", item.idTransaksi)
            startActivity(intent)
        }

        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = adapter

        fetchHistory()
    }

    override fun onResume() {
        super.onResume()
        fetchHistory()
    }

    private fun fetchHistory() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val prefs = requireActivity()
                    .getSharedPreferences("app_session", Context.MODE_PRIVATE)

                val token = prefs.getString("token", "") ?: ""
                val idUser = prefs.getInt("id_user", 0)
                val isLoggedIn = prefs.getBoolean("is_logged_in", false)

                Log.d(TAG, "=== FETCH HISTORY START ===")
                Log.d(TAG, "is_logged_in = $isLoggedIn")
                Log.d(TAG, "id_user = $idUser")
                Log.d(TAG, "token kosong? = ${token.isBlank()}")
                Log.d(TAG, "token = $token")

                val response = RetrofitClient.apiService.getTransaksiSaya("Bearer $token")

                Log.d(TAG, "response code = ${response.code()}")
                Log.d(TAG, "response message = ${response.message()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    val items = body?.data ?: emptyList()

                    Log.d(TAG, "body = $body")
                    Log.d(TAG, "jumlah history = ${items.size}")

                    adapter.submitList(items)
                    layoutEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

                    if (items.isEmpty()) {
                        Log.d(TAG, "history kosong")
                    } else {
                        items.forEachIndexed { index, item ->
                            Log.d(
                                TAG,
                                "item[$index] id=${item.idTransaksi}, status=${item.statusTransaksi}, total=${item.totalHarga}"
                            )
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()

                    Log.e(TAG, "gagal memuat history")
                    Log.e(TAG, "code = ${response.code()}")
                    Log.e(TAG, "message = ${response.message()}")
                    Log.e(TAG, "errorBody = $errorBody")

                    Toast.makeText(
                        requireContext(),
                        "Gagal memuat history: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "exception saat fetch history", e)
                Log.e(TAG, "exception message = ${e.message}")

                Toast.makeText(
                    requireContext(),
                    "Gagal terhubung ke server: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                Log.d(TAG, "=== FETCH HISTORY END ===")
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        rvHistory.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
}