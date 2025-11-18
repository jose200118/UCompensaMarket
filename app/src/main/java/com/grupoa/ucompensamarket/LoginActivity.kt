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
import com.grupoa.ucompensamarket.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        //  Botón de inicio de sesion
        binding.btnLoginSubmit.setOnClickListener {
            validarInformacion();
        }

        // Botón de view registro
        binding.btnRegisterLinkLogin.setOnClickListener {
            startActivity(Intent(applicationContext, RegistroCuentaActivity::class.java))
            finishAffinity()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private var correo = ""
    private var password = ""
    private fun validarInformacion() {
        correo = binding.inputEmailLogin.text.toString().trim()
        password = binding.inputPasswordLogin.text.toString().trim()
        if(correo.isEmpty()) {
            binding.inputEmailLogin.error = "El campo es requerido"
            binding.inputEmailLogin.requestFocus()
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.inputEmailLogin.error = "Correo electronico es invalido"
            binding.inputEmailLogin.requestFocus()
        }
        else if(password.isEmpty()) {
            binding.inputPasswordLogin.error = "El campo es requerido"
            binding.inputPasswordLogin.requestFocus()
        }
        else {
            logearUsuario()
        }
    }

    private fun logearUsuario() {
        progressDialog.setMessage("Ingresando")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(correo, password)
            .addOnSuccessListener {

                val uid = firebaseAuth.currentUser!!.uid
                val db = FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
                db.child("rol").get().addOnSuccessListener { snapshot -> val rolDelUsuario = snapshot.value?.toString() ?: ""
                    val prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                    prefs.edit().putString("ROL_USUARIO", rolDelUsuario).apply()
                }

                progressDialog.dismiss()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "No se realizo el logue dedido a ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}