package com.example.app_capstone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.app_capstone.R

class MainActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvFullName: TextView
    private lateinit var tvColegiatura: TextView
    private lateinit var tvEspecialidad: TextView
    private lateinit var tvPrecio: TextView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvWelcome = findViewById(R.id.tvWelcome)
        tvFullName = findViewById(R.id.tvFullName)
        tvColegiatura = findViewById(R.id.tvColegiatura)
        tvEspecialidad = findViewById(R.id.tvEspecialidad)
        tvPrecio = findViewById(R.id.tvPrecio)
        btnLogout = findViewById(R.id.btnLogout)

        // Obtener datos del SharedPreferences
        val sharedPref = getSharedPreferences("ConsultasPeru", MODE_PRIVATE)
        val name = sharedPref.getString("name", "Doctor")
        val lastName = sharedPref.getString("lastName", "")
        val colegiatura = sharedPref.getString("colegiatura", "No definido")
        val especialidad = sharedPref.getString("especialidad", "No definida")
        val precio = sharedPref.getFloat("precio", 0f)

        // Mostrar en pantalla
        tvWelcome.text = "Bienvenido, $name"
        tvFullName.text = "Dr(a). $name $lastName"
        tvColegiatura.text = "Colegiatura: $colegiatura"
        tvEspecialidad.text = "Especialidad: $especialidad"
        tvPrecio.text = "Precio: S/ ${"%.2f".format(precio)}"

        // Cerrar sesión
        btnLogout.setOnClickListener {
            // Borrar sesión
            val editor = sharedPref.edit()
            editor.clear()
            editor.apply()

            // Volver al login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
