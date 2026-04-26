package ananda.yoga.infinityps

import com.google.gson.annotations.SerializedName

data class BayarRequest(
    @SerializedName("metode_pembayaran") val metodePembayaran: String,
    @SerializedName("total_bayar") val totalBayar: Double? = null
)

data class BayarResponse(
    val message: String,
    val data: HistoryItem?
)

data class QrisPaymentResponse(
    val message: String,
    val data: QrisPaymentData?
)

data class QrisPaymentData(
    val transaksi: HistoryItem?,
    val payment: HistoryPembayaran?,
    val midtrans: MidtransRawResult?
)

data class MidtransRawResult(
    @SerializedName("transaction_id") val transactionId: String?,
    @SerializedName("transaction_status") val transactionStatus: String?,
    @SerializedName("payment_type") val paymentType: String?,
    @SerializedName("qr_string") val qrString: String?,
    val actions: List<MidtransAction>?
)

data class MidtransAction(
    val name: String?,
    val method: String?,
    val url: String?
)