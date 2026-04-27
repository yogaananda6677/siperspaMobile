package ananda.yoga.infinityps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PengaduanAdapter(
    private val items: MutableList<PengaduanItem>,
    private val onDetailClick: (PengaduanItem) -> Unit,
    private val onCancelClick: (PengaduanItem) -> Unit
) : RecyclerView.Adapter<PengaduanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvJudul: TextView = view.findViewById(R.id.tvJudulPengaduan)
        val tvKategori: TextView = view.findViewById(R.id.tvKategoriPengaduan)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggalPengaduan)
        val tvIsi: TextView = view.findViewById(R.id.tvIsiPengaduan)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusPengaduan)
        val btnDetail: TextView = view.findViewById(R.id.btnDetailPengaduan)
        val btnBatal: TextView = view.findViewById(R.id.btnBatalPengaduan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pengaduan, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvJudul.text = item.judulPengaduan ?: "-"
        holder.tvKategori.text = kategoriLabel(item.kategoriAduan)
        holder.tvTanggal.text = item.createdAt ?: "-"
        holder.tvIsi.text = item.isiPengaduan ?: "-"
        holder.tvStatus.text = statusLabel(item.statusPengaduan)

        val canCancel = item.statusPengaduan in listOf("pending", "proses")
        holder.btnBatal.visibility = if (canCancel) View.VISIBLE else View.GONE

        holder.btnDetail.setOnClickListener {
            onDetailClick(item)
        }

        holder.btnBatal.setOnClickListener {
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
}