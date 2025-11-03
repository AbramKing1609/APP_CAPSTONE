package com.example.app_capstone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType

class NuevaContrasenaActivity : AppCompatActivity() {

    private lateinit var etCrearContraseña: EditText
    private lateinit var etConfirmarContraseñaNueva: EditText
    private lateinit var btnCrear: Button
    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recuperar_cuenta3)

        etCrearContraseña = findViewById(R.id.etCrearContraseña)
        etConfirmarContraseñaNueva = findViewById(R.id.etConfirmarContraseñaNueva)
        btnCrear = findViewById(R.id.btnEnviar)

        email = intent.getStringExtra("email") ?: ""

        btnCrear.setOnClickListener {
            cambiarContrasena()
        }
    }

    private fun cambiarContrasena() {
        val nuevaPass = etCrearContraseña.text.toString().trim()
        val confirmarPass = etConfirmarContraseñaNueva.text.toString().trim()

        if (nuevaPass.isEmpty() || confirmarPass.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (nuevaPass != confirmarPass) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        btnCrear.isEnabled = false
        btnCrear.text = "Actualizando..."

        val url = "https://us-central1-consultasperu-262df.cloudfunctions.net/cambiarContrasena"

        val json = JSONObject().apply {
            put("email", email)
            put("nuevaContrasena", nuevaPass)
        }

        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = RequestBody.create(mediaType, json.toString())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    btnCrear.isEnabled = true
                    btnCrear.text = "Actualizar"
                    Toast.makeText(this@NuevaContrasenaActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    btnCrear.isEnabled = true
                    btnCrear.text = "Actualizar"
                    if (response.isSuccessful) {
                        Toast.makeText(this@NuevaContrasenaActivity, "✅ Contraseña actualizada correctamente", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@NuevaContrasenaActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@NuevaContrasenaActivity, "Error: $responseBody", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
