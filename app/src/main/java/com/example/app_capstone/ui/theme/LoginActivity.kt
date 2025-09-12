package com.example.app_capstone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app_capstone.R
import com.google.android.material.textfield.TextInputLayout

// 👇 IMPORTA ESTAS LIBRERÍAS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        // Inicializar vistas
        tilUsername = findViewById(R.id.tilUsername)
        tilPassword = findViewById(R.id.tilPassword)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoRegister = findViewById(R.id.tvGoRegister)

        btnLogin.setOnClickListener {
            if (validateLogin()) {
                checkCredentials()
            }
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateLogin(): Boolean {
        var isValid = true

        if (etUsername.text.isNullOrEmpty()) {
            tilUsername.error = "El usuario es obligatorio"
            isValid = false
        } else tilUsername.error = null

        if (etPassword.text.isNullOrEmpty()) {
            tilPassword.error = "La contraseña es obligatoria"
            isValid = false
        } else tilPassword.error = null

        return isValid
    }

    private fun checkCredentials() {
        val colegiatura = etUsername.text.toString()
        val password = etPassword.text.toString()

        if (colegiatura.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val email = "$colegiatura@consultasperu.com"

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener

                    val db = FirebaseFirestore.getInstance()
                    db.collection("doctores").document(uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val sharedPref = getSharedPreferences("ConsultasPeru", MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putString("name", document.getString("name"))
                                    putString("lastName", document.getString("lastName"))
                                    putString("colegiatura", document.getString("colegiatura"))
                                    putString("especialidad", document.getString("especialidad"))
                                    putFloat(
                                        "precio",
                                        document.getDouble("precio")?.toFloat() ?: 0f
                                    )
                                    apply()
                                }

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Bienvenido ${document.getString("name")}",
                                    Toast.LENGTH_SHORT
                                ).show()

                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "No se encontraron datos en Firestore",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this@LoginActivity,
                                "Error al obtener datos: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
