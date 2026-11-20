package com.airservice.pro.ui.mollier

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airservice.pro.R
import com.airservice.pro.databinding.FragmentMollierBinding
import com.airservice.pro.ui.common.UiHelpers.bindDropdown
import com.airservice.pro.util.MollierCampo
import com.airservice.pro.util.MollierEntradas
import com.airservice.pro.util.MollierError
import com.airservice.pro.util.MollierValidacion
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.util.Locale

class MollierFragment : Fragment() {

    private var _binding: FragmentMollierBinding? = null
    private val binding get() = _binding!!

    private var paginaLista = false
    private var refrigeranteSeleccionado = REFRIGERANTE_EJEMPLO

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMollierBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val refrigerantes = MollierValidacion.REFRIGERANTES
        binding.etRefrigerante.bindDropdown(refrigerantes, { it.etiqueta }) { elegido ->
            binding.etRefrigerante.setText(elegido?.etiqueta.orEmpty(), false)
            refrigeranteSeleccionado = elegido?.clave ?: refrigeranteSeleccionado
            actualizarAvisoInflamable()
        }
        binding.etRefrigerante.setText(refrigerantes.first().etiqueta, false)

        binding.etPresionBaja.setText(PRESION_BAJA_EJEMPLO)
        binding.etPresionAlta.setText(PRESION_ALTA_EJEMPLO)
        binding.etTempSuccion.setText(TEMP_SUCION_EJEMPLO)
        binding.etTempLiquido.setText(TEMP_LIQUIDO_EJEMPLO)

        binding.btnCalcular.setOnClickListener { calcular() }

        configurarWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configurarWebView() {
        binding.webMollier.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = false
            domStorageEnabled = false
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        binding.webMollier.addJavascriptInterface(Puente(), "Android")
        binding.webMollier.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                paginaLista = true
                aplicarTema()
                calcular()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return !request.url.toString().startsWith("file:///android_asset/")
            }
        }
        binding.webMollier.loadUrl("file:///android_asset/mollier.html")
    }

    private fun aplicarTema() {
        val tema = JSONObject().apply {
            put("fondo", color(R.color.background))
            put("texto", color(R.color.textPrimary))
            put("secundario", color(R.color.textSecondary))
            put("grilla", color(R.color.divider))
            put("domo", color(R.color.celeste))
            put("isobara", color(R.color.textSecondaryAlt))
            put("isoterma", color(R.color.naranja))
            put("ciclo", color(R.color.rojo))
            put("punto", color(R.color.verde))
        }
        binding.webMollier.evaluateJavascript("window.aplicarTema($tema)", null)
    }

    private fun calcular() {
        if (!paginaLista) {
            avisar(R.string.mollier_error_cargando)
            return
        }

        val resultado = MollierValidacion.validar(
            refrigerante = refrigeranteSeleccionado,
            presionBaja = binding.etPresionBaja.text?.toString().orEmpty(),
            presionAlta = binding.etPresionAlta.text?.toString().orEmpty(),
            temperaturaSuccion = binding.etTempSuccion.text?.toString().orEmpty(),
            temperaturaLiquido = binding.etTempLiquido.text?.toString().orEmpty()
        )

        if (!resultado.esValido) {
            mostrarErrores(resultado.errores)
            return
        }

        limpiarErrores()
        enviarAlMotor(resultado.entradas!!)
    }

    private fun enviarAlMotor(entradas: MollierEntradas) {
        val datos = JSONObject().apply {
            put("fluid", entradas.refrigerante)
            put("pBaja", entradas.presionBajaKpa)
            put("pAlta", entradas.presionAltaKpa)
            put("tSuccion", entradas.temperaturaSuccionC)
            put("tLiquido", entradas.temperaturaLiquidoC)
        }
        binding.webMollier.evaluateJavascript("window.evaluateCycle($datos)", null)
    }

    private fun mostrarErrores(errores: Map<MollierCampo, MollierError>) {
        binding.tilPresionBaja.error = null
        binding.tilPresionAlta.error = null
        binding.tilTempSuccion.error = null
        binding.tilTempLiquido.error = null

        errores.forEach { (campo, error) ->
            val mensaje = getString(mensajeDe(error))
            when (campo) {
                MollierCampo.PRESION_BAJA -> binding.tilPresionBaja.error = mensaje
                MollierCampo.PRESION_ALTA -> binding.tilPresionAlta.error = mensaje
                MollierCampo.TEMPERATURA_SUCION -> binding.tilTempSuccion.error = mensaje
                MollierCampo.TEMPERATURA_LIQUIDO -> binding.tilTempLiquido.error = mensaje
                MollierCampo.REFRIGERANTE -> avisar(mensaje)
            }
        }
    }

    private fun limpiarErrores() {
        binding.tilPresionBaja.error = null
        binding.tilPresionAlta.error = null
        binding.tilTempSuccion.error = null
        binding.tilTempLiquido.error = null
    }

    private fun actualizarAvisoInflamable() {
        binding.tvAvisoR290.visibility =
            if (refrigeranteSeleccionado == "R290") View.VISIBLE else View.GONE
    }

    private fun mensajeDe(error: MollierError): Int = when (error) {
        MollierError.REFRIGERANTE_INVALIDO -> R.string.mollier_error_refrigerante
        MollierError.PRESION_BAJA_INVALIDA -> R.string.mollier_error_presion_baja
        MollierError.PRESION_ALTA_INVALIDA -> R.string.mollier_error_presion_alta
        MollierError.PRESION_ALTA_MENOR -> R.string.mollier_error_presion_alta_menor
        MollierError.PRESION_FUERA_DE_RANGO -> R.string.mollier_error_presion_rango
        MollierError.TEMPERATURA_SUCION_INVALIDA -> R.string.mollier_error_t_succion
        MollierError.TEMPERATURA_LIQUIDO_INVALIDA -> R.string.mollier_error_t_liquido
        MollierError.TEMPERATURA_FUERA_DE_RANGO -> R.string.mollier_error_t_rango
    }

    private fun mensajeDe(codigo: String): Int = when (codigo) {
        "REFRIGERANTE_INVALIDO" -> R.string.mollier_error_refrigerante
        "P_BAJA_INVALIDA" -> R.string.mollier_error_presion_baja
        "P_ALTA_INVALIDA" -> R.string.mollier_error_presion_alta
        "P_ALTA_MENOR" -> R.string.mollier_error_presion_alta_menor
        "P_FUERA_DE_RANGO" -> R.string.mollier_error_presion_rango
        "T_SUCION_INVALIDA" -> R.string.mollier_error_t_succion
        "T_LIQUIDO_INVALIDA" -> R.string.mollier_error_t_liquido
        "T_FUERA_DE_RANGO" -> R.string.mollier_error_t_rango
        "SH_NEGATIVO" -> R.string.mollier_error_sh_negativo
        "SC_NEGATIVO" -> R.string.mollier_error_sc_negativo
        "T2_FUERA_DE_RANGO" -> R.string.mollier_error_t2_fuera
        else -> R.string.mollier_error_generico
    }

    private fun color(colorRes: Int): String {
        return String.format(Locale.US, "#%06X", 0xFFFFFF and ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun avisar(mensajeRes: Int) {
        val view = _binding?.root ?: return
        Snackbar.make(view, mensajeRes, Snackbar.LENGTH_LONG).show()
    }

    private fun avisar(mensaje: String) {
        val view = _binding?.root ?: return
        Snackbar.make(view, mensaje, Snackbar.LENGTH_LONG).show()
    }

    private fun enHiloPrincipal(accion: () -> Unit) {
        activity?.runOnUiThread {
            if (_binding != null) accion()
        }
    }

    private fun numero(valor: Double, decimales: Int = 1): String {
        return String.format(Locale.getDefault(), "%.${decimales}f", valor)
    }

    inner class Puente {

        @JavascriptInterface
        fun onResultsCalculated(sh: Double, sc: Double, tSatBaja: Double, tSatAlta: Double) {
            enHiloPrincipal {
                binding.tvSobrecalentamiento.text =
                    getString(R.string.mollier_valor_kelvin, numero(sh))
                binding.tvSubenfriamiento.text =
                    getString(R.string.mollier_valor_kelvin, numero(sc))
                binding.tvTsatBaja.text =
                    getString(R.string.mollier_valor_celsius, numero(tSatBaja))
                binding.tvTsatAlta.text =
                    getString(R.string.mollier_valor_celsius, numero(tSatAlta))
            }
        }

        @JavascriptInterface
        fun onCycleCalculated(json: String) {
            val datos = try {
                JSONObject(json)
            } catch (e: Exception) {
                null
            } ?: return

            enHiloPrincipal {
                binding.tvCop.text = numero(datos.optDouble("cop"), 2)
                binding.tvRazonCompresion.text = numero(datos.optDouble("razonCompresion"), 2)
                binding.tvTempDescarga.text =
                    getString(R.string.mollier_valor_celsius, numero(datos.optDouble("t2")))
                binding.tvH1.text =
                    getString(R.string.mollier_valor_kjkg, numero(datos.optDouble("h1")))
                binding.tvH2.text =
                    getString(R.string.mollier_valor_kjkg, numero(datos.optDouble("h2")))
                binding.tvCalidadMezcla.text =
                    getString(R.string.mollier_valor_porcentaje, numero(datos.optDouble("x4") * 100, 1))
            }
        }

        @JavascriptInterface
        fun onError(codigo: String) {
            enHiloPrincipal {
                avisar(mensajeDe(codigo))
            }
        }
    }

    override fun onDestroyView() {
        binding.webMollier.removeJavascriptInterface("Android")
        binding.webMollier.destroy()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REFRIGERANTE_EJEMPLO = "R410A"
        private const val PRESION_BAJA_EJEMPLO = "800"
        private const val PRESION_ALTA_EJEMPLO = "2600"
        private const val TEMP_SUCION_EJEMPLO = "12"
        private const val TEMP_LIQUIDO_EJEMPLO = "40"
    }
}
