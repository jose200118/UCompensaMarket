package com.grupoa.ucompensamarket

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import com.grupoa.ucompensamarket.databinding.ActivityMensajeBienvenidaBinding

class MensajeBienvenidaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMensajeBienvenidaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMensajeBienvenidaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Botón de registro
        binding.registerButton.setOnClickListener {
            startActivity(Intent(applicationContext, RegistroCuentaActivity::class.java))
            finish()
        }

        // Enlace "Inicia sesión"
        binding.loginLink.setOnClickListener {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        }
    }
}