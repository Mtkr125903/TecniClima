package com.airservice.pro.ui.history

import android.view.LayoutInflater
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.ClientEntity
import com.airservice.pro.data.EquipmentEntity
import com.airservice.pro.data.ServiceEntity
import com.airservice.pro.data.TechnicianEntity
import com.airservice.pro.ui.common.UiHelpers.bindDropdown
import com.airservice.pro.util.FechaUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Formulario de alta / edicion de servicios. */
object ServiceForm {

    private val TIPOS = listOf(
        "Preventivo",
        "Reparación",
        "Fuga",
        "Limpieza",
        "Instalación"
    )

    private val ESTADOS = listOf(
        "Pendiente",
        "En proceso",
        "Completado",
        "Cancelado"
    )

    fun show(fragment: Fragment, servicio: ServiceEntity?, onSaved: (ServiceEntity) -> Unit = {}) {
        val context = fragment.requireContext()
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.fragment_servicio_detalle, null)

        val etCliente = dialogView.findViewById<AutoCompleteTextView>(R.id.etCliente)
        val etEquipo = dialogView.findViewById<AutoCompleteTextView>(R.id.etEquipo)
        val etTipo = dialogView.findViewById<AutoCompleteTextView>(R.id.etTipo)
        val etTecnico = dialogView.findViewById<AutoCompleteTextView>(R.id.etTecnico)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcion)
        val etEstado = dialogView.findViewById<AutoCompleteTextView>(R.id.etEstado)
        val etCosto = dialogView.findViewById<EditText>(R.id.etCosto)
        val etFecha = dialogView.findViewById<EditText>(R.id.etFecha)

                                var clienteSeleccionado: ClientEntity? = null
        var equipoSeleccionado: EquipmentEntity? = null
        var tecnicoSeleccionado: TechnicianEntity? = null

        etTipo.bindDropdown(TIPOS, { it }) { tipo ->
            etTipo.setText(tipo.orEmpty(), false)
        }
        etEstado.bindDropdown(ESTADOS, { it }) { estado ->
            etEstado.setText(estado.orEmpty(), false)
        }
        etTipo.setText(servicio?.tipo ?: TIPOS.first(), false)
        etEstado.setText(servicio?.estado ?: ESTADOS.first(), false)
        etFecha.setText(servicio?.fecha ?: FechaUtil.ahoraIso())
        servicio?.let {
            etDescripcion.setText(it.descripcion)
            etCosto.setText(it.costo)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (servicio == null) R.string.dialog_new_service else R.string.action_edit)
            .setView(dialogView)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val cliente = clienteSeleccionado
                if (cliente == null) {
                    etCliente.error = context.getString(R.string.msg_no_client_selected)
                    return@setOnClickListener
                }
                val record = ServiceEntity(
                    id = servicio?.id ?: 0L,
                    clientId = cliente.id,
                    equipmentId = equipoSeleccionado?.id ?: 0L,
                    tipo = etTipo.text.toString().trim(),
                    descripcion = etDescripcion.text.toString().trim(),
                    estado = etEstado.text.toString().trim(),
                    fecha = etFecha.text.toString().trim().ifBlank { FechaUtil.ahoraIso() },
                    technicianId = tecnicoSeleccionado?.id ?: 0L,
                    costo = etCosto.text.toString().trim()
                )
                fragment.lifecycleScope.launch {
                    val dao = AppDatabase.getInstance(context).serviceDao()
                    withContext(Dispatchers.IO) {
                        if (servicio == null) dao.insert(record) else dao.update(record)
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
            val equipos = withContext(Dispatchers.IO) { db.equipmentDao().getAll() }
            val tecnicos = withContext(Dispatchers.IO) { db.technicianDao().getAll() }

            etCliente.bindDropdown(clientes, { it.nombre }) { elegido ->
                clienteSeleccionado = elegido
                val filtrados = equipos.filter { it.clientId == elegido?.id }
                etEquipo.bindDropdown(filtrados, { equipo ->
                    listOf(equipo.brand, equipo.model).filter { it.isNotBlank() }.joinToString(" ")
                }) { equipo ->
                    equipoSeleccionado = equipo
                    etEquipo.setText(
                        equipo?.let {
                            listOf(it.brand, it.model).filter { b -> b.isNotBlank() }.joinToString(" ")
                        }.orEmpty(),
                        false
                    )
                }
            }

            etTecnico.bindDropdown(tecnicos, { it.nombre }) { elegido ->
                tecnicoSeleccionado = elegido
                etTecnico.setText(elegido?.nombre.orEmpty(), false)
            }

            if (servicio != null) {
                val clientePrevio = clientes.firstOrNull { it.id == servicio.clientId }
                clienteSeleccionado = clientePrevio
                etCliente.setText(clientePrevio?.nombre.orEmpty(), false)

                val equipoPrevio = equipos.firstOrNull { it.id == servicio.equipmentId }
                equipoSeleccionado = equipoPrevio
                etEquipo.setText(
                    equipoPrevio?.let {
                        listOf(it.brand, it.model).filter { b -> b.isNotBlank() }.joinToString(" ")
                    }.orEmpty(),
                    false
                )

                val tecnicoPrevio = tecnicos.firstOrNull { it.id == servicio.technicianId }
                tecnicoSeleccionado = tecnicoPrevio
                etTecnico.setText(tecnicoPrevio?.nombre.orEmpty(), false)
            }
        }
    }
}
