package com.grupoa.ucompensamarket

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.grupoa.ucompensamarket.databinding.ActivityRegistroCuentaBinding

class RegistroCuentaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroCuentaBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegistroCuentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        // Registar usuario
        binding.btnRegisterSubmit.setOnClickListener {
            validarInformacion()
        }


        // Botón de view inicio de sesion
        binding.btnLoginLinkRegistro.setOnClickListener {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    var nombre = ""
    var correo = ""
    var password = ""
    var politica = false

    private fun validarInformacion() {
        nombre = "";
        correo = binding.inputEmail.text.toString().trim()
        password = binding.inputPassword.text.toString().trim()
        politica = binding.checkboxPrivacy.isChecked

       if(correo.isEmpty()) {
            binding.inputEmail.error = "El campo es requerido"
            binding.inputEmail.requestFocus()
       }
       else if(!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.inputEmail.error = "Correo electronico es invalido"
            binding.inputEmail.requestFocus()
       }
       else if(password.isEmpty()) {
            binding.inputPassword.error = "El campo es requerido"
            binding.inputPassword.requestFocus()
       }
       else if(!politica){
           binding.checkboxPrivacy.error = "Por favor, acepte la política"
           binding.checkboxPrivacy.requestFocus()
       }
       else {
           registarUsuario()
       }
    }

    private fun registarUsuario() {
        progressDialog.setMessage("Creando cuenta")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(correo, password)
            .addOnSuccessListener {
                actualizarInformacion()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Fallo la creacion de la cuenta debido a ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun actualizarInformacion() {
        progressDialog.setMessage("Guardando información")

        val uidU = firebaseAuth.uid
        val nombreU = nombre
        val correoU = firebaseAuth.currentUser!!.email
        val tiempoU = Constantes.obteneTiempoD()
        val politicaU = politica

        val datosUsuario = HashMap<String, Any>()

        datosUsuario["uid"] = "$uidU"
        datosUsuario["nombres"] = "$nombreU"
        datosUsuario["email"] = "$correoU"
        datosUsuario["telefono"] = ""
        datosUsuario["imagen"] = ""
        datosUsuario["politica"] = "$politicaU"
        datosUsuario["estado"] = "Online"
        datosUsuario["proveedor"] = "Correo"
        datosUsuario["tiempoR"] = "$tiempoU"

        val reference = FirebaseDatabase.getInstance().getReference("Usuarios")
        reference.child(uidU!!)
            .setValue(datosUsuario)
            .addOnSuccessListener {
                progressDialog.dismiss()
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Fallo la creacion de la cuenta debido a ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}