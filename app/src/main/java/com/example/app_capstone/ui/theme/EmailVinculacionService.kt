package com.example.app_capstone

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object EmailVinculacionService {
    private const val TAG = "EmailVinculacionService"
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Configuración DIRECTA de EmailJS (sin secrets)
    private const val SERVICE_ID = "service_2sho7jq"
    private const val TEMPLATE_ID = "template_x3vm7wk"
    private const val PUBLIC_KEY = "7ZYSnq954YIN048ah"
    private const val PRIVATE_KEY = "8CEgfONmiYW-jbTe1JnXT"

    fun enviarEmailVinculacion(
        toEmail: String,
        nombre: String,
        apellido: String,
        vinculacionUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Parámetros EXACTOS que espera tu template
            val templateParams = JSONObject().apply {
                put("to_name", "Dr. $nombre $apellido")
                put("to_email", toEmail)
                put("doctor_name", nombre)
                put("doctor_lastname", apellido)
                put("vinculacion_url", vinculacionUrl)
            }

            val jsonBody = JSONObject().apply {
                put("service_id", SERVICE_ID)
                put("template_id", TEMPLATE_ID)
                put("user_id", PUBLIC_KEY)
                put("template_params", templateParams)
                put("accessToken", PRIVATE_KEY)
            }

            val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)

            // Headers para evitar bloqueos
            val request = Request.Builder()
                .url("https://api.emailjs.com/api/v1.0/email/send")
                .post(body)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .header("Origin", "https://emailjs.com")
                .build()

            Log.d(TAG, "📧 Enviando email a: $toEmail")
            Log.d(TAG, "👨‍⚕️ Doctor: Dr. $nombre $apellido")
            Log.d(TAG, "🔗 URL: $vinculacionUrl")

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "❌ Error de conexión: ${e.message}")
                    onError("Error de conexión: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "📥 Response code: ${response.code}")
                    Log.d(TAG, "📥 Response body: $responseBody")

                    if (response.isSuccessful) {
                        Log.d(TAG, "✅ Email enviado exitosamente")
                        onSuccess()
                    } else {
                        Log.e(TAG, "❌ Error ${response.code}: $responseBody")

                        // Mensaje de error específico
                        val errorMessage = when (response.code) {
                            403 -> "Acceso denegado por EmailJS"
                            400 -> "Solicitud incorrecta"
                            429 -> "Límite de envíos excedido"
                            else -> "Error del servicio: ${response.code}"
                        }
                        onError(errorMessage)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            onError("Error: ${e.message}")
        }
    }
}