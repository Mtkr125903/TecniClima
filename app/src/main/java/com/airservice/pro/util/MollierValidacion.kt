package com.airservice.pro.util

/** Campo del formulario del diagrama de Mollier. */
enum class MollierCampo {
    REFRIGERANTE,
    PRESION_BAJA,
    PRESION_ALTA,
    TEMPERATURA_SUCION,
    TEMPERATURA_LIQUIDO
}

/** Motivo por el que un campo del diagrama de Mollier no es valido. */
enum class MollierError {
    REFRIGERANTE_INVALIDO,
    PRESION_BAJA_INVALIDA,
    PRESION_ALTA_INVALIDA,
    PRESION_ALTA_MENOR,
    PRESION_FUERA_DE_RANGO,
    TEMPERATURA_SUCION_INVALIDA,
    TEMPERATURA_LIQUIDO_INVALIDA,
    TEMPERATURA_FUERA_DE_RANGO
}

/** Refrigerante disponible en el diagrama de Mollier. */
data class MollierRefrigerante(val clave: String, val etiqueta: String)

/** Entradas ya normalizadas del ciclo de refrigeracion. */
data class MollierEntradas(
    val refrigerante: String,
    val presionBajaKpa: Double,
    val presionAltaKpa: Double,
    val temperaturaSuccionC: Double,
    val temperaturaLiquidoC: Double
)

/** Resultado de validar el formulario: las entradas o los errores por campo. */
data class MollierValidacionResultado(
    val entradas: MollierEntradas?,
    val errores: Map<MollierCampo, MollierError>
) {
    val esValido: Boolean get() = entradas != null
}

/**
 * Validacion de las entradas del diagrama de Mollier.
 *
 * El parseo es estricto: texto vacio, letras o simbolos se rechazan, a
 * diferencia de NumeroUtil.aNumero que convierte cualquier basura en 0.
 */
object MollierValidacion {

    const val PRESION_MIN_KPA = 40.0
    const val PRESION_MAX_KPA = 6000.0
    const val TEMPERATURA_MIN_C = -50.0
    const val TEMPERATURA_MAX_C = 130.0

    val REFRIGERANTES: List<MollierRefrigerante> = listOf(
        MollierRefrigerante("R410A", "R-410A"),
        MollierRefrigerante("R134a", "R-134a"),
        MollierRefrigerante("R32", "R-32"),
        MollierRefrigerante("R290", "R-290")
    )

    private val NUMERO = Regex("^[+-]?\\d+([.,]\\d+)?$")

    /** Convierte texto a numero aceptando coma o punto decimal. */
    fun aNumeroEstricto(texto: String): Double? {
        val limpio = texto.trim()
        if (!NUMERO.matches(limpio)) return null
        return limpio.replace(',', '.').toDoubleOrNull()
    }

    fun validar(
        refrigerante: String,
        presionBaja: String,
        presionAlta: String,
        temperaturaSuccion: String,
        temperaturaLiquido: String
    ): MollierValidacionResultado {
        val errores = mutableMapOf<MollierCampo, MollierError>()

        if (REFRIGERANTES.none { it.clave == refrigerante }) {
            errores[MollierCampo.REFRIGERANTE] = MollierError.REFRIGERANTE_INVALIDO
        }

        val pBaja = aNumeroEstricto(presionBaja)
        if (pBaja == null) {
            errores[MollierCampo.PRESION_BAJA] = MollierError.PRESION_BAJA_INVALIDA
        } else if (pBaja < PRESION_MIN_KPA || pBaja > PRESION_MAX_KPA) {
            errores[MollierCampo.PRESION_BAJA] = MollierError.PRESION_FUERA_DE_RANGO
        }

        val pAlta = aNumeroEstricto(presionAlta)
        if (pAlta == null) {
            errores[MollierCampo.PRESION_ALTA] = MollierError.PRESION_ALTA_INVALIDA
        } else if (pAlta < PRESION_MIN_KPA || pAlta > PRESION_MAX_KPA) {
            errores[MollierCampo.PRESION_ALTA] = MollierError.PRESION_FUERA_DE_RANGO
        } else if (pBaja != null && pAlta <= pBaja) {
            errores[MollierCampo.PRESION_ALTA] = MollierError.PRESION_ALTA_MENOR
        }

        val tSuccion = aNumeroEstricto(temperaturaSuccion)
        if (tSuccion == null) {
            errores[MollierCampo.TEMPERATURA_SUCION] = MollierError.TEMPERATURA_SUCION_INVALIDA
        } else if (tSuccion < TEMPERATURA_MIN_C || tSuccion > TEMPERATURA_MAX_C) {
            errores[MollierCampo.TEMPERATURA_SUCION] = MollierError.TEMPERATURA_FUERA_DE_RANGO
        }

        val tLiquido = aNumeroEstricto(temperaturaLiquido)
        if (tLiquido == null) {
            errores[MollierCampo.TEMPERATURA_LIQUIDO] = MollierError.TEMPERATURA_LIQUIDO_INVALIDA
        } else if (tLiquido < TEMPERATURA_MIN_C || tLiquido > TEMPERATURA_MAX_C) {
            errores[MollierCampo.TEMPERATURA_LIQUIDO] = MollierError.TEMPERATURA_FUERA_DE_RANGO
        }

        if (errores.isNotEmpty()) {
            return MollierValidacionResultado(null, errores)
        }

        return MollierValidacionResultado(
            MollierEntradas(
                refrigerante = refrigerante,
                presionBajaKpa = pBaja!!,
                presionAltaKpa = pAlta!!,
                temperaturaSuccionC = tSuccion!!,
                temperaturaLiquidoC = tLiquido!!
            ),
            errores
        )
    }
}
