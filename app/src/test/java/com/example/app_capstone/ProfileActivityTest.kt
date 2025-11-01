package com.example.app_capstone

import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivityTest {

    // Helper para acceder a funciones privadas
    private fun <T> invokePrivate(
        instance: Any?,
        methodName: String,
        vararg args: Any?
    ): T {
        val types = args.map { it?.javaClass ?: Any::class.java }.toTypedArray()
        val method: Method = ProfileActivity::class.java.getDeclaredMethod(methodName, *types)
        method.isAccessible = true
        return method.invoke(instance, *args) as T
    }

    //1 ordenarHorarios()
    @Test
    fun `ordenarHorarios should sort times ascending`() {
        val activity = mock(ProfileActivity::class.java)
        val input = listOf("14:30:00", "08:15:00", "09:00:00")
        val expected = listOf("08:15:00", "09:00:00", "14:30:00")

        val result: List<*> = invokePrivate(activity, "ordenarHorarios", input)
        assertEquals(expected, result)
    }

    // 2 convertirFechaFormato()
    @Test
    fun `convertirFechaFormato should convert correctly`() {
        val activity = mock(ProfileActivity::class.java)
        val result: String = invokePrivate(activity, "convertirFechaFormato", "31/10/2025")
        assertEquals("2025/10/31", result)
    }

    // 3 convertirHorarioFormato()
    @Test
    fun `convertirHorarioFormato should convert to 24h format`() {
        val activity = mock(ProfileActivity::class.java)
        val result: String = invokePrivate(activity, "convertirHorarioFormato", "02:30 PM")
        assertEquals("14:30:00", result)
    }

    // 4 getDiaSemanaFromDate()
    @Test
    fun `getDiaSemanaFromDate should return correct day of week`() {
        val activity = mock(ProfileActivity::class.java)
        val result: String = invokePrivate(activity, "getDiaSemanaFromDate", "2025/10/31")
        assertTrue(result in listOf("Viernes", "Friday")) // por formato local
    }

    // 5 agregarCampoHorarioConValor()
    @Test
    fun `agregarCampoHorarioConValor should not throw error`() {
        val activity = mock(ProfileActivity::class.java)
        val result = invokePrivate<Any?>(activity, "agregarCampoHorarioConValor", "08:00")
        assertNull(result) // No devuelve nada, solo probamos que no falle
    }

    // 6 agregarDisponibilidadUI()
    @Test
    fun `agregarDisponibilidadUI should not crash when called`() {
        val activity = mock(ProfileActivity::class.java)
        val fecha = "2025/10/31"
        val horarios = listOf("08:00", "09:00")
        val docId = "123ABC"
        val result = invokePrivate<Any?>(activity, "agregarDisponibilidadUI", fecha, horarios, docId)
        assertNull(result)
    }

    // 7 convertirFechaFormato inverso (invalid)
    @Test
    fun `convertirFechaFormato should handle invalid format gracefully`() {
        val activity = mock(ProfileActivity::class.java)
        try {
            invokePrivate<String>(activity, "convertirFechaFormato", "invalid")
            fail("Expected exception not thrown")
        } catch (e: Exception) {
            assertTrue(e is Exception)
        }
    }

    // 8️ ordenarHorarios con horas iguales
    @Test
    fun `ordenarHorarios should handle equal times`() {
        val activity = mock(ProfileActivity::class.java)
        val input = listOf("09:00:00", "09:00:00", "08:00:00")
        val expected = listOf("08:00:00", "09:00:00", "09:00:00")
        val result: List<*> = invokePrivate(activity, "ordenarHorarios", input)
        assertEquals(expected, result)
    }

    // 9️ convertirHorarioFormato 12AM case
    @Test
    fun `convertirHorarioFormato should handle 12AM correctly`() {
        val activity = mock(ProfileActivity::class.java)
        val result: String = invokePrivate(activity, "convertirHorarioFormato", "12:00 AM")
        assertEquals("00:00:00", result)
    }

    // 10 getDiaSemanaFromDate with another date
    @Test
    fun `getDiaSemanaFromDate should return Lunes for Monday`() {
        val activity = mock(ProfileActivity::class.java)
        val sdf = SimpleDateFormat("yyyy/MM/dd")
        val mondayDate = "2025/11/03" // Es lunes
        val result: String = invokePrivate(activity, "getDiaSemanaFromDate", mondayDate)
        assertTrue(result.contains("Lunes") || result.contains("Monday"))
    }
}
