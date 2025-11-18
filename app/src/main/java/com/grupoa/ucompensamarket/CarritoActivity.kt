package com.grupoa.ucompensamarket

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CarritoActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var rvCart: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnCheckout: Button

    private val cartList = ArrayList<CartItem>()
    private var cartAdapter: CartAdapter? = null
    private var cartRef: DatabaseReference? = null
    private var cartListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carrito)

        rvCart = findViewById(R.id.recyclerCarrito)
        tvEmpty = findViewById(R.id.tvEmptyCart)
        tvTotal = findViewById(R.id.tvTotal)
        btnCheckout = findViewById(R.id.btnCheckout)

        rvCart.layoutManager = LinearLayoutManager(this)
        cartAdapter = CartAdapter(this, cartList, object : CartAdapter.CartListener {
            override fun onChangeQuantity(item: CartItem, newQty: Int) {
                val user = auth.currentUser ?: return
                val ref = db.child("Carrito").child(user.uid).child(item.productId ?: "")
                ref.child("cantidad").setValue(newQty)
            }

            override fun onRemove(item: CartItem) {
                val user = auth.currentUser ?: return
                val ref = db.child("Carrito").child(user.uid).child(item.productId ?: "")
                ref.removeValue()
            }
        })
        rvCart.adapter = cartAdapter

        btnCheckout.setOnClickListener {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(this, "Inicia sesión para realizar la compra", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Registrar compra simple: crear nodo Compras/{userUid}/{pushId} con items y total
            val comprasRef = db.child("Compras").child(user.uid).push()
            val compraData = HashMap<String, Any>()
            compraData["items"] = cartList.map {
                mapOf(
                    "productId" to it.productId,
                    "nombre" to it.nombre,
                    "precio" to it.precio,
                    "cantidad" to it.cantidad,
                    "imagenUrl" to it.imagenUrl
                )
            }
            compraData["total"] = cartList.sumByDouble { it.precio * it.cantidad }
            compraData["timestamp"] = ServerValue.TIMESTAMP

            comprasRef.setValue(compraData).addOnSuccessListener {
                // limpiar carrito
                val cartRefUser = db.child("Carrito").child(user.uid)
                cartRefUser.removeValue().addOnSuccessListener {
                    Toast.makeText(this, "Compra registrada. ¡Gracias!", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error al procesar compra: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Inicia sesión para ver tu carrito", Toast.LENGTH_SHORT).show()
            tvEmpty.visibility = View.VISIBLE
            rvCart.visibility = View.GONE
            tvTotal.visibility = View.GONE
            btnCheckout.visibility = View.GONE
            return
        }

        cartRef = db.child("Carrito").child(user.uid)
        cartListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cartList.clear()
                for (sn in snapshot.children) {
                    val item = sn.getValue(CartItem::class.java)
                    item?.let { cartList.add(it) }
                }
                cartAdapter?.notifyDataSetChanged()
                tvEmpty.visibility = if (cartList.isEmpty()) View.VISIBLE else View.GONE
                rvCart.visibility = if (cartList.isEmpty()) View.GONE else View.VISIBLE

                val total = cartList.sumByDouble { it.precio * it.cantidad }
                tvTotal.text = String.format("Total: $%.2f", total)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CarritoActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        cartRef?.addValueEventListener(cartListener as ValueEventListener)
    }

    override fun onStop() {
        super.onStop()
        cartListener?.let { cartRef?.removeEventListener(it) }
    }


}