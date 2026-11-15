package com.airservice.pro.ui.technician

import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.TechnicianEntity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Formulario de alta / edicion de tecnicos. */
object TechnicianForm {

    fun show(fragment: Fragment, tecnico: TechnicianEntity?, onSaved: (TechnicianEntity) -> Unit = {}) {
        val context = fragment.requireContext()
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.fragment_tecnico_detalle, null)

        val etNombre = dialogView.findViewById<EditText>(R.id.etNombre)
        val etTelefono = dialogView.findViewById<EditText>(R.id.etTelefono)
        val etEspecialidad = dialogView.findViewById<EditText>(R.id.etEspecialidad)
        val swActivo = dialogView.findViewById<MaterialSwitch>(R.id.swActivo)

        if (tecnico != null) {
            etNombre.setText(tecnico.nombre)
            etTelefono.setText(tecnico.telefono)
            etEspecialidad.setText(tecnico.especialidad)
            swActivo.isChecked = tecnico.activo
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (tecnico == null) R.string.dialog_new_technician else R.string.action_edit)
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
                val record = TechnicianEntity(
                    id = tecnico?.id ?: 0L,
                    nombre = nombre,
                    telefono = etTelefono.text.toString().trim(),
                    especialidad = etEspecialidad.text.toString().trim(),
                    activo = swActivo.isChecked
                )
                fragment.lifecycleScope.launch {
                    val dao = AppDatabase.getInstance(context).technicianDao()
                    withContext(Dispatchers.IO) {
                        if (tecnico == null) dao.insert(record) else dao.update(record)
                    }
                    onSaved(record)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}
