package ananda.yoga.infinityps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ProdukCustomerAdapter(
    private val items: MutableList<CartProdukItem>,
    private val onQtyChanged: () -> Unit
) : RecyclerView.Adapter<ProdukCustomerAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamaProduk: TextView = itemView.findViewById(R.id.tvNamaProduk)
        val tvHargaProduk: TextView = itemView.findViewById(R.id.tvHargaProduk)
        val tvStockProduk: TextView = itemView.findViewById(R.id.tvStockProduk)
        val tvQtyProduk: TextView = itemView.findViewById(R.id.tvQtyProduk)
        val btnMinus: ImageButton = itemView.findViewById(R.id.btnMinus)
        val btnPlus: ImageButton = itemView.findViewById(R.id.btnPlus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produk_customer, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvNamaProduk.text = item.produk.nama
        holder.tvHargaProduk.text = formatRupiah(item.produk.harga)
        holder.tvStockProduk.text = "stok: ${item.produk.stock}"
        holder.tvQtyProduk.text = item.qty.toString()

        holder.btnMinus.setOnClickListener {
            if (item.qty > 0) {
                item.qty--
                holder.tvQtyProduk.text = item.qty.toString()
                onQtyChanged()
            }
        }

        holder.btnPlus.setOnClickListener {
            if (item.qty < item.produk.stock) {
                item.qty++
                holder.tvQtyProduk.text = item.qty.toString()
                onQtyChanged()
            }
        }
    }

    private fun formatRupiah(value: Long): String {
        return "Rp " + "%,d".format(Locale("id", "ID"), value).replace(',', '.')
    }
}