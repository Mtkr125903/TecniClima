package com.airservice.pro.data

import android.webkit.WebView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.airservice.pro.R
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MollierMotorTest {

    private lateinit var webView: WebView

    private val listo = CountDownLatch(1)
    private val calculado = CountDownLatch(1)

    @Volatile
    private var errorJs: String? = null

    @Volatile
    private var cicloJs: JSONObject? = null

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val contexto = ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.Theme_AirServicePro
            )
            webView = WebView(contexto)
            webView.settings.javaScriptEnabled = true
            webView.settings.allowFileAccess = true
            webView.addJavascriptInterface(Puente(), "Android")
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    listo.countDown()
                }
            }
            webView.loadUrl("file:///android_asset/mollier.html")
        }
        assertTrue("La pagina del diagrama no cargo", listo.await(20, TimeUnit.SECONDS))
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            webView.removeJavascriptInterface("Android")
            webView.destroy()
        }
    }

    private fun evaluar(script: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            webView.evaluateJavascript(script, null)
        }
    }

    private fun esperarCalculo(): JSONObject {
        assertTrue("El motor no respondio", calculado.await(20, TimeUnit.SECONDS))
        assertNull("El motor informo un error: $errorJs", errorJs)
        return cicloJs!!
    }

    @Test
    fun elMotorReproduceElCasoDeReferenciaDeCoolProp() {
        val referencia = leerReferencia()

        evaluar(
            "window.evaluateCycle({fluid:'R410A',pBaja:${referencia.getDouble("pBaja")}," +
                "pAlta:${referencia.getDouble("pAlta")},tSuccion:${referencia.getDouble("tSuccion")}," +
                "tLiquido:${referencia.getDouble("tLiquido")}})"
        )
        val ciclo = esperarCalculo()

        assertEquals(referencia.getDouble("tSatBaja"), ciclo.getDouble("tSatBaja"), 0.15)
        assertEquals(referencia.getDouble("tSatAlta"), ciclo.getDouble("tSatAlta"), 0.15)
        assertEquals(referencia.getDouble("sh"), ciclo.getDouble("sh"), 0.15)
        assertEquals(referencia.getDouble("sc"), ciclo.getDouble("sc"), 0.15)
        assertEquals(referencia.getDouble("h1"), ciclo.getDouble("h1"), 1.0)
        assertEquals(referencia.getDouble("h2"), ciclo.getDouble("h2"), 1.5)
        assertEquals(referencia.getDouble("t2"), ciclo.getDouble("t2"), 1.0)
        assertEquals(referencia.getDouble("x4"), ciclo.getDouble("x4"), 0.01)
        assertEquals(referencia.getDouble("cop"), ciclo.getDouble("cop"), 0.06)
    }

    @Test
    fun elMotorInformaSobrecalentamientoNegativo() {
        evaluar("window.evaluateCycle({fluid:'R410A',pBaja:800,pAlta:2600,tSuccion:-20,tLiquido:40})")
        assertTrue("El motor no informo el error", calculado.await(20, TimeUnit.SECONDS))
        assertEquals("SH_NEGATIVO", errorJs)
    }

    @Test
    fun elMotorInformaPresionFueraDeRango() {
        evaluar("window.evaluateCycle({fluid:'R410A',pBaja:800,pAlta:9000,tSuccion:12,tLiquido:40})")
        assertTrue("El motor no informo el error", calculado.await(20, TimeUnit.SECONDS))
        assertEquals("P_FUERA_DE_RANGO", errorJs)
    }

    @Test
    fun elDiagramaSeConstruyeConLaCampanaDeSaturacion() {
        val trazas = mutableListOf<String>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            webView.evaluateJavascript(
                "window.mollierEngine.construirFigura(" +
                    "window.mollierEngine.datos.R410A," +
                    "window.mollierEngine.calcular('R410A',800,2600,12,40)).data.length"
            ) { resultado ->
                trazas.add(resultado)
            }
        }
        var waited = 0
        while (trazas.isEmpty() && waited < 20) {
            Thread.sleep(200)
            waited++
        }
        assertTrue("No se obtuvo la figura", trazas.isNotEmpty())
        assertTrue("La figura no tiene trazas", trazas.first().toInt() > 10)
    }

    private fun leerReferencia(): JSONObject {
        var referencia: JSONObject? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            webView.evaluateJavascript("window.MOLLIER_CHECK") { resultado ->
                referencia = JSONObject(resultado)
            }
        }
        var waited = 0
        while (referencia == null && waited < 20) {
            Thread.sleep(200)
            waited++
        }
        assertNotNull("No se pudo leer la referencia", referencia)
        return referencia!!
    }

    inner class Puente {
        @android.webkit.JavascriptInterface
        fun onCycleCalculated(json: String) {
            cicloJs = try {
                JSONObject(json)
            } catch (e: Exception) {
                null
            }
            calculado.countDown()
        }

        @android.webkit.JavascriptInterface
        fun onResultsCalculated(sh: Double, sc: Double, tSatBaja: Double, tSatAlta: Double) {
        }

        @android.webkit.JavascriptInterface
        fun onError(codigo: String) {
            errorJs = codigo
            calculado.countDown()
        }
    }
}
