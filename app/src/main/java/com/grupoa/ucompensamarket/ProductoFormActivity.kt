package com.grupoa.ucompensamarket

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
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
import androidx.core.content.ContextCompat
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
    private lateinit var ivPreview: ImageView
    private lateinit var btnLocalizacion: Button
    private lateinit var btnGuardar: Button
    private lateinit var tvUbicacion: TextView
    private lateinit var btnSeleccionarImagen: ImageButton

    private lateinit var mapView: MapView
    private var marker: Marker? = null

    private lateinit var fusedLocation: FusedLocationProviderClient
    private var latitud: Double? = null
    private var longitud: Double? = null

    // Activity result launchers
    private lateinit var takePicturePreviewLauncher: ActivityResultLauncher<Void?>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    // Permisos: launchers dedicados
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private var notificationPermissionLauncher: ActivityResultLauncher<String>? = null

    private var imageBase64: String? = null

    private val dbRef by lazy { FirebaseDatabase.getInstance().getReference("Productos") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMdroid config (si lo usas)
        Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        setContentView(R.layout.activity_producto_form)

        // Inicializar views
        edtNombre = findViewById(R.id.edtNombre)
        edtDescripcion = findViewById(R.id.edtDescripcion)
        edtPrecio = findViewById(R.id.edtPrecio)
        ivPreview = findViewById(R.id.ivPreview)
        btnLocalizacion = findViewById(R.id.btnObtenerUbicacion)
        btnGuardar = findViewById(R.id.btnGuardarProducto)
        tvUbicacion = findViewById(R.id.tvUbicacion)
        btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen)

        mapView = findViewById(R.id.mapView)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(16.0)
        mapView.controller.setCenter(GeoPoint(4.60971, -74.08175))

        fusedLocation = LocationServices.getFusedLocationProviderClient(this)

        initActivityResultLaunchers()

        // Cargar datos si vienen por intent
        loadIntentData()

        btnSeleccionarImagen.setOnClickListener {
            showImagePickerDialog()
        }

        btnGuardar.setOnClickListener {
            val uid = intent.getStringExtra(EXTRA_UID)
            val isEdit = !uid.isNullOrEmpty()
            guardarProducto(isEdit, uid)
        }

        btnLocalizacion.setOnClickListener {
            obtenerUbicacion()
        }
    }

    private fun initActivityResultLaunchers() {
        // Lanzador para tomar foto (devuelve Bitmap con TakePicturePreview)
        takePicturePreviewLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                handleImageBitmap(bitmap)
            } else {
                Toast.makeText(this, "No se obtuvo imagen de la cámara", Toast.LENGTH_SHORT).show()
            }
        }

        // Lanzador para elegir imagen (GetContent)
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                decodeUriToBitmap(it)?.let { bmp -> handleImageBitmap(bmp) }
                    ?: Toast.makeText(this, "No se pudo leer la imagen", Toast.LENGTH_SHORT).show()
            }
        }

        // Permiso cámara: si concedido, abrimos cámara
        cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val perm = Manifest.permission.CAMERA
            if (granted) {
                openCamera()
            } else {
                // Denegado
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                    showPermissionDeniedDialog("cámara")
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Permiso ubicación: si concedido, reintentar obtener ubicación
        locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val perm = Manifest.permission.ACCESS_FINE_LOCATION
            if (granted) {
                obtenerUbicacion()
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                    showPermissionDeniedDialog("ubicación")
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Permiso notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                val perm = Manifest.permission.POST_NOTIFICATIONS
                if (!granted) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                        showPermissionDeniedDialog("notificaciones")
                    } else {
                        Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun loadIntentData() {
        val uid = intent.getStringExtra(EXTRA_UID)
        val nombre = intent.getStringExtra(EXTRA_NOMBRE)
        val descripcion = intent.getStringExtra(EXTRA_DESCRIPCION)
        val precio = intent.getDoubleExtra(EXTRA_PRECIO, 0.0)
        val imagenUrl = intent.getStringExtra(EXTRA_IMAGENURL)
        val latitudU = intent.getStringExtra(EXTRA_LAT)
        val longitudU = intent.getStringExtra(EXTRA_LNG)

        val isEdit = !uid.isNullOrEmpty()

        if (isEdit) {
            edtNombre.setText(nombre)
            edtDescripcion.setText(descripcion)
            if (precio != 0.0) edtPrecio.setText(precio.toString())
            imagenUrl?.let { tryLoadImage(it) }
            if (!latitudU.isNullOrEmpty() && !longitudU.isNullOrEmpty()) {
                latitud = latitudU.toDoubleOrNull()
                longitud = longitudU.toDoubleOrNull()
                if (latitud != null && longitud != null) {
                    tvUbicacion.text = "Ubicación: $latitud, $longitud"
                    actualizarMapa(latitud!!, longitud!!)
                }
            }
            btnGuardar.text = "Actualizar"
        } else {
            btnGuardar.text = "Crear"
        }
    }

    private fun tryLoadImage(imagenUrl: String) {
        if (imagenUrl.startsWith("data:image", ignoreCase = true)) {
            val base64Part = imagenUrl.substringAfter(",")
            base64ToBitmap(base64Part)?.let {
                ivPreview.setImageBitmap(it)
                imageBase64 = base64Part
            }
        } else if (imagenUrl.length > 1000 && !imagenUrl.contains("http", true)) {
            base64ToBitmap(imagenUrl)?.let {
                ivPreview.setImageBitmap(it)
                imageBase64 = imagenUrl
            } ?: Glide.with(this).load(imagenUrl).into(ivPreview)
        } else {
            Glide.with(this).load(imagenUrl).into(ivPreview)
        }
    }

    private fun showImagePickerDialog() {
        val items = arrayOf("Tomar foto", "Seleccionar de la galería")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> requestCameraPermissionThenOpen()
                    1 -> requestGalleryPermissionThenOpen()
                }
            }
            .show()
    }

    // ------- PERMISOS y ABRIR CÁMARA / GALERÍA -------

    private fun requestCameraPermissionThenOpen() {
        val perm = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
            return
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
            // Explicar y pedir
            AlertDialog.Builder(this)
                .setTitle("Permiso cámara requerido")
                .setMessage("La app necesita permiso de cámara para tomar fotos.")
                .setPositiveButton("Permitir") { _, _ ->
                    cameraPermissionLauncher.launch(perm)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            // Primer pedido
            cameraPermissionLauncher.launch(perm)
        }
    }

    private fun requestGalleryPermissionThenOpen() {
        // NOTA: usamos ActivityResultContracts.GetContent() (pickImageLauncher) que normalmente NO necesita permiso
        // en Android modernos. Si tu targetSdk < cierto nivel y detectas problemas en algunos dispositivos, podrías
        // pedir READ_EXTERNAL_STORAGE en tiempo de ejecución, pero en general GetContent funciona sin pedir permiso.
        openGallery()
    }

    private fun showPermissionDeniedDialog(feature: String) {
        AlertDialog.Builder(this)
            .setTitle("Permiso requerido")
            .setMessage("Has denegado permanentemente el permiso para $feature. Habilítalo manualmente en Ajustes → Aplicaciones → Permisos.")
            .setPositiveButton("Abrir ajustes") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openCamera() {
        takePicturePreviewLauncher.launch(null)
    }

    private fun openGallery() {
        // Usa GetContent (pickImageLauncher)
        pickImageLauncher.launch("image/*")
    }

    // ------- Decodificar y manejar imagenes -------

    private fun decodeUriToBitmap(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                // decodificamos con inSampleSize para evitar OOM
                val options = BitmapFactory.Options().apply { inSampleSize = calculateInSampleSize(stream, 1024, 1024) }
                // Tenemos que volver a abrir el stream, porque se consumió
                contentResolver.openInputStream(uri)?.use { stream2 ->
                    BitmapFactory.decodeStream(stream2, null, options)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(stream: InputStream, reqWidth: Int, reqHeight: Int): Int {
        // lee solo bounds
        return try {
            stream.mark(stream.available())
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, options)
            stream.reset()
            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            inSampleSize
        } catch (_: Exception) {
            4
        }
    }

    private fun handleImageBitmap(bitmap: Bitmap) {
        ivPreview.setImageBitmap(bitmap)
        imageBase64 = bitmapToBase64(bitmap)
    }

    // Redimensiona, comprime y devuelve Base64 limitado
    private fun bitmapToBase64(bitmap: Bitmap): String {
        // Redimensionar manteniendo proporción — ancho objetivo 800px
        val maxWidth = 800
        val (newW, newH) = if (bitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / bitmap.width
            Pair(maxWidth, (bitmap.height * ratio).toInt())
        } else {
            Pair(bitmap.width, bitmap.height)
        }

        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        val baos = ByteArrayOutputStream()
        var quality = 75
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)

        // Limitar ~300KB
        while (baos.size() > 300_000 && quality > 30) {
            baos.reset()
            quality -= 10
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        }
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    private fun base64ToBitmap(base64: String): Bitmap? =
        try {
            val decoded = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    // ------- Ubicación y mapa -------

    private fun obtenerUbicacion() {
        val perm = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            // pedir permiso de ubicación mediante el launcher ya registrado
            locationPermissionLauncher.launch(perm)
            return
        }

        fusedLocation.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    latitud = location.latitude
                    longitud = location.longitude
                    tvUbicacion.text = "Ubicación: $latitud, $longitud"
                    actualizarMapa(latitud!!, longitud!!)
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarMapa(lat: Double, lon: Double) {
        val geoPoint = GeoPoint(lat, lon)
        if (marker == null) {
            marker = Marker(mapView)
            marker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
        }
        marker!!.position = geoPoint
        mapView.controller.setZoom(18.5)
        mapView.controller.animateTo(geoPoint)
        mapView.invalidate()
    }

    // ------- Guardar producto -------

    private fun guardarProducto(isEdit: Boolean, uid: String?) {
        hideKeyboard()

        val nombre = edtNombre.text.toString().trim()
        val descripcion = edtDescripcion.text.toString().trim()
        val precioTxt = edtPrecio.text.toString().trim()

        if (TextUtils.isEmpty(nombre)) {
            edtNombre.error = "Ingrese nombre"
            return
        }
        val precio = precioTxt.toDoubleOrNull()
        if (precio == null) {
            edtPrecio.error = "Precio inválido"
            return
        }

        val imagenParaGuardar = if (!imageBase64.isNullOrEmpty()) "data:image/jpeg;base64,$imageBase64" else null

        val key = uid ?: dbRef.push().key
        val producto = Productos(key, nombre, descripcion, precio, imagenParaGuardar, latitud, longitud)
        dbRef.child(key!!).setValue(producto)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto guardado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
        } catch (_: Exception) {}
    }
}