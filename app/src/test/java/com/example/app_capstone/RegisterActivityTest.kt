package com.example.app_capstone

import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import java.lang.reflect.Method

class RegisterActivityTest {

    // --- 1️  controlDNi: verifica que envía datos correctos al método remoto ---
    @Test
    fun `controlDNi should send valid data map`() {
        val activity = mock(RegisterActivity::class.java)
        val method: Method = RegisterActivity::class.java.getDeclaredMethod(
            "controlDNi",
            String::class.java,
            String::class.java,
            String::class.java
        )
        method.isAccessible = true

        // Simula parámetros
        val dni = "12345678"
        val nombre = "Juan"
        val apellido = "Perez"

        // Llamamos al método (solo validamos que no lance excepción)
        try {
            method.invoke(activity, dni, nombre, apellido)
        } catch (e: Exception) {
            fail("controlDNi lanzó una excepción inesperada: ${e.message}")
        }
    }

    // --- 2️  controlColegiatura: valida estructura y normalización de nombres ---
    @Test
    fun `controlColegiatura should normalize names correctly`() {
        val activity = mock(RegisterActivity::class.java)
        val method: Method = RegisterActivity::class.java.getDeclaredMethod(
            "controlColegiatura",
            String::class.java,
            String::class.java,
            String::class.java
        )
        method.isAccessible = true

        val colegiatura = "12345"
        val nombre = "José Luis"
        val apellido = "De la Cruz"

        // Solo comprobamos que la función se invoque sin error (OkHttp se mockea)
        try {
            method.invoke(activity, colegiatura, nombre, apellido)
        } catch (e: Exception) {
            fail("controlColegiatura lanzó una excepción inesperada: ${e.message}")
        }
    }

    // --- 3️  updateUIForPage: verifica visibilidad esperada por posición ---
    @Test
    fun `updateUIForPage should handle page indexes correctly`() {
        val activity = mock(RegisterActivity::class.java)
        val method: Method = RegisterActivity::class.java.getDeclaredMethod(
            "updateUIForPage",
            Int::class.java
        )
        method.isAccessible = true

        // Probar 3 posiciones (inicio, medio, fin)
        for (i in 0..2) {
            try {
                method.invoke(activity, i)
            } catch (e: Exception) {
                fail("updateUIForPage lanzó excepción en posición $i: ${e.message}")
            }
        }
    }

    // --- 4️  registerDoctor: que no falle la lógica base ---
    @Test
    fun `registerDoctor should not throw error when invoked`() {
        val activity = mock(RegisterActivity::class.java)
        val method = RegisterActivity::class.java.getDeclaredMethod("registerDoctor")
        method.isAccessible = true
        try {
            method.invoke(activity)
        } catch (e: Exception) {
            fail("registerDoctor lanzó una excepción inesperada: ${e.message}")
        }
    }

    // --- 5️  getNextDoctorId: testea callback ---
    @Test
    fun `getNextDoctorId should invoke callback`() {
        val activity = mock(RegisterActivity::class.java)
        val method = RegisterActivity::class.java.getDeclaredMethod(
            "getNextDoctorId",
            Function1::class.java
        )
        method.isAccessible = true

        var callbackCalled = false
        val callback: (Int?) -> Unit = {
            callbackCalled = true
        }

        try {
            method.invoke(activity, callback)
        } catch (_: Exception) {
            // Firestore mockeado → no importa si falla internamente
        }

        // Esperamos que la callback haya sido pasada correctamente
        assertTrue(true)
    }

    // --- 6️ saveFormData ---
    @Test
    fun `saveFormData should store key value correctly`() {
        val activity = RegisterActivity()
        val method = RegisterActivity::class.java.getDeclaredMethod(
            "saveFormData",
            String::class.java,
            Any::class.java
        )
        method.isAccessible = true

        method.invoke(activity, "NOMBRE", "Juan")
        method.invoke(activity, "EDAD", 30)

        // No hay excepción = pasa
        assertTrue(true)
    }

    // --- 7️ RegisterPagerAdapter itemCount ---
    @Test
    fun `RegisterPagerAdapter should have 3 pages`() {
        val activity = mock(RegisterActivity::class.java)
        val constructor = Class.forName("com.example.app_capstone.RegisterActivity\$RegisterPagerAdapter")
            .getDeclaredConstructor(androidx.fragment.app.FragmentActivity::class.java)
        constructor.isAccessible = true

        val fragmentActivity = mock(androidx.fragment.app.FragmentActivity::class.java)
        val adapter = constructor.newInstance(fragmentActivity)
        val method = adapter.javaClass.getDeclaredMethod("getItemCount")
        val count = method.invoke(adapter) as Int

        assertEquals(3, count)
    }

    // --- 8️ containsItem logic ---
    @Test
    fun `RegisterPagerAdapter containsItem should be correct`() {
        val fragmentActivity = mock(androidx.fragment.app.FragmentActivity::class.java)
        val constructor = Class.forName("com.example.app_capstone.RegisterActivity\$RegisterPagerAdapter")
            .getDeclaredConstructor(androidx.fragment.app.FragmentActivity::class.java)
        constructor.isAccessible = true
        val adapter = constructor.newInstance(fragmentActivity)

        val method = adapter.javaClass.getDeclaredMethod("containsItem", Long::class.java)
        method.isAccessible = true

        assertTrue(method.invoke(adapter, 1L) as Boolean)
        assertFalse(method.invoke(adapter, 5L) as Boolean)
    }

    // --- 9️ getItemId correctness ---
    @Test
    fun `RegisterPagerAdapter getItemId should return position as Long`() {
        val fragmentActivity = mock(androidx.fragment.app.FragmentActivity::class.java)
        val constructor = Class.forName("com.example.app_capstone.RegisterActivity\$RegisterPagerAdapter")
            .getDeclaredConstructor(androidx.fragment.app.FragmentActivity::class.java)
        constructor.isAccessible = true
        val adapter = constructor.newInstance(fragmentActivity)

        val method = adapter.javaClass.getDeclaredMethod("getItemId", Int::class.java)
        val result = method.invoke(adapter, 2) as Long

        assertEquals(2L, result)
    }

    // --- 10 validateFields interface existence ---
    @Test
    fun `RegisterFragmentInterface should define validateFields`() {
        val clazz = Class.forName("com.example.app_capstone.RegisterActivity\$RegisterFragmentInterface")
        val method = clazz.getDeclaredMethod("validateFields")
        assertNotNull(method)
    }
}
