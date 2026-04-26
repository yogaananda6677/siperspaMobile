package ananda.yoga.infinityps

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
    private val onOwnedTransactionClick: (PsMonitoringItem) -> Unit
) : ListAdapter<PsMonitoringItem, MonitoringAdapter.ViewHolder>(DIFF) {

    companion object {
        private const val RESERVASI_OPEN_SECONDS = 50 * 60L

        private val DIFF = object : DiffUtil.ItemCallback<PsMonitoringItem>() {
            override fun areItemsTheSame(a: PsMonitoringItem, b: PsMonitoringItem): Boolean = a.idPs == b.idPs
            override fun areContentsTheSame(a: PsMonitoringItem, b: PsMonitoringItem): Boolean = a == b
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardRoot: CardView = itemView.findViewById(R.id.cardRoot)
        val tvNomorPs: TextView = itemView.findViewById(R.id.tvNomorPs)
        val tvTipePs: TextView = itemView.findViewById(R.id.tvTipePs)
        val tvHarga: TextView = itemView.findViewById(R.id.tvHarga)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvActionHint: TextView = itemView.findViewById(R.id.tvActionHint)

        val layoutAktif: LinearLayout = itemView.findViewById(R.id.layoutAktif)
        val tvPelanggan: TextView = itemView.findViewById(R.id.tvPelanggan)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvJamMulai: TextView = itemView.findViewById(R.id.tvJamMulai)
        val tvDurasi: TextView = itemView.findViewById(R.id.tvDurasi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monitoring_ps_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val ctx = holder.itemView.context

        holder.tvNomorPs.text = item.nomorPs
        holder.tvTipePs.text = item.tipe?.namaTipe ?: "Tanpa tipe"
        holder.tvHarga.text = formatRupiah(item.tipe?.hargaSewa ?: 0L)

        resetView(holder)

        val transaksi = item.activeTransaksi
        val transaksiStatus = transaksi?.statusTransaksi?.lowercase()
        val statusBayar = transaksi?.pembayaran?.statusBayar?.lowercase()
        val transaksiUserId = transaksi?.user?.idUser
        val sewa = transaksi?.detailSewa?.firstOrNull { it.idPs == item.idPs }

        val isCurrentUser = transaksiUserId == currentUserId
        val isOwnedWaiting = transaksiStatus == "waiting" && isCurrentUser
        val isOwnedScheduled = transaksiStatus == "dijadwalkan" && isCurrentUser
        val isOwnedActive = transaksiStatus == "aktif" && isCurrentUser

        val remainingSeconds = if (sewa != null) getRemainingSeconds(sewa) else 0L
        val isReservasiWindow =
            (item.statusPs.equals("digunakan", true) || transaksiStatus == "aktif") &&
                    remainingSeconds in 1..RESERVASI_OPEN_SECONDS

        when {
            item.statusPs.equals("maintenance", true) -> {
                holder.layoutAktif.visibility = View.VISIBLE
                holder.tvStatus.text = "Maintenance"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_maintenance)
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_maintenance_text))
                holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.card_default_bg))
                holder.cardRoot.alpha = 0.90f
                holder.tvJamMulai.text = "Sedang perbaikan"
                holder.tvDurasi.text = "Belum bisa dipakai"
            }

            isOwnedWaiting -> {
                holder.layoutAktif.visibility = View.VISIBLE
                holder.tvStatus.text = "Booking"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_booking)
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_booking_text))
                holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.card_owned_active_bg))

                showOwnUserInfo(holder, transaksi?.user?.name, transaksi?.user?.username)
                holder.tvActionHint.visibility = View.VISIBLE
                holder.tvActionHint.text = "Detail"

                holder.tvJamMulai.text = if (sewa?.jamMulai != null) {
                    "Mulai ${formatJam(sewa.jamMulai)}"
                } else {
                    "Booking pending"
                }
                holder.tvDurasi.text = "Menunggu admin"

                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener { onOwnedTransactionClick(item) }
            }

            transaksiStatus == "waiting" -> {
                holder.layoutAktif.visibility = View.VISIBLE
                holder.tvStatus.text = "Booking"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_booking)
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_booking_text))
                holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.card_default_bg))

                holder.tvJamMulai.text = if (sewa?.jamMulai != null) {
                    "Mulai ${formatJam(sewa.jamMulai)}"
                } else {
                    "Booking pending"
                }
                holder.tvDurasi.text = "Sudah dibooking"
            }

            isOwnedScheduled -> {
                holder.layoutAktif.visibility = View.VISIBLE
                holder.tvStatus.text = "Punyaku"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_owned_active)
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_owned_active_text))
                holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.card_owned_active_bg))

                showOwnUserInfo(holder, transaksi?.user?.name, transaksi?.user?.username)
                holder.tvActionHint.visibility = View.VISIBLE
                holder.tvActionHint.text = "Detail"

                holder.tvJamMulai.text = "Mulai ${formatJam(sewa?.jamMulai)}"
                holder.tvDurasi.text = "Booking kamu"

                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener { onOwnedTransactionClick(item) }
            }

            transaksiStatus == "dijadwalkan" -> {
                holder.layoutAktif.visibility = View.VISIBLE
                holder.tvStatus.text = "Booked"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_booking)
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_booking_text))
                holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.card_default_bg))

                holder.tvJamMulai.text = "Mulai ${formatJam(sewa?.jamMulai)}"
                holder.tvDurasi.text = "Sudah dibooking"
            }

            statusBayar == "menunggu_validasi" -> {
                holder.layoutAktif.visibility = View.VISIBLE
                holder.tvStatus.text = if (isCurrentUser) "Validasi Cash" else "Dipakai"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_booking)
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_booking_text))
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(
                        ctx,
                        if (isCurrentUser) R.color.card_owned_active_bg else R.color.card_aktif_bg
                    )
                )

                if (isCurrentUser) {
                    showOwnUserInfo(holder, transaksi?.user?.name, transaksi?.user?.username)
                    holder.tvActionHint.visibility = View.VISIBLE
                    holder.tvActionHint.text = "Kelola"
                    holder.cardRoot.isClickable = true
                    holder.cardRoot.isFocusable = true
                    holder.cardRoot.setOnClickListener { onOwnedTransactionClick(item) }
                }

                holder.tvJamMulai.text = if (sewa != null) formatCountdown(remainingSeconds) else "Transaksi aktif"
                holder.tvDurasi.text = if (isCurrentUser) {
                    "Cash menunggu validasi"
                } else {
                    "Sedang digunakan"
                }
            }

            isOwnedActive -> {
                holder.layoutAktif.visibility = View.VISIBLE
                holder.tvStatus.text = "Punyaku"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_owned_active)
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_owned_active_text))
                holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.card_owned_active_bg))

                showOwnUserInfo(holder, transaksi?.user?.name, transaksi?.user?.username)
                holder.tvActionHint.visibility = View.VISIBLE
                holder.tvActionHint.text = "Detail"

                if (sewa != null) {
                    holder.tvJamMulai.text = formatCountdown(remainingSeconds)
                    holder.tvDurasi.text = when (statusBayar) {
                        "lunas" -> "Lunas"
                        "menunggu" -> "Belum dibayar"
                        "gagal" -> "Pembayaran gagal"
                        else -> "Kelola transaksi"
                    }
                } else {
                    holder.tvJamMulai.text = "Transaksi aktif"
                    holder.tvDurasi.text = "Kelola transaksi"
                }

                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener { onOwnedTransactionClick(item) }
            }

            isReservasiWindow -> {
                holder.layoutAktif.visibility = View.VISIBLE
                holder.tvStatus.text = "Reservasi"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_booking)
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_booking_text))
                holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.card_default_bg))

                holder.tvActionHint.visibility = View.VISIBLE
                holder.tvActionHint.text = "Booking"
                holder.tvJamMulai.text = "Mulai otomatis 50 menit lagi"
                holder.tvDurasi.text = "Reservasi dibuka • maks. 3 jam"

                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener { onAvailableClick(item) }
            }

            item.statusPs.equals("digunakan", true) || transaksiStatus == "aktif" -> {
                holder.layoutAktif.visibility = View.VISIBLE
                holder.tvStatus.text = "Dipakai"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_aktif)
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_aktif_text))
                holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.card_aktif_bg))

                if (sewa != null) {
                    holder.tvJamMulai.text = formatCountdown(remainingSeconds)
                    holder.tvDurasi.text = "Sampai ${formatJam(sewa.jamSelesai)}"
                } else {
                    holder.tvJamMulai.text = "Sedang digunakan"
                    holder.tvDurasi.text = "Masih aktif"
                }
            }

            else -> {
                holder.layoutAktif.visibility = View.VISIBLE
                holder.tvStatus.text = "Tersedia"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_tersedia)
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_tersedia_text))
                holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.card_default_bg))

                holder.tvActionHint.visibility = View.VISIBLE
                holder.tvActionHint.text = "Booking"
                holder.tvJamMulai.text = "Siap dipakai"
                holder.tvDurasi.text = "Tap untuk booking"

                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener { onAvailableClick(item) }
            }
        }
    }

    private fun resetView(holder: ViewHolder) {
        holder.tvActionHint.visibility = View.GONE
        holder.layoutAktif.visibility = View.GONE
        holder.tvPelanggan.visibility = View.GONE
        holder.tvUsername.visibility = View.GONE
        holder.tvJamMulai.text = ""
        holder.tvDurasi.text = ""

        holder.cardRoot.setOnClickListener(null)
        holder.cardRoot.isClickable = false
        holder.cardRoot.isFocusable = false
        holder.cardRoot.alpha = 1.0f
    }

    private fun showOwnUserInfo(holder: ViewHolder, name: String?, username: String?) {
        holder.tvPelanggan.visibility = View.VISIBLE
        holder.tvUsername.visibility = View.VISIBLE
        holder.tvPelanggan.text = name ?: "Kamu"
        holder.tvUsername.text = username?.let { "@$it" } ?: ""
    }

    private fun getRemainingSeconds(sewa: DetailSewa): Long {
        return if (sewa.sisaDetik > 0) sewa.sisaDetik else calculateRemainingSeconds(sewa.jamSelesai)
    }

    private fun calculateRemainingSeconds(jamSelesai: String?): Long {
        if (jamSelesai.isNullOrBlank()) return 0L

        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        )

        return try {
            for (formatter in formats) {
                try {
                    formatter.timeZone = TimeZone.getDefault()
                    val endDate: Date = formatter.parse(jamSelesai) ?: continue
                    val diffMillis = endDate.time - System.currentTimeMillis()
                    return maxOf(0L, diffMillis / 1000)
                } catch (_: Exception) {
                }
            }
            0L
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
        return if (cleaned.contains(" ")) cleaned.substringAfterLast(" ").take(5) else cleaned.take(5)
    }

    private fun formatRupiah(value: Long): String {
        return "Rp${"%,d".format(Locale("in", "ID"), value).replace(',', '.')}/jam"
    }
}