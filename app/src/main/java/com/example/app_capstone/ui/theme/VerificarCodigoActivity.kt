package com.example.app_capstone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class VerificarCodigoActivity : AppCompatActivity() {

    private lateinit var etCodigoVerificacion: EditText
    private lateinit var btnIngresarCodigo: Button
    private lateinit var tvReenviar: TextView
    private lateinit var db: FirebaseFirestore

    private var email = ""
    private var codigoCorrecto = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recuperar_cuenta2)

        email = intent.getStringExtra("email") ?: ""
        codigoCorrecto = intent.getStringExtra("codigo") ?: ""

        etCodigoVerificacion = findViewById(R.id.etCodigoVerificacion)
        btnIngresarCodigo = findViewById(R.id.btnIngresarCodigo)
        tvReenviar = findViewById(R.id.tvReenviar)

        db = FirebaseFirestore.getInstance()

        btnIngresarCodigo.setOnClickListener {
            verificarCodigo()
        }

        tvReenviar.setOnClickListener {
            reenviarCodigo()
        }
    }

    private fun verificarCodigo() {
        val codigoIngresado = etCodigoVerificacion.text.toString().trim()

        if (codigoIngresado.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese el código de verificación", Toast.LENGTH_SHORT).show()
            return
        }

        if (codigoIngresado.length != 6) {
            Toast.makeText(this, "El código debe tener 6 dígitos", Toast.LENGTH_SHORT).show()
            return
        }

        verificarCodigoEnFirestore(codigoIngresado)
    }

    private fun verificarCodigoEnFirestore(codigoIngresado: String) {
        db.collection("codigos_verificacion")
            .document(email)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val codigoGuardado = document.getString("codigo")
                    val timestamp = document.getLong("timestamp") ?: 0
                    val utilizado = document.getBoolean("utilizado") ?: false

                    val tiempoActual = System.currentTimeMillis()
                    val tiempoExpiracion = 10 * 60 * 1000

                    if (utilizado) {
                        Toast.makeText(this, "Este código ya fue utilizado", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    if (tiempoActual - timestamp > tiempoExpiracion) {
                        Toast.makeText(this, "El código ha expirado", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    if (codigoIngresado == codigoGuardado) {
                        marcarCodigoComoUtilizado()
                        Toast.makeText(this, "✅ Código verificado correctamente", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, NuevaContrasenaActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "❌ Código incorrecto", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "No se encontró código de verificación para este email", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al verificar el código: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun marcarCodigoComoUtilizado() {
        db.collection("codigos_verificacion")
            .document(email)
            .update("utilizado", true)
            .addOnFailureListener { e ->
                println("Error al marcar código como utilizado: ${e.message}")
            }
    }

    private fun reenviarCodigo() {
        val nuevoCodigo = (100000..999999).random().toString()

        val codigoData = hashMapOf(
            "codigo" to nuevoCodigo,
            "timestamp" to System.currentTimeMillis(),
            "email" to email,
            "utilizado" to false
        )

        db.collection("codigos_verificacion")
            .document(email)
            .set(codigoData)
            .addOnSuccessListener {
                enviarNuevoCodigoEmail(nuevoCodigo)
                codigoCorrecto = nuevoCodigo
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al reenviar código: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun enviarNuevoCodigoEmail(codigo: String) {
        println("DEBUG Reenviar: Usando EmailServiceSimple para reenviar código...")

        EmailServiceSimple.sendEmail(
            toEmail = email,
            codigo = codigo,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this@VerificarCodigoActivity, "✅ Nuevo código enviado", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this@VerificarCodigoActivity, "❌ $error", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}