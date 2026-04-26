package ananda.yoga.infinityps

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object TransaksiReminderScheduler {

    private const val TEN_MIN_BEFORE_LOCK_OFFSET_MINUTES = 40L
    private const val LOCK_OFFSET_MINUTES = 30L

    fun scheduleReminders(context: Context, transaksi: HistoryItem) {
        if (!transaksi.statusTransaksi.equals("aktif", true)) return

        val detailSewa = transaksi.detailSewa.firstOrNull() ?: return
        val jamSelesaiMillis = parseDateToMillis(detailSewa.jamSelesai) ?: return

        val now = System.currentTimeMillis()
        val notif40Time = jamSelesaiMillis - TimeUnit.MINUTES.toMillis(TEN_MIN_BEFORE_LOCK_OFFSET_MINUTES)
        val notif30Time = jamSelesaiMillis - TimeUnit.MINUTES.toMillis(LOCK_OFFSET_MINUTES)

        scheduleOne(
            context = context,
            uniqueName = "transaksi_${transaksi.idTransaksi}_notif_40",
            runAtMillis = notif40Time,
            title = "Pengingat transaksi",
            message = "10 menit lagi kamu tidak bisa menambah waktu atau produk untuk transaksi #${transaksi.idTransaksi}.",
            notificationId = transaksi.idTransaksi * 10 + 40
        )

        scheduleOne(
            context = context,
            uniqueName = "transaksi_${transaksi.idTransaksi}_notif_30",
            runAtMillis = notif30Time,
            title = "Batas perubahan transaksi",
            message = "Sekarang transaksi #${transaksi.idTransaksi} sudah tidak bisa menambah waktu atau produk lagi.",
            notificationId = transaksi.idTransaksi * 10 + 30
        )
    }

    private fun scheduleOne(
        context: Context,
        uniqueName: String,
        runAtMillis: Long,
        title: String,
        message: String,
        notificationId: Int
    ) {
        val delay = runAtMillis - System.currentTimeMillis()
        if (delay <= 0) return

        val input = Data.Builder()
            .putString(TransaksiReminderWorker.KEY_TITLE, title)
            .putString(TransaksiReminderWorker.KEY_MESSAGE, message)
            .putInt(TransaksiReminderWorker.KEY_NOTIFICATION_ID, notificationId)
            .build()

        val request = OneTimeWorkRequestBuilder<TransaksiReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
    }

    private fun parseDateToMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null

        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
        )

        for (format in formats) {
            try {
                val parsed = format.parse(raw)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }

        return null
    }
}