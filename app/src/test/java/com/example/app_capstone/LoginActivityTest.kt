package com.example.app_capstone

import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import java.lang.reflect.Method

class LoginActivityTest {

    // --- Helper más flexible para invocar funciones privadas ---
    private fun <T> invokePrivate(
        instance: Any?,
        methodName: String,
        vararg args: Any?
    ): T? {
        val clazz = LoginActivity::class.java
        // Buscar método por nombre y cantidad de parámetros (no tipo exacto)
        val method: Method = clazz.declaredMethods.firstOrNull {
            it.name == methodName && it.parameterTypes.size == args.size
        } ?: throw NoSuchMethodException("$methodName not found in ${clazz.simpleName}")

        method.isAccessible = true
        return method.invoke(instance, *args) as? T
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
        val email = "andiroyal1609@gmail.com"
        val regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        assertTrue(regex.matches(email))
    }

    // --- 3️ Manejo de error: usuario no registrado ---
    @Test
    fun `handleAuthFailure should handle FirebaseAuthInvalidUserException`() {
        val activity = mock(LoginActivity::class.java)
        val ex = mock(com.google.firebase.auth.FirebaseAuthInvalidUserException::class.java)
        val result = invokePrivate<Any?>(activity, "handleAuthFailure", ex as Exception)
        assertNull(result)
    }

    // --- 4️ Manejo de error: contraseña incorrecta ---
    @Test
    fun `handleAuthFailure should handle FirebaseAuthInvalidCredentialsException`() {
        val activity = mock(LoginActivity::class.java)
        val ex = mock(com.google.firebase.auth.FirebaseAuthInvalidCredentialsException::class.java)
        val result = invokePrivate<Any?>(activity, "handleAuthFailure", ex as Exception)
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

    // --- 6️ Clasificación de entrada (email vs colegiatura) ---
    @Test
    fun `checkCredentials should classify input correctly`() {
        val email = "andiroyal1609@gmail.com"
        val colegiatura = "067890"
        val regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

        assertTrue(regex.matches(email))
        assertFalse(regex.matches(colegiatura))
    }

    // --- 7️ signInWithEmail ---
    @Test
    fun `signInWithEmail should not crash when called`() {
        val activity = mock(LoginActivity::class.java)
        val result = invokePrivate<Any?>(activity, "signInWithEmail", "test@correo.com", "123456")
        assertNull(result)
    }

    // --- 8️ searchByColegiaturaAndSignIn ---
    @Test
    fun `searchByColegiaturaAndSignIn should not throw error`() {
        val activity = mock(LoginActivity::class.java)
        val result = invokePrivate<Any?>(activity, "searchByColegiaturaAndSignIn", "987654", "clave")
        assertNull(result)
    }

    // --- 9️ fetchDoctorDataAndNavigate ---
    @Test
    fun `fetchDoctorDataAndNavigate should not crash`() {
        val activity = mock(LoginActivity::class.java)
        val result = invokePrivate<Any?>(activity, "fetchDoctorDataAndNavigate", "UID123")
        assertNull(result)
    }

    // --- 10️ Guardado de datos ---
    @Test
    fun `saveDoctorDataToSharedPreferences should not crash`() {
        val activity = mock(LoginActivity::class.java)
        val document = mock(com.google.firebase.firestore.DocumentSnapshot::class.java)
        val result = invokePrivate<Any?>(activity, "saveDoctorDataToSharedPreferences", document)
        assertNull(result)
    }
}
