package com.grupoa.ucompensamarket

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AdaptadorProductos(private val context: Context, private val listaProductos: List<Productos>,
                         private val listener: OnItemClickListener? = null) : RecyclerView.Adapter<AdaptadorProductos.ViewHolder>() {

    interface OnItemClickListener {
        fun onAgregar(producto: Productos, position: Int)
        fun onEditar(producto: Productos, position: Int)
        fun onEliminar(producto: Productos, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listaProductos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val producto = listaProductos[position]
        holder.nombre.text = producto.nombre ?: ""
        holder.precio.text = String.format("$%.2f", producto.precio)

        val url = producto.imagenUrl
        Glide.with(holder.itemView.context)
            .load(if (url.isNullOrBlank()) R.drawable.ic_shopping_cart else url)
            .centerCrop()
            .placeholder(R.drawable.ic_shopping_cart)
            .error(R.drawable.ic_shopping_cart)
            .into(holder.imagenProducto)

        // ESTABLECER VISIBILIDAD POR DEFECTO PARA EVITAR ESTADOS RESIDUALES AL RECICLAR VIEWS
        holder.btnAgregar.visibility = View.VISIBLE
        holder.btnVerDetalle.visibility = View.VISIBLE
        holder.btnEditar.visibility = View.VISIBLE
        holder.btnEliminar.visibility = View.VISIBLE

        // Permiso que no tiene el cliente -> ocultar botones de edición/elim si NO es vendedor
        if (!SessionManager.isVendedor(context)) {
            holder.btnEditar.visibility = View.GONE
            holder.btnEliminar.visibility = View.GONE
        }

        // Permisos que no tiene el vendedor -> ocultar botones de cliente si NO es cliente
        if (!SessionManager.isCliente(context)) {
            holder.btnAgregar.visibility = View.GONE
            holder.btnVerDetalle.visibility = View.GONE
        }

        // Click listeners
        holder.btnAgregar.setOnClickListener {
            listener?.onAgregar(producto, position)
                ?: Toast.makeText(context, "${producto.nombre} agregado (callback no implementado)", Toast.LENGTH_SHORT).show()
        }
        holder.btnVerDetalle.setOnClickListener {
            // Usamos onEditar como "ver detalle" si así lo definiste; si tienes otro callback, cámbialo.
            listener?.onEditar(producto, position)
                ?: Toast.makeText(context, "Detalle ${producto.nombre} (callback no implementado)", Toast.LENGTH_SHORT).show()
        }
        holder.btnEditar.setOnClickListener {
            listener?.onEditar(producto, position)
                ?: Toast.makeText(context, "Editar ${producto.nombre} (callback no implementado)", Toast.LENGTH_SHORT).show()
        }
        holder.btnEliminar.setOnClickListener {
            listener?.onEliminar(producto, position)
                ?: Toast.makeText(context, "Eliminar ${producto.nombre} (callback no implementado)", Toast.LENGTH_SHORT).show()
        }
    }

    // Método público para forzar refresco desde el fragment si lo deseas
    fun refresh() {
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagenProducto: ImageView = itemView.findViewById(R.id.item_imgProducto)
        val nombre: TextView = itemView.findViewById(R.id.item_nombreProducto)
        val precio: TextView = itemView.findViewById(R.id.item_precioProducto)
        val btnAgregar: ImageButton = itemView.findViewById(R.id.btnAgregarCarrito)
        val btnVerDetalle: ImageButton = itemView.findViewById(R.id.btnVerDetalle)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminar)
    }
}