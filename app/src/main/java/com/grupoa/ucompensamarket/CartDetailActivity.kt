package com.grupoa.ucompensamarket

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CartDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var ivProducto: ImageView
    private lateinit var tvNombre: TextView
    private lateinit var tvPrecio: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvCantidad: TextView
    private lateinit var btnPlus: ImageButton
    private lateinit var btnMinus: ImageButton
    private lateinit var btnEliminar: Button
    private lateinit var btnGuardar: Button
    private var currentItem: CartItem? = null
    private var productId: String? = null
    private var cartRef: DatabaseReference? = null
    private var cartListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart_detail)

        ivProducto = findViewById(R.id.detail_img)
        tvNombre = findViewById(R.id.detail_nombre)
        tvPrecio = findViewById(R.id.detail_precio)
        tvSubtotal = findViewById(R.id.detail_subtotal)
        tvCantidad = findViewById(R.id.detail_cantidad)
        btnPlus = findViewById(R.id.detail_btn_plus)
        btnMinus = findViewById(R.id.detail_btn_minus)
        btnEliminar = findViewById(R.id.detail_btn_eliminar)
        btnGuardar = findViewById(R.id.detail_btn_guardar)

        productId = intent.getStringExtra(EXTRA_PRODUCT_ID)
        val user = auth.currentUser
        if (user == null || productId.isNullOrEmpty()) {
            Toast.makeText(this, "No se pudo cargar detalle del carrito.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cartRef = db.child("Carrito").child(user.uid).child(productId!!)
        cartListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val item = snapshot.getValue(CartItem::class.java)
                if (item == null) {
                    Toast.makeText(this@CartDetailActivity, "El item ya no existe en el carrito.", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
                currentItem = item
                bindItem(item)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CartDetailActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        cartRef?.addValueEventListener(cartListener as ValueEventListener)

        btnPlus.setOnClickListener {
            val q = (currentItem?.cantidad ?: 1) + 1
            updateCantidad(q)
        }
        btnMinus.setOnClickListener {
            val q = ((currentItem?.cantidad ?: 1) - 1).coerceAtLeast(1)
            updateCantidad(q)
        }

        btnGuardar.setOnClickListener {
            Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnEliminar.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Eliminar del carrito")
            builder.setMessage("¿Eliminar ${currentItem?.nombre} del carrito?")
            builder.setPositiveButton("Sí") { _, _ ->
                cartRef?.removeValue()?.addOnSuccessListener {
                    Toast.makeText(this, "El item fue eliminado del carrito", Toast.LENGTH_SHORT).show()
                    finish()
                }?.addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            builder.setNegativeButton("No", null)
            builder.show()
        }
    }

    private fun bindItem(item: CartItem) {
        tvNombre.text = item.nombre ?: ""
        tvPrecio.text = String.format("$%.2f", item.precio)
        tvCantidad.text = item.cantidad.toString()
        val subtotal = item.precio * item.cantidad
        tvSubtotal.text = String.format("Subtotal: $%.2f", subtotal)
        Glide.with(this)
            .load(if (item.imagenUrl.isNullOrBlank()) R.drawable.ic_shopping_cart else item.imagenUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_shopping_cart)
            .into(ivProducto)
    }

    private fun updateCantidad(newQty: Int) {
        currentItem?.let {
            cartRef?.child("cantidad")?.setValue(newQty)
                ?.addOnSuccessListener {
                    // listener actualiza UI
                }
                ?.addOnFailureListener { e ->
                    Toast.makeText(this, "Error al actualizar cantidad: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cartListener?.let { cartRef?.removeEventListener(it) }
    }
}