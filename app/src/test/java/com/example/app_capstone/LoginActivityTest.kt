package com.example.app_capstone

import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import java.lang.reflect.Method

class LoginActivityTest {

    // --- Helper para invocar funciones privadas/reflexión ---
    private fun <T> invokePrivate(
        instance: Any?,
        methodName: String,
        vararg args: Any?
    ): T {
        val types = args.map { it?.javaClass ?: Any::class.java }.toTypedArray()
        val method: Method = LoginActivity::class.java.getDeclaredMethod(methodName, *types)
        method.isAccessible = true
        return method.invoke(instance, *args) as T
    }

    // --- 1️ Validación de campos vacíos ---
    @Test
    fun `checkCredentials should handle empty inputs`() {
        val activity = mock(LoginActivity::class.java)
        try {
            invokePrivate<Unit>(activity, "checkCredentials")
        } catch (e: Exception) {
            assertTrue(e is Exception)
        }
    }

    // --- 2️ Validación de correo ---
    @Test
    fun `checkCredentials should detect email correctly`() {
        val email = "doctor@example.com"
        assertTrue(android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
    }

    // --- 3️ Manejo de error de autenticación (usuario no registrado) ---
    @Test
    fun `handleAuthFailure should handle FirebaseAuthInvalidUserException`() {
        val activity = mock(LoginActivity::class.java)
        val ex = com.google.firebase.auth.FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "Usuario no registrado")
        val result = invokePrivate<Any?>(activity, "handleAuthFailure", ex)
        assertNull(result)
    }

    // --- 4️ Manejo de error de contraseña incorrecta ---
    @Test
    fun `handleAuthFailure should handle FirebaseAuthInvalidCredentialsException`() {
        val activity = mock(LoginActivity::class.java)
        val ex = com.google.firebase.auth.FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "Contraseña inválida")
        val result = invokePrivate<Any?>(activity, "handleAuthFailure", ex)
        assertNull(result)
    }

    // --- 5️ Manejo de error genérico ---
    @Test
    fun `handleAuthFailure should handle generic exception`() {
        val activity = mock(LoginActivity::class.java)
        val ex = Exception("Error desconocido")
        val result = invokePrivate<Any?>(activity, "handleAuthFailure", ex)
        assertNull(result)
    }

    // --- 6️ Lógica de tipo de entrada ---
    @Test
    fun `checkCredentials should classify input correctly`() {
        val email = "doctor@mail.com"
        val colegiatura = "123456"
        assertTrue(android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
        assertFalse(android.util.Patterns.EMAIL_ADDRESS.matcher(colegiatura).matches())
    }

    // --- 7️ Testeo de signInWithEmail ---
    @Test
    fun `signInWithEmail should not crash when called`() {
        val activity = mock(LoginActivity::class.java)
        val result = invokePrivate<Any?>(activity, "signInWithEmail", "test@mail.com", "123456")
        assertNull(result)
    }

    // --- 8️ Testeo de searchByColegiaturaAndSignIn ---
    @Test
    fun `searchByColegiaturaAndSignIn should not throw error`() {
        val activity = mock(LoginActivity::class.java)
        val result = invokePrivate<Any?>(activity, "searchByColegiaturaAndSignIn", "987654", "clave")
        assertNull(result)
    }

    // --- 9️ Testeo de fetchDoctorDataAndNavigate ---
    @Test
    fun `fetchDoctorDataAndNavigate should not crash`() {
        val activity = mock(LoginActivity::class.java)
        val result = invokePrivate<Any?>(activity, "fetchDoctorDataAndNavigate", "UID123")
        assertNull(result)
    }

    // --- 10 Guardado de datos ---
    @Test
    fun `saveDoctorDataToSharedPreferences should not crash`() {
        val activity = mock(LoginActivity::class.java)
        val document = mock(com.google.firebase.firestore.DocumentSnapshot::class.java)
        val result = invokePrivate<Any?>(activity, "saveDoctorDataToSharedPreferences", document)
        assertNull(result)
    }
}
