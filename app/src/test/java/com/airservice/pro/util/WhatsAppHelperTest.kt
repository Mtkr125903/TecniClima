package com.airservice.pro.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppHelperTest {

    @Test
    fun `normaliza un numero internacional con mas`() {
        assertEquals("5491155551010", WhatsAppHelper.normalizarTelefono("+54 9 11 5555 1010"))
    }

    @Test
    fun `normaliza un numero local de 10 digitos`() {
        assertEquals("5491155551010", WhatsAppHelper.normalizarTelefono("11 5555 1010"))
    }

    @Test
    fun `quita el cero inicial de los prefijos locales`() {
        assertEquals("5491155551010", WhatsAppHelper.normalizarTelefono("011 15 5555 1010"))
    }

    @Test
    fun `ignora guiones espacios y parentesis`() {
        assertEquals("5491155551010", WhatsAppHelper.normalizarTelefono("(11) 5555-1010"))
    }

    @Test
    fun `un numero de linea fijo no agrega el nueve`() {
        assertEquals("543413555555", WhatsAppHelper.normalizarTelefono("341 355 5555"))
    }

    @Test
    fun `devuelve vacio para telefonos nulos o sin digitos`() {
        assertEquals("", WhatsAppHelper.normalizarTelefono(null))
        assertEquals("", WhatsAppHelper.normalizarTelefono("   "))
        assertEquals("", WhatsAppHelper.normalizarTelefono("sin numero"))
    }
}
