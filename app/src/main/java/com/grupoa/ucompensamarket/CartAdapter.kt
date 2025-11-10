package com.grupoa.ucompensamarket

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CartAdapter(
    private val context: Context,
    private val items: List<CartItem>,
    private val listener: CartListener
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    interface CartListener {
        fun onChangeQuantity(item: CartItem, newQty: Int)
        fun onRemove(item: CartItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.item_carrito, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNombre.text = item.nombre ?: ""
        holder.tvPrecio.text = String.format("$%.2f", item.precio)
        holder.tvCantidad.text = item.cantidad.toString()

        Glide.with(context)
            .load(if (item.imagenUrl.isNullOrBlank()) R.drawable.ic_shopping_cart else item.imagenUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_shopping_cart)
            .into(holder.ivProducto)

        holder.btnPlus.setOnClickListener {
            val nueva = item.cantidad + 1
            listener.onChangeQuantity(item, nueva)
        }
        holder.btnMinus.setOnClickListener {
            val nueva = (item.cantidad - 1).coerceAtLeast(1)
            listener.onChangeQuantity(item, nueva)
        }
        holder.btnRemove.setOnClickListener {
            listener.onRemove(item)
        }

        holder.itemView.setOnClickListener {
            // Abrir detalle del item del carrito
            val intent = Intent(context, CartDetailActivity::class.java)
            intent.putExtra(CartDetailActivity.EXTRA_PRODUCT_ID, item.productId)
            context.startActivity(intent)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProducto: ImageView = itemView.findViewById(R.id.itemCart_img)
        val tvNombre: TextView = itemView.findViewById(R.id.itemCart_nombre)
        val tvPrecio: TextView = itemView.findViewById(R.id.itemCart_precio)
        val tvCantidad: TextView = itemView.findViewById(R.id.itemCart_cantidad)
        val btnPlus: ImageButton = itemView.findViewById(R.id.btnPlus)
        val btnMinus: ImageButton = itemView.findViewById(R.id.btnMinus)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)
    }
}