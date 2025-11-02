package com.example.app_capstone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RecuperarCuentaActivity : AppCompatActivity() {

    private lateinit var etCorreoElectronico: EditText
    private lateinit var btnEnviar: Button
    private lateinit var tvVolverAInicio: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var codigoVerificacion = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recuperar_cuenta1)

        etCorreoElectronico = findViewById(R.id.etCorreoElectronico)
        btnEnviar = findViewById(R.id.btnEnviar)
        tvVolverAInicio = findViewById(R.id.tvVolverAInicio)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnEnviar.setOnClickListener {
            enviarCodigoVerificacion()
        }

        tvVolverAInicio.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun enviarCodigoVerificacion() {
        val email = etCorreoElectronico.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese su correo electrónico", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Por favor ingrese un correo electrónico válido", Toast.LENGTH_SHORT).show()
            return
        }

        btnEnviar.isEnabled = false
        btnEnviar.text = "Verificando..."
        verificarCorreoEnMedicos(email)
    }

    private fun verificarCorreoEnMedicos(email: String) {
        db.collection("medicos")
            .whereEqualTo("CORREO", email)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val querySnapshot = task.result
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        generarYEnviarCodigo(email)
                    } else {
                        btnEnviar.isEnabled = true
                        btnEnviar.text = "Enviar"
                        Toast.makeText(
                            this,
                            "El correo electrónico no está registrado en nuestro sistema",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    btnEnviar.isEnabled = true
                    btnEnviar.text = "Enviar"
                    Toast.makeText(
                        this,
                        "Error al verificar el correo: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun generarYEnviarCodigo(email: String) {
        codigoVerificacion = (100000..999999).random().toString()
        guardarCodigoTemporal(email, codigoVerificacion)
        enviarEmailConEmailJS(email, codigoVerificacion)
    }

    private fun guardarCodigoTemporal(email: String, codigo: String) {
        val codigoData = hashMapOf(
            "codigo" to codigo,
            "timestamp" to System.currentTimeMillis(),
            "email" to email,
            "utilizado" to false
        )

        db.collection("codigos_verificacion")
            .document(email)
            .set(codigoData)
            .addOnSuccessListener {
                println("Código guardado en Firestore para: $email")
            }
            .addOnFailureListener { e ->
                println("Error al guardar código: ${e.message}")
            }
    }

    private fun enviarEmailConEmailJS(email: String, codigo: String) {
        println("DEBUG: Usando EmailServiceSimple para enviar código...")

        EmailServiceSimple.sendEmail(
            toEmail = email,
            codigo = codigo,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(
                        this@RecuperarCuentaActivity,
                        "✅ Código enviado a $email",
                        Toast.LENGTH_LONG
                    ).show()

                    val intentVerificacion = Intent(this@RecuperarCuentaActivity, VerificarCodigoActivity::class.java)
                    intentVerificacion.putExtra("email", email)
                    intentVerificacion.putExtra("codigo", codigoVerificacion)
                    startActivity(intentVerificacion)
                    finish()
                }
            },
            onError = { error ->
                runOnUiThread {
                    btnEnviar.isEnabled = true
                    btnEnviar.text = "Enviar"
                    Toast.makeText(
                        this@RecuperarCuentaActivity,
                        "❌ $error",
                        Toast.LENGTH_LONG
                    ).show()
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