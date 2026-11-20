package com.airservice.pro.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MollierValidacionTest {

    private fun validar(
        refrigerante: String = "R410A",
        presionBaja: String = "800",
        presionAlta: String = "2600",
        succion: String = "12",
        liquido: String = "40"
    ) = MollierValidacion.validar(refrigerante, presionBaja, presionAlta, succion, liquido)

    @Test
    fun `una entrada valida devuelve las entradas normalizadas`() {
        val resultado = validar()

        assertTrue(resultado.esValido)
        assertEquals("R410A", resultado.entradas?.refrigerante)
        assertEquals(800.0, resultado.entradas?.presionBajaKpa ?: 0.0, 0.001)
        assertEquals(2600.0, resultado.entradas?.presionAltaKpa ?: 0.0, 0.001)
        assertEquals(12.0, resultado.entradas?.temperaturaSuccionC ?: 0.0, 0.001)
        assertEquals(40.0, resultado.entradas?.temperaturaLiquidoC ?: 0.0, 0.001)
    }

    @Test
    fun `el punto decimal puede ser coma`() {
        val resultado = validar(presionBaja = "800,5")

        assertTrue(resultado.esValido)
        assertEquals(800.5, resultado.entradas?.presionBajaKpa ?: 0.0, 0.001)
    }

    @Test
    fun `un texto no numerico se rechaza en vez de convertirse en cero`() {
        val resultado = validar(presionBaja = "abc")

        assertTrue(!resultado.esValido)
        assertEquals(
            MollierError.PRESION_BAJA_INVALIDA,
            resultado.errores[MollierCampo.PRESION_BAJA]
        )
    }

    @Test
    fun `un numero con letras al final se rechaza`() {
        val resultado = validar(presionAlta = "2600kPa")

        assertTrue(!resultado.esValido)
        assertEquals(
            MollierError.PRESION_ALTA_INVALIDA,
            resultado.errores[MollierCampo.PRESION_ALTA]
        )
    }

    @Test
    fun `un campo vacio se rechaza`() {
        val resultado = validar(succion = "   ")

        assertTrue(!resultado.esValido)
        assertEquals(
            MollierError.TEMPERATURA_SUCION_INVALIDA,
            resultado.errores[MollierCampo.TEMPERATURA_SUCION]
        )
    }

    @Test
    fun `la presion alta debe ser mayor que la baja`() {
        val resultado = validar(presionBaja = "2600", presionAlta = "800")

        assertTrue(!resultado.esValido)
        assertEquals(
            MollierError.PRESION_ALTA_MENOR,
            resultado.errores[MollierCampo.PRESION_ALTA]
        )
    }

    @Test
    fun `una presion fuera de rango se rechaza`() {
        val resultado = validar(presionBaja = "10")

        assertTrue(!resultado.esValido)
        assertEquals(
            MollierError.PRESION_FUERA_DE_RANGO,
            resultado.errores[MollierCampo.PRESION_BAJA]
        )
    }

    @Test
    fun `una temperatura fuera de rango se rechaza`() {
        val resultado = validar(liquido = "200")

        assertTrue(!resultado.esValido)
        assertEquals(
            MollierError.TEMPERATURA_FUERA_DE_RANGO,
            resultado.errores[MollierCampo.TEMPERATURA_LIQUIDO]
        )
    }

    @Test
    fun `un refrigerante desconocido se rechaza`() {
        val resultado = validar(refrigerante = "R999")

        assertTrue(!resultado.esValido)
        assertEquals(
            MollierError.REFRIGERANTE_INVALIDO,
            resultado.errores[MollierCampo.REFRIGERANTE]
        )
    }

    @Test
    fun `los cuatro refrigerantes del modulo estan soportados`() {
        assertEquals(4, MollierValidacion.REFRIGERANTES.size)
        MollierValidacion.REFRIGERANTES.forEach { refrigerante ->
            val resultado = validar(refrigerante = refrigerante.clave)
            assertTrue("Fallo con ${refrigerante.clave}", resultado.esValido)
        }
    }

    @Test
    fun `el parseo numerico acepta signos y decimales`() {
        assertEquals(-12.5, MollierValidacion.aNumeroEstricto("-12,5") ?: 0.0, 0.0001)
        assertEquals(40.0, MollierValidacion.aNumeroEstricto(" 40 ") ?: 0.0, 0.0001)
        assertNull(MollierValidacion.aNumeroEstricto("1.2.3"))
        assertNull(MollierValidacion.aNumeroEstricto(""))
        assertNull(MollierValidacion.aNumeroEstricto("NaN"))
        assertNotNull(MollierValidacion.aNumeroEstricto("+7"))
    }
}
