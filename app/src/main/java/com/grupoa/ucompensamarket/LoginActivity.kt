package com.grupoa.ucompensamarket

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.grupoa.ucompensamarket.Fragmentos.FragmentPerfil
import com.grupoa.ucompensamarket.databinding.ActivityLoginBinding
import com.grupoa.ucompensamarket.databinding.ActivityMainBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //  Botón de inicio de sesion
        binding.btnLoginSubmit.setOnClickListener {
            iniciarSesionYNavegarAPerfil();
        }

        // Botón de view registro
        binding.btnRegisterLinkLogin.setOnClickListener {
            startActivity(Intent(applicationContext, RegistroCuentaActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    private fun iniciarSesionYNavegarAPerfil() {
        // 1. Crear el Intent para ir a la Activity Principal (ej: MainActivity)
        val intent = Intent(this, MainActivity::class.java)
        // Opcional: Si quieres pasar a un Fragment específico (como el Perfil) al iniciar MainActivity,
        // puedes usar 'Extras' y manejar esta lógica en el onCreate de MainActivity.
        intent.putExtra("NAVIGATE_TO", "PROFILE")

        // 2. Iniciar la nueva Activity
        startActivity(intent)
        // 3. Destruir la vista actual (LoginActivity).
        // Con esto, el usuario no podrá volver al Login con el botón 'Atrás'.
        finish()
    }
}