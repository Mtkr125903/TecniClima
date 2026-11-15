package com.airservice.pro.ui.agenda

import android.view.LayoutInflater
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.ClientEntity
import com.airservice.pro.data.TechnicianEntity
import com.airservice.pro.data.TurnEntity
import com.airservice.pro.ui.common.UiHelpers.bindDropdown
import com.airservice.pro.util.FechaUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Formulario de alta / edicion de turnos de agenda. */
object TurnForm {

    private val ESTADOS = listOf("Pendiente", "Confirmado", "Completado", "Cancelado")

    fun show(fragment: Fragment, turno: TurnEntity?, onSaved: (TurnEntity) -> Unit = {}) {
        val context = fragment.requireContext()
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.fragment_turno_detalle, null)

        val etCliente = dialogView.findViewById<AutoCompleteTextView>(R.id.etCliente)
        val etTecnico = dialogView.findViewById<AutoCompleteTextView>(R.id.etTecnico)
        val etTitulo = dialogView.findViewById<EditText>(R.id.etTitulo)
        val etFecha = dialogView.findViewById<EditText>(R.id.etFecha)
        val etHora = dialogView.findViewById<EditText>(R.id.etHora)
        val etDireccion = dialogView.findViewById<EditText>(R.id.etDireccion)
        val etEstado = dialogView.findViewById<AutoCompleteTextView>(R.id.etEstado)
        val etNotas = dialogView.findViewById<EditText>(R.id.etNotas)

                        var clienteSeleccionado: ClientEntity? = null
        var tecnicoSeleccionado: TechnicianEntity? = null

        etEstado.bindDropdown(ESTADOS, { it }) { estado ->
            etEstado.setText(estado.orEmpty(), false)
        }
        etEstado.setText(turno?.estado ?: ESTADOS.first(), false)
        etFecha.setText(turno?.fecha ?: FechaUtil.hoyIso())
        etHora.setText(turno?.hora ?: "09:00")
        turno?.let {
            etTitulo.setText(it.titulo)
            etDireccion.setText(it.direccion)
            etNotas.setText(it.notas)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (turno == null) R.string.dialog_new_turn else R.string.action_edit)
            .setView(dialogView)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val titulo = etTitulo.text.toString().trim()
                if (titulo.isEmpty()) {
                    etTitulo.error = context.getString(R.string.dialog_required_field)
                    return@setOnClickListener
                }
                val record = TurnEntity(
                    id = turno?.id ?: 0L,
                    clientId = clienteSeleccionado?.id ?: 0L,
                    technicianId = tecnicoSeleccionado?.id ?: 0L,
                    titulo = titulo,
                    fecha = etFecha.text.toString().trim().ifBlank { FechaUtil.hoyIso() },
                    hora = etHora.text.toString().trim().ifBlank { "09:00" },
                    direccion = etDireccion.text.toString().trim(),
                    estado = etEstado.text.toString().trim(),
                    notas = etNotas.text.toString().trim()
                )
                fragment.lifecycleScope.launch {
                    val dao = AppDatabase.getInstance(context).turnDao()
                    withContext(Dispatchers.IO) {
                        if (turno == null) dao.insert(record) else dao.update(record)
                    }
                    onSaved(record)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()

        fragment.lifecycleScope.launch {
            val db = AppDatabase.getInstance(context)
            val clientes = withContext(Dispatchers.IO) { db.clientDao().getAll() }
            val tecnicos = withContext(Dispatchers.IO) { db.technicianDao().getAll() }

            etCliente.bindDropdown(clientes, { it.nombre }) { elegido ->
                clienteSeleccionado = elegido
                etCliente.setText(elegido?.nombre.orEmpty(), false)
                elegido?.let { etDireccion.setText(it.direccion) }
            }
            etTecnico.bindDropdown(tecnicos, { it.nombre }) { elegido ->
                tecnicoSeleccionado = elegido
                etTecnico.setText(elegido?.nombre.orEmpty(), false)
            }

            if (turno != null) {
                val clientePrevio = clientes.firstOrNull { it.id == turno.clientId }
                clienteSeleccionado = clientePrevio
                etCliente.setText(clientePrevio?.nombre.orEmpty(), false)

                val tecnicoPrevio = tecnicos.firstOrNull { it.id == turno.technicianId }
                tecnicoSeleccionado = tecnicoPrevio
                etTecnico.setText(tecnicoPrevio?.nombre.orEmpty(), false)
            }
        }
    }
}
