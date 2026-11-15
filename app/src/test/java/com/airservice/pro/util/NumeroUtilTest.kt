package com.airservice.pro.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumeroUtilTest {

    @Test
    fun `aNumero acepta numero plano`() {
        assertEquals(35000.0, NumeroUtil.aNumero("35000"), 0.001)
    }

    @Test
    fun `aNumero acepta formato con punto de miles`() {
        assertEquals(35000.0, NumeroUtil.aNumero("$ 35.000"), 0.001)
    }

    @Test
    fun `aNumero acepta coma decimal`() {
        assertEquals(35000.5, NumeroUtil.aNumero("35.000,50"), 0.001)
    }

    @Test
    fun `aNumero devuelve cero para valores vacios o invalidos`() {
        assertEquals(0.0, NumeroUtil.aNumero(null), 0.001)
        assertEquals(0.0, NumeroUtil.aNumero(""), 0.001)
        assertEquals(0.0, NumeroUtil.aNumero("abc"), 0.001)
    }

    @Test
    fun `normalizar deja los enteros sin decimales`() {
        assertEquals("35000", NumeroUtil.normalizar("$ 35.000"))
        assertEquals("35000.5", NumeroUtil.normalizar("35.000,50"))
    }

    @Test
    fun `moneda usa el separador local y el simbolo`() {
        val resultado = NumeroUtil.moneda("35000")
        assertTrue(resultado.startsWith("$ "))
        assertTrue(resultado.contains("35.000"))
    }
}
