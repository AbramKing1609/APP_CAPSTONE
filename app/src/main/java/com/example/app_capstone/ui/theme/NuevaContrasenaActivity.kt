package com.example.app_capstone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NuevaContrasenaActivity : AppCompatActivity() {

    private lateinit var etCrearContrasena: EditText
    private lateinit var etConfirmarContrasenaNueva: EditText
    private lateinit var btnEnviar: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var email = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recuperar_cuenta3)

        email = intent.getStringExtra("email") ?: ""

        etCrearContrasena = findViewById(R.id.etCrearContraseña)
        etConfirmarContrasenaNueva = findViewById(R.id.etConfirmarContraseñaNueva)
        btnEnviar = findViewById(R.id.btnEnviar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etCrearContrasena.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        etConfirmarContrasenaNueva.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        btnEnviar.setOnClickListener {
            cambiarContrasena()
        }
    }

    private fun cambiarContrasena() {
        val nuevaContrasena = etCrearContrasena.text.toString().trim()
        val confirmarContrasena = etConfirmarContrasenaNueva.text.toString().trim()

        if (nuevaContrasena.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese la nueva contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        if (confirmarContrasena.isEmpty()) {
            Toast.makeText(this, "Por favor confirme la contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        if (nuevaContrasena != confirmarContrasena) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        if (nuevaContrasena.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        btnEnviar.isEnabled = false
        btnEnviar.text = "Actualizando..."

        actualizarContrasenaEnFirestore(nuevaContrasena)
    }

    private fun actualizarContrasenaEnFirestore(nuevaContrasena: String) {
        db.collection("medicos")
            .whereEqualTo("CORREO", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val medicoId = document.id

                    db.collection("medicos")
                        .document(medicoId)
                        .update("CONTRASENA", nuevaContrasena)
                        .addOnSuccessListener {
                            actualizarContrasenaEnFirebaseAuth(nuevaContrasena)
                        }
                        .addOnFailureListener { e ->
                            manejarErrorCambioContrasena(e)
                        }
                } else {
                    btnEnviar.isEnabled = true
                    btnEnviar.text = "Crear"
                    Toast.makeText(
                        this,
                        "Error: No se encontró el usuario en la base de datos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                manejarErrorCambioContrasena(e)
            }
    }

    private fun actualizarContrasenaEnFirebaseAuth(nuevaContrasena: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                btnEnviar.isEnabled = true
                btnEnviar.text = "Crear"

                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "✅ Contraseña actualizada exitosamente. Se ha enviado un email de confirmación.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "✅ Contraseña actualizada en el sistema.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
    }

    private fun manejarErrorCambioContrasena(exception: Exception?) {
        btnEnviar.isEnabled = true
        btnEnviar.text = "Crear"

        val errorMessage = when {
            exception?.message?.contains("network", ignoreCase = true) == true ->
                "Error de conexión. Verifique su internet"
            exception?.message?.contains("user not found", ignoreCase = true) == true ->
                "Usuario no encontrado"
            else ->
                "Error al cambiar contraseña: ${exception?.message ?: "Error desconocido"}"
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }
}