package com.airservice.pro.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    /**
     * Comparte un archivo PDF generado a traves de FileProvider.
     */
    fun compartirPdf(context: Context, archivo: File, textoExtra: String? = null): Boolean {
        if (!archivo.exists()) return false
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivo
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                setClipData(ClipData.newRawUri(null, uri))
                textoExtra?.let { putExtra(Intent.EXTRA_TEXT, it) }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, context.getString(com.airservice.pro.R.string.share_pdf_title))
                    .apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}
