package com.example.app_capstone

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object EmailServiceSimple {
    private const val TAG = "EmailServiceSimple"
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    fun sendEmail(
        toEmail: String,
        codigo: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val jsonBody = JSONObject().apply {
                put("service_id", "service_m8pv9n9")
                put("template_id", "template_0vko6to")
                put("user_id", "TSHzaLMD9tDdszGxt")
                put("template_params", JSONObject().apply {
                    put("to_email", toEmail)
                    put("codigo", codigo)
                    put("to_name", "Usuario")
                    put("from_name", "Dr_Pe")
                })
            }

            val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("https://api.emailjs.com/api/v1.0/email/send")
                .post(body)
                .header("Content-Type", "application/json")
                .header("Authorization", "NbxXvwt6hVbZ1M_612PZF")
                .header("Origin", "http://localhost")
                .header("User-Agent", "Android-App")
                .build()

            Log.d(TAG, "Enviando email a: $toEmail")
            Log.d(TAG, "Request: ${jsonBody.toString()}")

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Error de conexión: ${e.message}")
                    onError("Error de conexión: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "Response code: ${response.code}")
                    Log.d(TAG, "Response body: $responseBody")

                    when {
                        response.isSuccessful -> {
                            Log.d(TAG, "Email enviado exitosamente")
                            onSuccess()
                        }
                        response.code == 403 -> {
                            Log.e(TAG, "ERROR 403: Credenciales inválidas")
                            onError("ERROR 403: Problema de autenticación con EmailJS")
                        }
                        response.code == 400 -> {
                            Log.e(TAG, "ERROR 400: Bad Request")
                            onError("ERROR 400: Solicitud incorrecta")
                        }
                        else -> {
                            Log.e(TAG, "Error ${response.code}: $responseBody")
                            onError("Error ${response.code}: $responseBody")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error creando la solicitud: ${e.message}")
            onError("Error creando la solicitud: ${e.message}")
        }
    }
}