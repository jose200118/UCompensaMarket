package com.grupoa.ucompensamarket

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.os.Handler
import android.os.Looper

class SplashActivity : AppCompatActivity() {
    private val SPLASH_DURATION: Long = 3000 // 3000 ms = 3 segundos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Usamos Handler para el temporizador
        Handler(Looper.getMainLooper()).postDelayed({
            // Crea un Intent para pasar a la Activity de Bienvenida (MensajeBienvenidaActivity)
            val intent = Intent(this, MensajeBienvenidaActivity::class.java)
            startActivity(intent)

            // **LA CLAVE:** Finaliza esta actividad.
            // Esto asegura que al presionar 'Atrás' desde la siguiente pantalla,
            // el usuario no vuelva a la Splash Screen.
            finish()
        }, SPLASH_DURATION)
    }
}