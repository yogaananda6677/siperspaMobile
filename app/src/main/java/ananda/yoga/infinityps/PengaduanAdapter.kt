package ananda.yoga.infinityps

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class PengaduanAdapter(
    private val items: MutableList<PengaduanItem>,
    private val onDetailClick: (PengaduanItem) -> Unit,
    private val onCancelClick: (PengaduanItem) -> Unit
) : RecyclerView.Adapter<PengaduanAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvJudulPengaduan: TextView = itemView.findViewById(R.id.tvJudulPengaduan)
        val tvKategoriPengaduan: TextView = itemView.findViewById(R.id.tvKategoriPengaduan)
        val tvStatusPengaduan: TextView = itemView.findViewById(R.id.tvStatusPengaduan)
        val tvIsiPengaduan: TextView = itemView.findViewById(R.id.tvIsiPengaduan)
        val tvTanggalPengaduan: TextView = itemView.findViewById(R.id.tvTanggalPengaduan)
        val btnDetailPengaduan: TextView = itemView.findViewById(R.id.btnDetailPengaduan)
        val btnBatalPengaduan: TextView = itemView.findViewById(R.id.btnBatalPengaduan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pengaduan, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context

        holder.tvJudulPengaduan.text = item.judulPengaduan ?: "-"
        holder.tvKategoriPengaduan.text = kategoriLabel(item.kategoriAduan)
        holder.tvStatusPengaduan.text = statusLabel(item.statusPengaduan)
        holder.tvIsiPengaduan.text = item.isiPengaduan ?: "-"
        holder.tvTanggalPengaduan.text = item.createdAt ?: "-"

        applyStatusStyle(ctx, holder.tvStatusPengaduan, item.statusPengaduan)

        holder.btnDetailPengaduan.setOnClickListener {
            onDetailClick(item)
        }

        val canCancel = item.statusPengaduan.equals("pending", true)
        holder.btnBatalPengaduan.visibility = if (canCancel) View.VISIBLE else View.GONE

        holder.btnBatalPengaduan.setOnClickListener {
            onCancelClick(item)
        }
    }

    fun setData(newItems: List<PengaduanItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun kategoriLabel(value: String?): String {
        return when (value) {
            "ps_rusak" -> "PS Rusak"
            "pelayanan" -> "Pelayanan"
            "kebersihan" -> "Kebersihan"
            "pembayaran" -> "Pembayaran"
            "fasilitas" -> "Fasilitas"
            "lainnya" -> "Lainnya"
            else -> "-"
        }
    }

    private fun statusLabel(value: String?): String {
        return when (value) {
            "pending" -> "Pending"
            "proses" -> "Diproses"
            "selesai" -> "Selesai"
            "dibatalkan" -> "Dibatalkan"
            else -> "-"
        }
    }

    private fun applyStatusStyle(
        context: Context,
        textView: TextView,
        status: String?
    ) {
        when (status?.lowercase()) {
            "pending" -> {
                textView.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_badge_status_soft)
                textView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
            }

            "proses" -> {
                textView.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_status_owned_active)
                textView.setTextColor(ContextCompat.getColor(context, R.color.status_owned_active_text))
            }

            "selesai" -> {
                textView.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_status_tersedia)
                textView.setTextColor(ContextCompat.getColor(context, R.color.status_tersedia_text))
            }

            "dibatalkan" -> {
                textView.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_status_danger)
                textView.setTextColor(ContextCompat.getColor(context, R.color.status_danger_text))
            }

            else -> {
                textView.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_dashboard_chip_card)
                textView.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }
}