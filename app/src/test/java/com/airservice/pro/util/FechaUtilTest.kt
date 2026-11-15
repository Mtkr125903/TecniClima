package com.airservice.pro.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class FechaUtilTest {

    @Test
    fun `hoyIso tiene el formato yyyy-MM-dd`() {
        assertTrue(FechaUtil.hoyIso().matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun `ahoraIso tiene el formato yyyy-MM-dd HH-mm`() {
        assertTrue(FechaUtil.ahoraIso().matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")))
    }

    @Test
    fun `mostrar convierte el iso a formato local`() {
        assertEquals("14/03/2023", FechaUtil.mostrar("2023-03-14"))
        assertEquals("14/03/2023 09:30", FechaUtil.mostrar("2023-03-14 09:30"))
    }

    @Test
    fun `mostrar devuelve el valor original si no es iso`() {
        assertEquals("sin fecha", FechaUtil.mostrar("sin fecha"))
        assertEquals("", FechaUtil.mostrar(null))
    }

    @Test
    fun `parseIso interpreta fechas y horas`() {
        assertNotNull(FechaUtil.parseIso("2023-03-14"))
        assertNotNull(FechaUtil.parseIso("2023-03-14 09:30"))
        assertNull(FechaUtil.parseIso("texto"))
    }

    @Test
    fun `esHoy detecta el dia actual`() {
        assertTrue(FechaUtil.esHoy(FechaUtil.hoyIso()))
        assertTrue(FechaUtil.esHoy(FechaUtil.hoyIso() + " 10:00"))
        assertFalse(FechaUtil.esHoy("2020-01-01"))
        assertFalse(FechaUtil.esHoy(null))
    }

    @Test
    fun `sumarDias suma y resta correctamente`() {
        assertEquals("2023-03-15", FechaUtil.sumarDias("2023-03-14", 1))
        assertEquals("2023-03-13", FechaUtil.sumarDias("2023-03-14", -1))
        assertEquals("2023-04-14", FechaUtil.sumarDias("2023-03-14", 31))
    }

    @Test
    fun `sumarDias cruza el cambio de anio`() {
        assertEquals("2024-01-01", FechaUtil.sumarDias("2023-12-31", 1))
    }

    @Test
    fun `isoDia formatea un date`() {
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(2023, Calendar.MARCH, 14, 9, 30, 0)
        assertEquals("2023-03-14", FechaUtil.isoDia(calendar.time as Date))
    }
}
