package com.grupoa.ucompensamarket

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.grupoa.ucompensamarket.Fragmentos.FragmentPerfil
import com.grupoa.ucompensamarket.Fragmentos.FragmentProductos
import com.grupoa.ucompensamarket.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private  lateinit var  binding: ActivityMainBinding
    private  lateinit var firebaseAuth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // Desactivar el modo oscuro
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        if(firebaseAuth.currentUser == null) {
            irOpcionesLogin()
        }

        // Fragmento (view) por defecto
        verFragmentoProductos()

        binding.bottomNV.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_productos -> {
                    // Visualizar perfil
                    verFragmentoProductos()
                    true
                }
                R.id.item_perfil -> {
                    // Visualizar perfil
                    verFragmentoPerfil()
                    true
                }
                else -> false
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun irOpcionesLogin() {
        startActivity(Intent(applicationContext, LoginActivity::class.java))
        finish()
    }

    private fun verFragmentoProductos(){
        binding.tvTitulo.text = "Productos"
        val fragment = FragmentProductos()
        var fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.framentoFL.id, fragment, "Fragment Productos")
        fragmentTransaction.commit()
    }

    private fun verFragmentoPerfil(){
        binding.tvTitulo.text = "Perfil"
        val fragment = FragmentPerfil()
        var fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.framentoFL.id, fragment, "Fragment Perfil")
        fragmentTransaction.commit()
    }
}