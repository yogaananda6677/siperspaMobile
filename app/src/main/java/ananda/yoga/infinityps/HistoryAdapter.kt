package ananda.yoga.infinityps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    private val onClick: (HistoryItem) -> Unit
) : ListAdapter<HistoryItem, HistoryAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HistoryItem>() {
            override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
                return oldItem.idTransaksi == newItem.idTransaksi
            }

            override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardRoot: CardView = view.findViewById(R.id.cardRoot)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvStatusTransaksi: TextView = view.findViewById(R.id.tvStatusTransaksi)
        val tvStatusBayar: TextView = view.findViewById(R.id.tvStatusBayar)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_transaksi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        val psName = item.detailSewa.firstOrNull()?.playstation?.nomorPs
        holder.tvTitle.text = if (psName != null) {
            "Transaksi #${item.idTransaksi} • PS $psName"
        } else {
            "Transaksi #${item.idTransaksi}"
        }

        val produkCount = item.detailProduk.sumOf { it.qty }
        holder.tvSubtitle.text = buildString {
            append("${item.detailSewa.size} sewa")
            if (produkCount > 0) append(" • $produkCount produk")
        }

        holder.tvDate.text = formatDate(item.tanggal)
        holder.tvStatusTransaksi.text = formatStatusTransaksi(item.statusTransaksi)
        holder.tvStatusBayar.text = formatStatusBayar(item.pembayaran?.statusBayar)
        holder.tvTotal.text = formatRupiah(item.totalHarga)

        holder.cardRoot.setOnClickListener {
            onClick(item)
        }
    }

    private fun formatStatusTransaksi(value: String): String {
        return when (value.lowercase()) {
            "aktif" -> "Berjalan"
            "menunggu_pembayaran" -> "Menunggu Pembayaran"
            "waiting" -> "Menunggu Approval"
            "dijadwalkan" -> "Dijadwalkan"
            "selesai" -> "Selesai"
            "dibatalkan" -> "Dibatalkan"
            "ditolak" -> "Ditolak"
            else -> value.replaceFirstChar { it.uppercase() }
        }
    }

    private fun formatStatusBayar(value: String?): String {
        return when ((value ?: "menunggu").lowercase()) {
            "menunggu_validasi" -> "Menunggu Validasi"
            "menunggu" -> "Menunggu"
            "lunas" -> "Lunas"
            "gagal" -> "Gagal"
            else -> value?.replaceFirstChar { it.uppercase() } ?: "Menunggu"
        }
    }

    private fun formatRupiah(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(value)
    }

    private fun formatDate(value: String): String {
        return try {
            val inputFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            )
            val output = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))

            for (format in inputFormats) {
                try {
                    val parsed = format.parse(value)
                    if (parsed != null) return output.format(parsed)
                } catch (_: Exception) {
                }
            }

            value
        } catch (_: Exception) {
            value
        }
    }
}