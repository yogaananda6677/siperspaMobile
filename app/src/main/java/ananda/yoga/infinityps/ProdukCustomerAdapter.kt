package ananda.yoga.infinityps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ProdukCustomerAdapter(
    private val onQtyChanged: () -> Unit
) : RecyclerView.Adapter<ProdukCustomerAdapter.ViewHolder>() {

    private val filteredItems = mutableListOf<CartProdukItem>()
    private val selectedState = linkedMapOf<Int, CartProdukItem>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamaProduk: TextView = itemView.findViewById(R.id.tvNamaProduk)
        val tvKategoriProduk: TextView = itemView.findViewById(R.id.tvKategoriProduk)
        val tvHargaProduk: TextView = itemView.findViewById(R.id.tvHargaProduk)
        val tvQty: TextView = itemView.findViewById(R.id.tvQtyProduk)
        val btnMinus: TextView = itemView.findViewById(R.id.btnMinusProduk)
        val btnPlus: TextView = itemView.findViewById(R.id.btnPlusProduk)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produk_customer, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = filteredItems.size



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItems[position]
        val produk = item.produk

        holder.tvNamaProduk.text = produk.nama
        holder.tvKategoriProduk.text = produk.jenis?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale("id", "ID")) else it.toString()
        } ?: "-"
        holder.tvHargaProduk.text = formatRupiah(produk.harga)
        holder.tvQty.text = item.qty.toString()

        holder.btnMinus.setOnClickListener {
            if (item.qty > 0) {
                item.qty -= 1
                if (item.qty == 0) {
                    selectedState.remove(produk.idProduk)
                } else {
                    selectedState[produk.idProduk] = item.copy()
                }
                notifyItemChanged(position)
                onQtyChanged()
            }
        }

        holder.btnPlus.setOnClickListener {
            item.qty += 1
            selectedState[produk.idProduk] = item.copy()
            notifyItemChanged(position)
            onQtyChanged()
        }
    }


    fun submitFilteredList(newItems: List<CartProdukItem>) {
        filteredItems.clear()
        filteredItems.addAll(newItems.map { incoming ->
            selectedState[incoming.produk.idProduk]?.copy() ?: incoming
        })
        notifyDataSetChanged()
    }

    fun getSelectedProduk(): List<ProdukRequest> {
        return selectedState.values
            .filter { it.qty > 0 }
            .map {
                ProdukRequest(
                    idProduk = it.produk.idProduk,
                    qty = it.qty
                )
            }
    }

    fun getSelectedCartItems(): List<CartProdukItem> {
        return selectedState.values.filter { it.qty > 0 }
    }

    private fun formatRupiah(value: Long): String {
        return "Rp " + "%,d".format(Locale("id", "ID"), value).replace(',', '.')
    }
}