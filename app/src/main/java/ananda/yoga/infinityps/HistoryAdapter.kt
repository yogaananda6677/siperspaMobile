package ananda.yoga.infinityps

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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

        val statusTransaksiText = formatStatusTransaksi(item.statusTransaksi)
        val statusBayarText = formatStatusBayar(item.pembayaran?.statusBayar)

        holder.tvStatusTransaksi.text = statusTransaksiText
        holder.tvStatusBayar.text = statusBayarText

        applyStatusTransaksiStyle(holder.tvStatusTransaksi, item.statusTransaksi)
        applyStatusBayarStyle(holder.tvStatusBayar, item.pembayaran?.statusBayar)

        holder.tvTotal.text = formatRupiah(item.totalHarga)

        holder.cardRoot.setOnClickListener {
            onClick(item)
        }
    }

    private fun applyStatusTransaksiStyle(textView: TextView, status: String) {
        when (status.lowercase()) {
            "aktif" -> setBadge(textView, "#EDE9FE", "#6D28D9")
            "menunggu_pembayaran" -> setBadge(textView, "#FEF3C7", "#B45309")
            "waiting" -> setBadge(textView, "#FEF3C7", "#B45309")
            "dijadwalkan" -> setBadge(textView, "#FFE4D6", "#C2410C")
            "selesai" -> setBadge(textView, "#DCFCE7", "#15803D")
            "dibatalkan", "ditolak" -> setBadge(textView, "#FEE2E2", "#B91C1C")
            else -> setBadge(textView, "#E5E7EB", "#374151")
        }
    }

    private fun applyStatusBayarStyle(textView: TextView, status: String?) {
        when ((status ?: "menunggu").lowercase()) {
            "lunas" -> setBadge(textView, "#FEF3C7", "#B45309")
            "menunggu_validasi" -> setBadge(textView, "#FEF3C7", "#B45309")
            "menunggu" -> setBadge(textView, "#FEF3C7", "#B45309")
            "gagal" -> setBadge(textView, "#FEE2E2", "#B91C1C")
            else -> setBadge(textView, "#E5E7EB", "#374151")
        }
    }

    private fun setBadge(textView: TextView, bgColor: String, textColor: String) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(Color.parseColor(bgColor))
        }
        textView.background = drawable
        textView.setTextColor(Color.parseColor(textColor))
        textView.setPadding(20, 10, 20, 10)
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