//package com.grupoa.ucompensamarket
//
//import android.content.Context
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import org.w3c.dom.Text
//import kotlin.Double
//
//class AdaptadorProductos( context: Context, listaProductos: List<Productos>)
//    : RecyclerView.Adapter<AdaptadorProductos.ViewHolder?>() {
//
//        private val context : Context
//        private val listaProductos : List<Productos>
//
//        init {
//            this.context = context
//            this.listaProductos = listaProductos
//        }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view : View = LayoutInflater.from(context).inflate(R.layout.item_producto, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun getItemCount(): Int {
//        return listaProductos.size
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val productos : Productos = listaProductos[position]
//        holder.nombre.text = productos.nombre
//        holder.precio.text = "$${productos.precio}"
//
//        // Cargar la imagen con Glide
//        Glide.with(holder.itemView.context)
//            .load(productos.imagenUrl)
//            .placeholder(R.drawable.ic_shopping_cart)
//            .error(R.drawable.ic_shopping_cart)
//            .into(holder.imagenUrl)
//    }
//
//
//
//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
////        var uid: TextView
//        var imagenUrl: ImageView
//        var nombre: TextView
////        var descripcion: TextView
//        var precio: TextView
//
//        init {
////            uid = itemView.findViewById(R.id.item_uid)
//            imagenUrl = itemView.findViewById(R.id.item_imgProducto)
//            nombre = itemView.findViewById(R.id.item_nombreProducto)
////            descripcion = itemView.findViewById(R.id.item_descripcion)
//            precio = itemView.findViewById(R.id.item_precioProducto)
//        }
//    }
//}


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

class AdaptadorProductos(
    private val context: Context,
    private val listaProductos: List<Productos>,
    private val listener: OnItemClickListener? = null
) : RecyclerView.Adapter<AdaptadorProductos.ViewHolder>() {

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

        holder.btnAgregar.setOnClickListener {
            listener?.onAgregar(producto, position)
                ?: Toast.makeText(context, "${producto.nombre} agregado (callback no implementado)", Toast.LENGTH_SHORT).show()
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

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagenProducto: ImageView = itemView.findViewById(R.id.item_imgProducto)
        val nombre: TextView = itemView.findViewById(R.id.item_nombreProducto)
        val precio: TextView = itemView.findViewById(R.id.item_precioProducto)
        val btnAgregar: ImageButton = itemView.findViewById(R.id.btnAgregarCarrito)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditar)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminar)
    }
}