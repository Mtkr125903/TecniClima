package com.airservice.pro.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Almacenamiento de fotos adjuntas.
 *
 * Los archivos viven en el directorio privado de la app (external-files/fotos)
 * y se comparten como content:// con FileProvider.
 */
object PhotoStorage {

    const val CARPETA = "fotos"

    /** Tipo de foto adjunta a un equipo (columna photo.tipo). */
    const val TIPO_FOTO_EQUIPO = "EQUIPMENT"

    /** Crea un archivo vacio para una foto nueva y devuelve su Uri. */
    fun nuevoArchivo(context: Context): Pair<File, Uri> {
        val carpeta = File(context.getExternalFilesDir(null) ?: context.filesDir, CARPETA)
        carpeta.mkdirs()
        val nombre = "foto_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
        val archivo = File(carpeta, nombre)
        archivo.createNewFile()
        return archivo to uriDe(context, archivo)
    }

    /** Uri seguro (FileProvider) para un archivo de la carpeta de fotos. */
    fun uriDe(context: Context, archivo: File): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )

    /**
     * Copia una imagen elegida en la galeria al almacenamiento propio de la app
     * (el permiso temporal de la galeria no dura para siempre).
     */
    fun copiarDesde(context: Context, origen: Uri): Uri? = runCatching {
        val (archivo, uri) = nuevoArchivo(context)
        context.contentResolver.openInputStream(origen)?.use { entrada ->
            archivo.outputStream().use { salida -> entrada.copyTo(salida) }
        } ?: return null
        uri
    }.getOrNull()

    /** Borra el archivo fisico de una foto (ignora si ya no existe). */
    fun borrarArchivo(context: Context, uri: String) {
        runCatching {
            val ruta = uri.substringAfter("fotos/", "")
            if (ruta.isNotBlank() && ruta != uri) {
                val carpeta = File(context.getExternalFilesDir(null) ?: context.filesDir, CARPETA)
                File(carpeta, ruta).delete()
            }
        }
    }
}
