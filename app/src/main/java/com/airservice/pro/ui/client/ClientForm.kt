package com.airservice.pro.ui.client

import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.ClientEntity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Formulario de alta / edicion de clientes. */
object ClientForm {

    fun show(fragment: Fragment, cliente: ClientEntity?, onSaved: (ClientEntity) -> Unit = {}) {
        val context = fragment.requireContext()
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.fragment_cliente_detalle, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombre)
        val etTelefono = dialogView.findViewById<EditText>(R.id.etTelefono)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etDireccion = dialogView.findViewById<EditText>(R.id.etDireccion)
        val etNotas = dialogView.findViewById<EditText>(R.id.etNotas)

        if (cliente != null) {
            etNombre.setText(cliente.nombre)
            etTelefono.setText(cliente.telefono)
            etEmail.setText(cliente.email)
            etDireccion.setText(cliente.direccion)
            etNotas.setText(cliente.notas)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (cliente == null) R.string.dialog_new_client else R.string.dialog_edit_client)
            .setView(dialogView)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nombre = etNombre.text.toString().trim()
                if (nombre.isEmpty()) {
                    etNombre.error = context.getString(R.string.dialog_required_field)
                    return@setOnClickListener
                }
                val record = ClientEntity(
                    id = cliente?.id ?: 0L,
                    nombre = nombre,
                    telefono = etTelefono.text.toString().trim(),
                    email = etEmail.text.toString().trim(),
                    direccion = etDireccion.text.toString().trim(),
                    notas = etNotas.text.toString().trim()
                )
                fragment.lifecycleScope.launch {
                    val dao = AppDatabase.getInstance(context).clientDao()
                    withContext(Dispatchers.IO) {
                        if (cliente == null) dao.insert(record) else dao.update(record)
                    }
                    onSaved(record)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}
