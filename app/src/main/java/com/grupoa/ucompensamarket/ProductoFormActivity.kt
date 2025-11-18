package com.grupoa.ucompensamarket

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProductoFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_UID = "extra_uid"
        const val EXTRA_NOMBRE = "extra_nombre"
        const val EXTRA_DESCRIPCION = "extra_descripcion"
        const val EXTRA_PRECIO = "extra_precio"
        const val EXTRA_IMAGENURL = "extra_imagenurl"
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
    }

    private lateinit var edtNombre: EditText
    private lateinit var edtDescripcion: EditText
    private lateinit var edtPrecio: EditText
    private lateinit var edtImagenUrl: EditText
    private lateinit var ivPreview: ImageView
    private lateinit var btnLocalizacion: Button
    private lateinit var btnGuardar: Button
    private lateinit var tvUbicacion: TextView
    private lateinit var contenedorCamara: LinearLayout
    private lateinit var btnSeleccionarImagen: ImageButton

    private lateinit var mapView: MapView
    private var marker: Marker? = null

    private lateinit var fusedLocation: FusedLocationProviderClient
    private var latitud: Double? = null
    private var longitud: Double? = null
    private val REQUEST_LOCATION = 1001

    private val dbRef by lazy { FirebaseDatabase.getInstance().getReference("Productos") }

    // Activity Result launchers
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // Base64 de la imagen capturada/seleccionada (sin prefijo "data:")
    private var imageBase64: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar OSMdroid CONFIG
        Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        setContentView(R.layout.activity_producto_form)

        // Views
        edtNombre = findViewById(R.id.edtNombre)
        edtDescripcion = findViewById(R.id.edtDescripcion)
        edtPrecio = findViewById(R.id.edtPrecio)
        edtImagenUrl = findViewById(R.id.edtImagenUrl)
        ivPreview = findViewById(R.id.ivPreview)
        btnLocalizacion = findViewById(R.id.btnObtenerUbicacion)
        btnGuardar = findViewById(R.id.btnGuardarProducto)
        tvUbicacion = findViewById(R.id.tvUbicacion)
        contenedorCamara = findViewById(R.id.contenCamara)
        btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen)

        mapView = findViewById(R.id.mapView)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(16.0)
        mapView.controller.setCenter(GeoPoint(4.60971, -74.08175)) // Bogotá por defecto

        fusedLocation = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar activity result launchers
        initActivityResultLaunchers()

        // Datos si venimos a EDITAR
        val uid = intent.getStringExtra(EXTRA_UID)
        val nombre = intent.getStringExtra(EXTRA_NOMBRE)
        val descripcion = intent.getStringExtra(EXTRA_DESCRIPCION)
        val precio = intent.getDoubleExtra(EXTRA_PRECIO, 0.0)
        val imagenUrl = intent.getStringExtra(EXTRA_IMAGENURL)
        val latitudU = intent.getStringExtra(EXTRA_LAT)
        val longitudU = intent.getStringExtra(EXTRA_LNG)

        // Mostrar/ocultar contenedor de cámara según rol
        edtImagenUrl.visibility = View.GONE
        if (!SessionManager.isVendedor(this)) {
            contenedorCamara.visibility = View.GONE
            btnLocalizacion.visibility = View.GONE
            btnGuardar.visibility = View.GONE

            edtNombre.isEnabled = false            // no editable, aspecto por defecto de deshabilitado
            edtNombre.isFocusable = false
            edtNombre.isClickable = false

            edtDescripcion.isEnabled = false            // no editable, aspecto por defecto de deshabilitado
            edtDescripcion.isFocusable = false
            edtDescripcion.isClickable = false

            edtPrecio.isEnabled = false            // no editable, aspecto por defecto de deshabilitado
            edtPrecio.isFocusable = false
            edtPrecio.isClickable = false
        }

        val isEdit = !uid.isNullOrEmpty()

        if (isEdit) {
            // Cargar lat/lon del intent, si vienen
            if (!latitudU.isNullOrEmpty() && !longitudU.isNullOrEmpty()) {
                latitud = latitudU.toDoubleOrNull()
                longitud = longitudU.toDoubleOrNull()

                if (latitud != null && longitud != null) {
                    tvUbicacion.text = "Ubicación: $latitud, $longitud"
                    actualizarMapa(latitud!!, longitud!!)
                }
            }

            edtNombre.setText(nombre)
            edtDescripcion.setText(descripcion)
            if (precio != 0.0) edtPrecio.setText(precio.toString())
            if (!imagenUrl.isNullOrEmpty()) {
                // imagenUrl puede ser URL o Base64 (data:...); manejar ambos casos
                edtImagenUrl.setText(imagenUrl)
                if (imagenUrl.startsWith("data:image", ignoreCase = true)) {
                    // base64 con prefijo data:
                    val base64Part = imagenUrl.substringAfter(",")
                    val bmp = base64ToBitmap(base64Part)
                    if (bmp != null) {
                        ivPreview.setImageBitmap(bmp)
                        imageBase64 = base64Part
                    }
                    else {
                        ivPreview.setImageResource(R.drawable.ic_shopping_cart)
                    }
                }
                else if (imagenUrl.length > 1000 && !imagenUrl.contains("http", true)) {
                    // Probablemente sea base64 sin prefijo
                    val bmp = base64ToBitmap(imagenUrl)
                    if (bmp != null) {
                        ivPreview.setImageBitmap(bmp)
                        imageBase64 = imagenUrl
                    }
                    else {
                        Glide.with(this).load(imagenUrl)
                            .centerCrop()
                            .placeholder(R.drawable.ic_shopping_cart)
                            .into(ivPreview)
                    }
                }
                else {
                    // URL normal
                    Glide.with(this).load(imagenUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_shopping_cart)
                        .error(R.drawable.ic_shopping_cart)
                        .into(ivPreview)
                }
            }
            btnGuardar.text = "Actualizar"
        }
        else {
            btnGuardar.text = "Crear"
        }

        // Actualizar preview cuando pierde foco (para URL)
        edtImagenUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val url = edtImagenUrl.text.toString().trim()
                if (url.isNotEmpty()) {
                    if (url.startsWith("data:image", ignoreCase = true)) {
                        val base64Part = url.substringAfter(",")
                        base64ToBitmap(base64Part)?.let { ivPreview.setImageBitmap(it) }
                        imageBase64 = base64Part
                    }
                    else if (url.length > 1000 && !url.contains("http", true)) {
                        // prob. base64 sin prefijo
                        base64ToBitmap(url)?.let {
                            ivPreview.setImageBitmap(it)
                            imageBase64 = url
                        }
                    }
                    else {
                        Glide.with(this).load(url)
                            .centerCrop()
                            .placeholder(R.drawable.ic_shopping_cart)
                            .error(R.drawable.ic_shopping_cart)
                            .into(ivPreview)
                        imageBase64 = null
                    }
                }
            }
        }

        // click para seleccionar imagen (camara/galeria)
        btnSeleccionarImagen.setOnClickListener {
            showImagePickerDialog()
        }

        btnGuardar.setOnClickListener {
            hideKeyboard()
            btnGuardar.isEnabled = false

            val nombreTxt = edtNombre.text.toString().trim()
            val descripcionTxt = edtDescripcion.text.toString().trim()
            val precioTxt = edtPrecio.text.toString().trim()
            val imagenTxtFromField = edtImagenUrl.text.toString().trim()

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

            // Preferir el Base64 tomado/seleccionado (imageBase64) si existe,
            // sino tomar lo que el usuario haya puesto en el campo (puede ser URL o Base64)
            val imagenParaGuardar: String? = when {
                !imageBase64.isNullOrEmpty() -> "data:image/jpeg;base64,${imageBase64!!}"
                imagenTxtFromField.isEmpty() -> null
                else -> imagenTxtFromField
            }

            if (isEdit) {
                // UPDATE
                val producto = Productos(
                    uid,
                    nombreTxt,
                    descripcionTxt,
                    precioVal,
                    imagenParaGuardar,
                    latitud,
                    longitud
                )

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

                val producto = Productos(
                    key,
                    nombreTxt,
                    descripcionTxt,
                    precioVal,
                    imagenParaGuardar,
                    latitud,
                    longitud
                )

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

        btnLocalizacion.setOnClickListener {
            obtenerUbicacion()
        }
    }

    private fun initActivityResultLaunchers() {
        // Permisos
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { perms ->
            // no hacemos nada especial aquí; la acción que solicitó permisos
            // reintenta abrir cámara/galería según necesidad.
        }

        // Cámara (thumbnail)
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val bmp = data?.extras?.get("data") as? Bitmap
                if (bmp != null) {
                    handleImageBitmap(bmp)
                }
                else {
                    Toast.makeText(this, "No se obtuvo imagen de la cámara", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Galería
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val uri: Uri? = data?.data
                if (uri != null) {
                    val bmp = uriToBitmap(uri)
                    if (bmp != null) {
                        handleImageBitmap(bmp)
                    }
                    else {
                        Toast.makeText(this, "No se pudo leer la imagen seleccionada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showImagePickerDialog() {
        val items = arrayOf("Tomar foto", "Seleccionar de la galería")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        // Solicitar permiso cámara si no está
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        }
                        openCamera()
                    }
                    1 -> {
                        // Permiso lectura (según SDK)
                        val perms = mutableListOf<String>()
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        // Android 13+ usa READ_MEDIA_IMAGES
                        // permissionLauncher puede pedir ambos si se quiere soportar API > 33
                        if (perms.isNotEmpty()) {
                            permissionLauncher.launch(perms.toTypedArray())
                        }
                        openGallery()
                    }
                }
            }
            .show()
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(takePictureIntent)
        } else {
            Toast.makeText(this, "No hay app de cámara disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 } // Reducción 50%
            val input: InputStream? = contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(input, null, options)
            input?.close()
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun handleImageBitmap(bitmap: Bitmap) {
        ivPreview.setImageBitmap(bitmap)

        // Convertir a Base64 reducido
        val base64 = bitmapToBase64(bitmap)
        imageBase64 = base64

        // Mostrar en campo de texto
        edtImagenUrl.setText("data:image/jpeg;base64,$base64")
    }

    // Convierte Bitmap a Base64 con reducción automática de calidad y tamaño
    private fun bitmapToBase64(bitmap: Bitmap): String {
        // Reducir resolución antes de comprimir
        val resizedBitmap = resizeBitmap(bitmap, 800, 800) // Máximo 800px
        val baos = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos) // Calidad 70%
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratioBitmap = bitmap.width.toFloat() / bitmap.height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var width = maxWidth
        var height = maxHeight

        if (ratioMax > 1) {
            width = (maxHeight * ratioBitmap).toInt()
        } else {
            height = (maxWidth / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val decoded = Base64.decode(base64, Base64.DEFAULT)
            val options = BitmapFactory.Options().apply { inSampleSize = 2 } // Escalado seguro
            BitmapFactory.decodeByteArray(decoded, 0, decoded.size, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    // ============================
    //       UBICACIÓN + MAPA
    // ============================
    private fun obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
            return
        }

        fusedLocation.lastLocation.addOnSuccessListener { location ->
            if (location != null) {

                latitud = location.latitude
                longitud = location.longitude

                tvUbicacion.text = "Ubicación: $latitud, $longitud"

                actualizarMapa(latitud!!, longitud!!)
            }
            else {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dibuja o mueve el marcador
    private fun actualizarMapa(lat: Double, lon: Double) {
        val geoPoint = GeoPoint(lat, lon)

        if (marker == null) {
            marker = Marker(mapView)
            marker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
        }

        marker!!.position = geoPoint
        marker!!.title = "Ubicación seleccionada"

        // Ajusta el zoom a un valor mayor para 'acercar' (ej. 18.5 o 19)
        val desiredZoom = 18.5
        mapView.controller.setZoom(desiredZoom)
        mapView.controller.animateTo(geoPoint) // centrado y animado
        mapView.invalidate()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_LOCATION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            obtenerUbicacion()
        }
    }

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            val v = currentFocus
            if (v != null) imm.hideSoftInputFromWindow(v.windowToken, 0)
        } catch (_: Exception) {
        }
    }
}