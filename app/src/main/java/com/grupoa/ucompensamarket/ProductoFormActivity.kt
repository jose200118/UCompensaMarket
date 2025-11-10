package com.grupoa.ucompensamarket

import android.os.Bundle
import android.text.TextUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase

class ProductoFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_UID = "extra_uid"
        const val EXTRA_NOMBRE = "extra_nombre"
        const val EXTRA_DESCRIPCION = "extra_descripcion"
        const val EXTRA_PRECIO = "extra_precio"
        const val EXTRA_IMAGENURL = "extra_imagenurl"
    }

    private lateinit var edtNombre: EditText
    private lateinit var edtDescripcion: EditText
    private lateinit var edtPrecio: EditText
    private lateinit var edtImagenUrl: EditText
    private lateinit var ivPreview: ImageView
    private lateinit var btnGuardar: Button

    private val dbRef by lazy { FirebaseDatabase.getInstance().getReference("Productos") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_producto_form)

        edtNombre = findViewById(R.id.edtNombre)
        edtDescripcion = findViewById(R.id.edtDescripcion)
        edtPrecio = findViewById(R.id.edtPrecio)
        edtImagenUrl = findViewById(R.id.edtImagenUrl)
        ivPreview = findViewById(R.id.ivPreview)
        btnGuardar = findViewById(R.id.btnGuardarProducto)

        // Leer extras (si vienen, estamos en modo EDIT)
        val uid = intent.getStringExtra(EXTRA_UID)
        val nombre = intent.getStringExtra(EXTRA_NOMBRE)
        val descripcion = intent.getStringExtra(EXTRA_DESCRIPCION)
        val precio = intent.getDoubleExtra(EXTRA_PRECIO, 0.0)
        val imagenUrl = intent.getStringExtra(EXTRA_IMAGENURL)

        val isEdit = !uid.isNullOrEmpty()

        if (isEdit) {
            // llenar campos
            edtNombre.setText(nombre)
            edtDescripcion.setText(descripcion)
            if (precio != 0.0) edtPrecio.setText(precio.toString())
            if (!imagenUrl.isNullOrEmpty()) {
                edtImagenUrl.setText(imagenUrl)
                Glide.with(this).load(imagenUrl).centerCrop().placeholder(R.drawable.ic_shopping_cart).into(ivPreview)
            }
            btnGuardar.text = "Actualizar"
        }
        else {
            btnGuardar.text = "Crear"
        }

        // Actualizar preview cuando pierde foco en la URL (simple)
        edtImagenUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val url = edtImagenUrl.text.toString().trim()
                if (url.isNotEmpty()) {
                    Glide.with(this).load(url).centerCrop().placeholder(R.drawable.ic_shopping_cart).error(R.drawable.ic_shopping_cart).into(ivPreview)
                }
            }
        }

        btnGuardar.setOnClickListener {
            hideKeyboard()
            btnGuardar.isEnabled = false

            val nombreTxt = edtNombre.text.toString().trim()
            val descripcionTxt = edtDescripcion.text.toString().trim()
            val precioTxt = edtPrecio.text.toString().trim()
            val imagenTxt = edtImagenUrl.text.toString().trim()

            if (TextUtils.isEmpty(nombreTxt)) {
                edtNombre.error = "Ingrese nombre"
                btnGuardar.isEnabled = true
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(precioTxt)) {
                edtPrecio.error = "Ingrese precio"
                btnGuardar.isEnabled = true
                return@setOnClickListener
            }

            val precioVal = precioTxt.toDoubleOrNull()
            if (precioVal == null) {
                edtPrecio.error = "Precio inválido"
                btnGuardar.isEnabled = true
                return@setOnClickListener
            }

            if (isEdit) {
                // UPDATE
                val producto = Productos(uid, nombreTxt, descripcionTxt, precioVal, if (imagenTxt.isEmpty()) null else imagenTxt)
                dbRef.child(uid!!).setValue(producto)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnGuardar.isEnabled = true
                    }
            }
            else {
                // CREATE
                val newRef = dbRef.push()
                val key = newRef.key
                val producto = Productos(key, nombreTxt, descripcionTxt, precioVal, if (imagenTxt.isEmpty()) null else imagenTxt)
                newRef.setValue(producto)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Producto creado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al crear: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnGuardar.isEnabled = true
                    }
            }
        }
    }

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            val v = currentFocus
            if (v != null) imm.hideSoftInputFromWindow(v.windowToken, 0)
        } catch (_: Exception) { }
    }
}