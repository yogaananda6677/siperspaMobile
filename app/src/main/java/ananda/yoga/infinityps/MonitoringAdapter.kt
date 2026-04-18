package ananda.yoga.infinityps

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MonitoringAdapter(
    private val currentUserId: Int,
    private val onAvailableClick: (PsMonitoringItem) -> Unit,
    private val onOwnedActiveClick: (PsMonitoringItem) -> Unit
) : ListAdapter<PsMonitoringItem, MonitoringAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PsMonitoringItem>() {
            override fun areItemsTheSame(a: PsMonitoringItem, b: PsMonitoringItem): Boolean {
                return a.idPs == b.idPs
            }

            override fun areContentsTheSame(a: PsMonitoringItem, b: PsMonitoringItem): Boolean {
                return a == b
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardRoot: CardView = itemView.findViewById(R.id.cardRoot)
        val tvNomorPs: TextView = itemView.findViewById(R.id.tvNomorPs)
        val tvTipePs: TextView = itemView.findViewById(R.id.tvTipePs)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val layoutAktif: LinearLayout = itemView.findViewById(R.id.layoutAktif)
        val tvPelanggan: TextView = itemView.findViewById(R.id.tvPelanggan)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvJamMulai: TextView = itemView.findViewById(R.id.tvJamMulai)
        val tvDurasi: TextView = itemView.findViewById(R.id.tvDurasi)
        val layoutProduk: LinearLayout = itemView.findViewById(R.id.layoutProduk)
        val tvProdukList: TextView = itemView.findViewById(R.id.tvProdukList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monitoring_ps, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val ctx = holder.itemView.context

        holder.tvNomorPs.text = "PS ${item.nomorPs}"
        holder.tvTipePs.text = item.tipe?.namaTipe ?: "-"

        holder.layoutAktif.visibility = View.GONE
        holder.layoutProduk.visibility = View.GONE
        holder.tvPelanggan.visibility = View.GONE
        holder.tvUsername.visibility = View.GONE
        holder.tvProdukList.text = ""
        holder.tvJamMulai.text = ""
        holder.tvDurasi.text = ""

        holder.cardRoot.setOnClickListener(null)
        holder.cardRoot.isClickable = false
        holder.cardRoot.isFocusable = false
        holder.cardRoot.alpha = 1.0f

        val transaksi = item.activeTransaksi
        val transaksiStatus = transaksi?.statusTransaksi?.lowercase()
        val sewa = transaksi?.detailSewa?.firstOrNull { it.idPs == item.idPs }
        val statusBayar = transaksi?.pembayaran?.statusBayar?.lowercase()
        val transaksiUserId = transaksi?.user?.idUser

        val isOwnedActive =
            (transaksiStatus == "aktif" || transaksiStatus == "menunggu_pembayaran") &&
                    transaksiUserId == currentUserId

        Log.d(
            "MONITORING_ADAPTER",
            "ps=${item.nomorPs}, statusPs=${item.statusPs}, transaksiStatus=$transaksiStatus, " +
                    "statusBayar=$statusBayar, transaksiUserId=$transaksiUserId, currentUserId=$currentUserId, " +
                    "isOwnedActive=$isOwnedActive"
        )

        when {
            item.statusPs.equals("maintenance", true) -> {
                holder.tvStatus.text = "Maintenance"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_maintenance)
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_maintenance_text)
                )
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_default_bg)
                )
                holder.layoutAktif.visibility = View.VISIBLE
                holder.tvJamMulai.text = "Tidak tersedia"
                holder.tvDurasi.text = ""
                holder.cardRoot.alpha = 0.85f
            }

            isOwnedActive && statusBayar == "menunggu_validasi" -> {
                holder.tvStatus.text = "Menunggu Validasi"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_booking)
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_booking_text)
                )
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_owned_active_bg)
                )
                holder.layoutAktif.visibility = View.VISIBLE

                if (sewa != null) {
                    val remainingSeconds = calculateRemainingSeconds(sewa.jamSelesai)
                    holder.tvJamMulai.text = formatCountdown(remainingSeconds)
                    holder.tvDurasi.text = "Pembayaran cash menunggu admin"
                } else {
                    holder.tvJamMulai.text = "Transaksi kamu aktif"
                    holder.tvDurasi.text = "Pembayaran cash menunggu admin"
                }

                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener {
                    onOwnedActiveClick(item)
                }
            }

            isOwnedActive -> {
                holder.tvStatus.text = "Sedang Kamu Pakai"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_owned_active)
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_owned_active_text)
                )
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_owned_active_bg)
                )
                holder.layoutAktif.visibility = View.VISIBLE

                if (sewa != null) {
                    val remainingSeconds = calculateRemainingSeconds(sewa.jamSelesai)
                    holder.tvJamMulai.text = formatCountdown(remainingSeconds)
                    holder.tvDurasi.text = when (statusBayar) {
                        "lunas" -> "Lunas • Tap lihat detail"
                        "menunggu" -> "Belum dibayar • Tap kelola"
                        else -> "Belum lunas • Tap kelola"
                    }
                } else {
                    holder.tvJamMulai.text = "Transaksi kamu aktif"
                    holder.tvDurasi.text = when (statusBayar) {
                        "lunas" -> "Lunas • Tap lihat detail"
                        "menunggu" -> "Belum dibayar • Tap kelola"
                        else -> "Tap untuk kelola"
                    }
                }

                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener {
                    onOwnedActiveClick(item)
                }
            }

            item.statusPs.equals("digunakan", true) ||
                    transaksiStatus == "aktif" ||
                    transaksiStatus == "menunggu_pembayaran" -> {

                holder.tvStatus.text = "Digunakan"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_aktif)
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_aktif_text)
                )
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_aktif_bg)
                )
                holder.layoutAktif.visibility = View.VISIBLE

                if (sewa != null) {
                    val remainingSeconds = calculateRemainingSeconds(sewa.jamSelesai)
                    holder.tvJamMulai.text = formatCountdown(remainingSeconds)
                    holder.tvDurasi.text = "Berakhir ${formatJam(sewa.jamSelesai)}"
                } else {
                    holder.tvJamMulai.text = "Sedang digunakan"
                    holder.tvDurasi.text = ""
                }
            }

            transaksiStatus == "dijadwalkan" -> {
                holder.tvStatus.text = "Dijadwalkan"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_booking)
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_booking_text)
                )
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_default_bg)
                )
                holder.layoutAktif.visibility = View.VISIBLE
                holder.cardRoot.alpha = 0.9f

                holder.tvJamMulai.text = "Mulai ${formatJam(sewa?.jamMulai)}"
                holder.tvDurasi.text = "Sudah dibooking"
            }

            transaksiStatus == "waiting" -> {
                holder.tvStatus.text = "Menunggu"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_booking)
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_booking_text)
                )
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_default_bg)
                )
                holder.layoutAktif.visibility = View.VISIBLE
                holder.cardRoot.alpha = 0.9f

                holder.tvJamMulai.text = "Booking pending"
                holder.tvDurasi.text = "Menunggu approval"
            }

            else -> {
                holder.tvStatus.text = "Tersedia"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_tersedia)
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_tersedia_text)
                )
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_default_bg)
                )
                holder.layoutAktif.visibility = View.VISIBLE
                holder.tvJamMulai.text = "Siap dipakai"
                holder.tvDurasi.text = "Tap untuk booking"

                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener {
                    onAvailableClick(item)
                }
            }
        }
    }

    private fun calculateRemainingSeconds(jamSelesai: String?): Long {
        if (jamSelesai.isNullOrBlank()) return 0L

        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            formatter.timeZone = TimeZone.getDefault()
            val endDate: Date = formatter.parse(jamSelesai) ?: return 0L
            val diffMillis = endDate.time - System.currentTimeMillis()
            maxOf(0L, diffMillis / 1000)
        } catch (_: Exception) {
            0L
        }
    }

    private fun formatCountdown(totalSeconds: Long): String {
        if (totalSeconds <= 0) return "Waktu habis"

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "Sisa ${hours}j ${minutes}m"
            minutes > 0 -> "Sisa ${minutes}m ${seconds}d"
            else -> "Sisa ${seconds}d"
        }
    }

    private fun formatJam(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        val cleaned = raw.trim()
        return if (cleaned.contains(" ")) {
            cleaned.substringAfterLast(" ").take(5)
        } else {
            cleaned.take(5)
        }
    }
}