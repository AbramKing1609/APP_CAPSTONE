package com.example.app_capstone

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app_capstone.R
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    // TextInputLayout
    private lateinit var tilName: TextInputLayout
    private lateinit var tilLastName: TextInputLayout
    private lateinit var tilColegiatura: TextInputLayout
    private lateinit var tilEspecialidad: TextInputLayout
    private lateinit var tilPrecio: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    // EditText
    private lateinit var etName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etColegiatura: EditText
    private lateinit var etEspecialidad: EditText
    private lateinit var etPrecio: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText

    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

        // Inicializar TextInputLayout
        tilName = findViewById(R.id.tilName)
        tilLastName = findViewById(R.id.tilLastName)
        tilColegiatura = findViewById(R.id.tilColegiatura)
        tilEspecialidad = findViewById(R.id.tilEspecialidad)
        tilPrecio = findViewById(R.id.tilPrecio)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)

        // Inicializar EditText
        etName = findViewById(R.id.etName)
        etLastName = findViewById(R.id.etLastName)
        etColegiatura = findViewById(R.id.etColegiatura)
        etEspecialidad = findViewById(R.id.etEspecialidad)
        etPrecio = findViewById(R.id.etPrecio)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        // Botón
        btnRegister = findViewById(R.id.btnRegister)

        btnRegister.setOnClickListener {
            if (validateForm()) {
                registerDoctor()
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Nombre
        if (etName.text.isNullOrEmpty()) {
            tilName.error = "El nombre es obligatorio"
            isValid = false
        } else tilName.error = null

        // Apellido
        if (etLastName.text.isNullOrEmpty()) {
            tilLastName.error = "El apellido es obligatorio"
            isValid = false
        } else tilLastName.error = null

        // Colegiatura
        if (etColegiatura.text.isNullOrEmpty()) {
            tilColegiatura.error = "La colegiatura es obligatoria"
            isValid = false
        } else tilColegiatura.error = null

        // Especialidad
        if (etEspecialidad.text.isNullOrEmpty()) {
            tilEspecialidad.error = "La especialidad es obligatoria"
            isValid = false
        } else tilEspecialidad.error = null

        // Precio
        if (etPrecio.text.isNullOrEmpty()) {
            tilPrecio.error = "El precio es obligatorio"
            isValid = false
        } else tilPrecio.error = null

        // Contraseña
        val password = etPassword.text.toString()
        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es obligatoria"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Debe tener al menos 6 caracteres"
            isValid = false
        } else tilPassword.error = null

        // Confirmar contraseña
        val confirmPassword = etConfirmPassword.text.toString()
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Confirme su contraseña"
            isValid = false
        } else if (password != confirmPassword) {
            tilConfirmPassword.error = "Las contraseñas no coinciden"
            isValid = false
        } else tilConfirmPassword.error = null

        return isValid
    }

    private fun registerDoctor() {
        val name = etName.text.toString()
        val lastName = etLastName.text.toString()
        val colegiatura = etColegiatura.text.toString()
        val especialidad = etEspecialidad.text.toString()
        val precio = etPrecio.text.toString().toFloatOrNull() ?: 0f
        val password = etPassword.text.toString()

        // 👉 Usaremos la colegiatura como "email"
        val email = "$colegiatura@consultasperu.com"

        // 1. Registrar en Firebase Authentication
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: return@addOnCompleteListener

                    // 2. Guardar datos en Firestore
                    val db = FirebaseFirestore.getInstance()
                    val doctor = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "lastName" to lastName,
                        "colegiatura" to colegiatura,
                        "especialidad" to especialidad,
                        "precio" to precio
                    )

                    db.collection("doctores").document(uid)
                        .set(doctor)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Doctor registrado en Firebase ✅", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error guardando datos: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Error en registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }