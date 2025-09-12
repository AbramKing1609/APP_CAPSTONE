package com.example.app_capstone

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app_capstone.MainActivity
import com.example.app_capstone.RegisterActivity
import com.example.app_capstone.R
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoRegister: TextView

    // Instancia de FirebaseAuth para la autenticación
    private lateinit var auth: FirebaseAuth
    // Instancia de FirebaseFirestore para la base de datos
    private lateinit var db: FirebaseFirestore

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

        // Inicializar la instancia de Firebase Auth y Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnLogin.setOnClickListener {
            checkCredentials()
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun checkCredentials() {
        val colegiatura = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validar los campos de entrada
        if (colegiatura.isEmpty()) {
            tilUsername.error = "El número de colegiatura es obligatorio"
            return
        } else {
            tilUsername.error = null
        }

        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es obligatoria"
            return
        } else {
            tilPassword.error = null
        }

        // Primero, se debe buscar el documento del doctor por su número de colegiatura.
        db.collection("medicos")
            .whereEqualTo("colegiatura", colegiatura)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // No se encontró un doctor con esa colegiatura
                    Toast.makeText(this, "Número de colegiatura no encontrado", Toast.LENGTH_SHORT).show()
                } else {
                    // Se encontró un doctor, se obtiene el primer documento (asumiendo que es único)
                    val document = querySnapshot.documents[0]
                    val email = document.getString("email")

                    if (email != null) {
                        // Se utiliza el correo electrónico obtenido para la autenticación
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    // Inicio de sesión exitoso, se procede a guardar los datos en SharedPreferences y navegar a la siguiente actividad
                                    val sharedPref = getSharedPreferences("ConsultasPeru", Context.MODE_PRIVATE)
                                    with(sharedPref.edit()) {
                                        // Guardar todos los datos del doctor en SharedPreferences
                                        putString("name", document.getString("name"))
                                        putString("lastName", document.getString("lastName"))
                                        putString("email", document.getString("email"))
                                        putString("dni", document.getString("dni"))
                                        putString("celular", document.getString("celular"))
                                        putString("age", document.get("age")?.toString())
                                        putString("city", document.getString("city"))
                                        putString("nationality", document.getString("nationality"))
                                        putString("especialidad", document.getString("especialidad"))
                                        putString("colegiatura", document.getString("colegiatura"))
                                        putString("university", document.getString("university"))
                                        putString("experienceYears", document.get("experienceYears")?.toString())
                                        putString("hospital", document.getString("hospital"))
                                        putString("additionalInfo", document.getString("additionalInfo"))
                                        putString("profileImageUrl", document.getString("profileImageUrl"))
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
                                    // Fallo en la autenticación con la contraseña
                                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                                    val errorMessage = when (task.exception) {
                                        is FirebaseAuthInvalidCredentialsException -> "Contraseña incorrecta"
                                        is FirebaseAuthInvalidUserException -> "Usuario no válido"
                                        else -> "Error de autenticación: ${task.exception?.message}"
                                    }
                                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Error: el correo electrónico del doctor no se encontró.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                // Error al buscar en Firestore
                Log.e("LoginActivity", "Error al buscar el documento en Firestore", e)
                Toast.makeText(this, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
