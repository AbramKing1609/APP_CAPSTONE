package com.example.app_capstone

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // 👈 Muestra tu layout del splash

        // 🔹 Animación simple de aparición
        val logo = findViewById<ImageView>(R.id.logoImage)
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 1500
        logo.startAnimation(fadeIn)

        // 🔹 Verificar sesión activa en Firebase
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        Handler(Looper.getMainLooper()).postDelayed({
            if (currentUser != null) {
                // ✅ Usuario autenticado → ir al dashboard principal
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // ✅ Sin sesión → ir al menú principal con “Ingresar” y “Registrarme”
                startActivity(Intent(this, MainAppActivity::class.java))
            }
            finish()
        }, 1800) // ⏳ Espera 1.8 segundos
    }
}
