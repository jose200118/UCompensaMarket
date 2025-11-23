package com.grupoa.ucompensamarket.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.grupoa.ucompensamarket.AdaptadorProductos
import com.grupoa.ucompensamarket.CarritoActivity
import com.grupoa.ucompensamarket.CartItem
import com.grupoa.ucompensamarket.Productos
import com.grupoa.ucompensamarket.databinding.FragmentProductosBinding
import com.grupoa.ucompensamarket.ProductoFormActivity
import com.grupoa.ucompensamarket.SessionManager

class FragmentProductos : Fragment() {
    private lateinit var binding: FragmentProductosBinding
    private lateinit var mContext: Context
    private var productosAdaptador: AdaptadorProductos? = null
    private var productoLista: ArrayList<Productos> = ArrayList()

    private lateinit var auth: FirebaseAuth
    private var authListener: FirebaseAuth.AuthStateListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View {
        binding = FragmentProductosBinding.inflate(inflater, container, false)

        binding.recyclerProductos.setHasFixedSize(true)
        binding.recyclerProductos.layoutManager = LinearLayoutManager(mContext)

        productoLista = ArrayList()
        listarProductos()

        auth = FirebaseAuth.getInstance()

        // Inicialmente ajustar visibilidad según la sesión actual
        updateFabVisibility()

        // FAB para crear producto
        binding.fabAddProduct.setOnClickListener {
            val intent = Intent(requireContext(), ProductoFormActivity::class.java)
            // SIN extras => modo CREAR
            startActivity(intent)
        }

        // FAB para abrir carrito — esto abre CarritoActivity donde verás los items
        binding.fabOpenCart.setOnClickListener {
            startActivity(Intent(requireContext(), CarritoActivity::class.java))
        }

        // Auth listener: cuando cambia el usuario (login/logout), actualizamos la UI
        authListener = FirebaseAuth.AuthStateListener {
            // SessionManager puede basarse en el usuario o en prefs; actualizamos visibilidades
            updateFabVisibility()
            // Opcional: si el contenido del adapter depende del usuario (ej. favoritos), refrescar
            productosAdaptador?.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        authListener?.let { auth.addAuthStateListener(it) }
        // Asegurarnos de que cuando el fragment entra en foreground, la visibilidad está correcta
        updateFabVisibility()
    }

    override fun onResume() {
        super.onResume()
        updateFabVisibility()
    }

    override fun onStop() {
        super.onStop()
        authListener?.let { auth.removeAuthStateListener(it) }
    }

    private fun updateFabVisibility() {
        // Si el fragmento ya no está asociado o la vista no existe, salir
        if (!isAdded || view == null) return

        // Permiso que no tiene vendedores: ocultar si NO es vendedor
        binding.fabAddProduct.visibility = if (SessionManager.isVendedor(requireContext())) View.VISIBLE else View.GONE

        // Permiso que no tiene clientes: ocultar si NO es cliente
        binding.fabOpenCart.visibility = if (SessionManager.isCliente(requireContext())) View.VISIBLE else View.GONE
    }

    private fun listarProductos() {
        val reference = FirebaseDatabase.getInstance().getReference("Productos").orderByChild("nombre")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productoLista.clear()
                for (sn in snapshot.children) {
                    val producto: Productos? = sn.getValue(Productos::class.java)
                    producto?.let {
                        // Guardar la clave de Firebase como uid para permitir editar/eliminar
                        it.uid = sn.key
                        productoLista.add(it)
                    }
                }

                productosAdaptador = AdaptadorProductos(mContext, productoLista, object : AdaptadorProductos.OnItemClickListener {

                    override fun onAgregar(producto: Productos, position: Int) {
                        // Guardar en Firebase -> Carrito/{userUid}/{productId}
                        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (user == null) {
                            Toast.makeText(mContext, "Por favor inicia sesión para usar el carrito.", Toast.LENGTH_SHORT).show()
                            return
                        }
                        val productId = producto.uid
                        if (productId.isNullOrEmpty()) {
                            Toast.makeText(mContext, "Producto inválido.", Toast.LENGTH_SHORT).show()
                            return
                        }

                        val db = com.google.firebase.database.FirebaseDatabase.getInstance().reference
                        val cartItemRef = db.child("Carrito").child(user.uid).child(productId)

                        cartItemRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                                if (snapshot.exists()) {
                                    val currentQty = snapshot.child("cantidad").getValue(Int::class.java) ?: 1
                                    cartItemRef.child("cantidad").setValue(currentQty + 1)
                                        .addOnSuccessListener {
                                            Toast.makeText(mContext, "${producto.nombre} incrementado en el carrito", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(mContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                else {
                                    val newItem = CartItem(productId, producto.nombre, producto.precio ?: 0.0, producto.imagenUrl, 1)
                                    cartItemRef.setValue(newItem)
                                        .addOnSuccessListener {
                                            Toast.makeText(mContext, "${producto.nombre} agregado al carrito", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(mContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }

                            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                                Toast.makeText(mContext, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }

                    override fun onEditar(producto: Productos, position: Int) {
                        // Abrir la actividad de Formulario pasando los datos del producto
                        val intent = Intent(mContext, ProductoFormActivity::class.java)
                        intent.putExtra(ProductoFormActivity.EXTRA_UID, producto.uid)
                        intent.putExtra(ProductoFormActivity.EXTRA_NOMBRE, producto.nombre)
                        intent.putExtra(ProductoFormActivity.EXTRA_DESCRIPCION, producto.descripcion)
                        intent.putExtra(ProductoFormActivity.EXTRA_PRECIO, producto.precio)
                        intent.putExtra(ProductoFormActivity.EXTRA_IMAGENURL, producto.imagenUrl)
                        intent.putExtra(ProductoFormActivity.EXTRA_LAT, producto.lat?.toString())
                        intent.putExtra(ProductoFormActivity.EXTRA_LNG, producto.lng?.toString())
                        startActivity(intent)
                    }

                    override fun onEliminar(producto: Productos, position: Int) {
                        // Confirmación antes de eliminar
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("Eliminar producto")
                        builder.setMessage("¿Estás seguro que deseas eliminar \"${producto.nombre}\"?")
                        builder.setPositiveButton("Sí") { _, _ ->
                            // Eliminar en Firebase usando su uid (clave)
                            val uid = producto.uid
                            if (!uid.isNullOrEmpty()) {
                                val ref = FirebaseDatabase.getInstance().getReference("Productos")
                                ref.child(uid).removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(mContext, "Producto eliminado", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(mContext, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(mContext, "No se pudo identificar el producto para eliminar.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        builder.setNegativeButton("No", null)
                        builder.show()
                    }
                })

                binding.recyclerProductos.adapter = productosAdaptador
                productosAdaptador?.notifyDataSetChanged()

                // Mostrar u ocultar mensaje vacío y RecyclerView
                binding.tvEmptyProducts.visibility = if (productoLista.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerProductos.visibility = if (productoLista.isEmpty()) View.GONE else View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(mContext, "Error al obtener productos: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}