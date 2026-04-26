package ananda.yoga.infinityps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

sealed class MonitoringZoneListItem {
    data class Header(val title: String) : MonitoringZoneListItem()
    data class PsItem(val item: PsMonitoringItem) : MonitoringZoneListItem()
}

class MonitoringZoneAdapter(
    private val currentUserId: Int,
    private val onAvailableClick: (PsMonitoringItem) -> Unit,
    private val onOwnedTransactionClick: (PsMonitoringItem) -> Unit
) : ListAdapter<MonitoringZoneListItem, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_PS = 1
        private const val RESERVASI_OPEN_SECONDS = 50 * 60L

        private val DIFF = object : DiffUtil.ItemCallback<MonitoringZoneListItem>() {
            override fun areItemsTheSame(
                oldItem: MonitoringZoneListItem,
                newItem: MonitoringZoneListItem
            ): Boolean {
                return when {
                    oldItem is MonitoringZoneListItem.Header && newItem is MonitoringZoneListItem.Header ->
                        oldItem.title == newItem.title
                    oldItem is MonitoringZoneListItem.PsItem && newItem is MonitoringZoneListItem.PsItem ->
                        oldItem.item.idPs == newItem.item.idPs
                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: MonitoringZoneListItem,
                newItem: MonitoringZoneListItem
            ): Boolean = oldItem == newItem
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvZoneTitle: TextView = itemView.findViewById(R.id.tvZoneTitle)
    }

    class PsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardRoot: CardView = itemView.findViewById(R.id.cardRoot)
        val tvNomorPs: TextView = itemView.findViewById(R.id.tvNomorPs)
        val tvTipeMini: TextView = itemView.findViewById(R.id.tvTipeMini)
        val tvStatusMini: TextView = itemView.findViewById(R.id.tvStatusMini)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MonitoringZoneListItem.Header -> VIEW_TYPE_HEADER
            is MonitoringZoneListItem.PsItem -> VIEW_TYPE_PS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_monitoring_zone_header, parent, false))
        } else {
            PsViewHolder(inflater.inflate(R.layout.item_monitoring_zone_ps, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is MonitoringZoneListItem.Header -> bindHeader(holder as HeaderViewHolder, row)
            is MonitoringZoneListItem.PsItem -> bindPs(holder as PsViewHolder, row.item)
        }
    }

    private fun bindHeader(holder: HeaderViewHolder, row: MonitoringZoneListItem.Header) {
        holder.tvZoneTitle.text = row.title
    }

    private fun bindPs(holder: PsViewHolder, item: PsMonitoringItem) {
        val ctx = holder.itemView.context
        holder.tvNomorPs.text = item.nomorPs
        holder.tvTipeMini.text = shortenTipe(item.tipe?.namaTipe ?: "-")

        val transaksi = item.activeTransaksi
        val transaksiStatus = transaksi?.statusTransaksi?.trim()?.lowercase()
        val statusBayar = transaksi?.pembayaran?.statusBayar?.trim()?.lowercase()
        val transaksiUserId = transaksi?.user?.idUser
        val sewa = transaksi?.detailSewa?.firstOrNull { it.idPs == item.idPs }

        val isCurrentUser = transaksiUserId == currentUserId
        val isOwnedWaiting = transaksiStatus == "waiting" && isCurrentUser
        val isOwnedScheduled = transaksiStatus == "dijadwalkan" && isCurrentUser
        val isOwnedActive = transaksiStatus == "aktif" && isCurrentUser

        val remainingSeconds = if (sewa != null) getRemainingSeconds(sewa) else 0L

        val isReservableSoon =
            transaksiStatus == "aktif" &&
                    sewa != null &&
                    remainingSeconds in 1..RESERVASI_OPEN_SECONDS

        holder.cardRoot.isClickable = false
        holder.cardRoot.isFocusable = false
        holder.cardRoot.setOnClickListener(null)

        when {
            item.statusPs.equals("maintenance", true) -> {
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_maintenance_zone)
                )
                holder.tvStatusMini.text = "Perbaikan"
                holder.tvStatusMini.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_maintenance_text)
                )
            }

            isOwnedWaiting -> {
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_owned_active_bg)
                )
                holder.tvStatusMini.text = "Pending"
                holder.tvStatusMini.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_owned_active_text)
                )
                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener { onOwnedTransactionClick(item) }
            }

            transaksiStatus == "waiting" -> {
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_booking_zone)
                )
                holder.tvStatusMini.text = "Booked"
                holder.tvStatusMini.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_booking_text)
                )
            }

            isOwnedScheduled -> {
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_owned_active_bg)
                )
                holder.tvStatusMini.text = "Punyaku"
                holder.tvStatusMini.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_owned_active_text)
                )
                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener { onOwnedTransactionClick(item) }
            }

            transaksiStatus == "dijadwalkan" -> {
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_booking_zone)
                )
                holder.tvStatusMini.text = "Booked"
                holder.tvStatusMini.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_booking_text)
                )
            }

            statusBayar == "menunggu_validasi" && isCurrentUser -> {
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_owned_active_bg)
                )
                holder.tvStatusMini.text = "Validasi"
                holder.tvStatusMini.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_owned_active_text)
                )
                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener { onOwnedTransactionClick(item) }
            }

            isOwnedActive -> {
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_owned_active_bg)
                )
                holder.tvStatusMini.text = if (sewa != null) {
                    formatCountdown(remainingSeconds)
                } else {
                    "Punyaku"
                }
                holder.tvStatusMini.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_owned_active_text)
                )
                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener { onOwnedTransactionClick(item) }
            }

            isReservableSoon -> {
                // masih dipakai, tapi reservasi sudah dibuka
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_booking_zone)
                )
                holder.tvStatusMini.text = "Tersedia"
                holder.tvStatusMini.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_booking_text)
                )
                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener { onAvailableClick(item) }
            }

            item.statusPs.equals("digunakan", true) || transaksiStatus == "aktif" -> {
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_aktif_bg)
                )
                holder.tvStatusMini.text = if (sewa != null) {
                    formatCountdown(remainingSeconds)
                } else {
                    "Dipakai"
                }
                holder.tvStatusMini.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_aktif_text)
                )
            }

            else -> {
                holder.cardRoot.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.card_available_zone)
                )
                holder.tvStatusMini.text = "Tersedia"
                holder.tvStatusMini.setTextColor(
                    ContextCompat.getColor(ctx, R.color.status_tersedia_text)
                )
                holder.cardRoot.isClickable = true
                holder.cardRoot.isFocusable = true
                holder.cardRoot.setOnClickListener { onAvailableClick(item) }
            }
        }
    }

    private fun shortenTipe(raw: String): String {
        val text = raw.trim()
        return when {
            text.contains("vip", true) -> "VIP"
            text.contains("reg", true) -> "REG"
            text.contains("pro", true) -> "PRO"
            text.length > 8 -> text.take(8)
            else -> text
        }
    }

    private fun getRemainingSeconds(sewa: DetailSewa): Long {
        if (sewa.sisaDetik > 0) return sewa.sisaDetik
        return parseServerDateToMillis(sewa.jamSelesai)?.let { end ->
            maxOf(0L, (end - System.currentTimeMillis()) / 1000)
        } ?: 0L
    }

    private fun parseServerDateToMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null

        val candidates = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )

        candidates.forEach { pattern ->
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.timeZone =
                    if (pattern.contains("'Z'")) TimeZone.getTimeZone("UTC")
                    else TimeZone.getDefault()
                val date = sdf.parse(raw)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }

        return null
    }

    private fun formatCountdown(totalSeconds: Long): String {
        if (totalSeconds <= 0) return "Habis"

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        return when {
            hours > 0 -> "${hours}j ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }
}