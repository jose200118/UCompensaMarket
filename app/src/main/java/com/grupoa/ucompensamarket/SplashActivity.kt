package com.grupoa.ucompensamarket

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    private val SPLASH_DURATION: Long = 3000 // 3000 ms = 3 segundos

    private  lateinit var firebaseAuth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        firebaseAuth = FirebaseAuth.getInstance()
        // Usamos Handler para el temporizador
        Handler(Looper.getMainLooper()).postDelayed({
            // Navegar a mensaje de bienvenida en caso de no estar logeado
            if(firebaseAuth.currentUser == null) {
                // Crea un Intent para pasar a la Activity de Bienvenida (MensajeBienvenidaActivity)
                val intent = Intent(this, MensajeBienvenidaActivity::class.java)
                startActivity(intent)
            }

            // Navegar al inicio
            comprobarSesion()
        }, SPLASH_DURATION)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun comprobarSesion() {
        if(firebaseAuth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}