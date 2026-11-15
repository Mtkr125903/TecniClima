package com.airservice.pro.ui.track

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
import com.airservice.pro.data.TechnicianEntity
import com.airservice.pro.data.WorkEntity
import com.airservice.pro.ui.common.UiHelpers.bindDropdown
import com.airservice.pro.util.FechaUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Formulario de alta / edicion de trabajos (seguimiento). */
object WorkForm {

    private val ESTADOS = listOf("Sin iniciar", "En proceso", "Completado", "Cancelado")

    fun show(fragment: Fragment, trabajo: WorkEntity?, onSaved: (WorkEntity) -> Unit = {}) {
        val context = fragment.requireContext()
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.fragment_trabajo_detalle, null)

        val etCliente = dialogView.findViewById<AutoCompleteTextView>(R.id.etCliente)
        val etEquipo = dialogView.findViewById<AutoCompleteTextView>(R.id.etEquipo)
        val etTecnico = dialogView.findViewById<AutoCompleteTextView>(R.id.etTecnico)
        val etTitulo = dialogView.findViewById<EditText>(R.id.etTitulo)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcion)
        val etEstado = dialogView.findViewById<AutoCompleteTextView>(R.id.etEstado)
        val sliderAvance = dialogView.findViewById<Slider>(R.id.sliderAvance)

                                var clienteSeleccionado: ClientEntity? = null
        var equipoSeleccionado: EquipmentEntity? = null
        var tecnicoSeleccionado: TechnicianEntity? = null

        etEstado.bindDropdown(ESTADOS, { it }) { estado ->
            etEstado.setText(estado.orEmpty(), false)
        }
        etEstado.setText(trabajo?.estado ?: ESTADOS.first(), false)
        sliderAvance.value = (trabajo?.avance ?: 0).coerceIn(0, 100).toFloat()
        trabajo?.let {
            etTitulo.setText(it.titulo)
            etDescripcion.setText(it.descripcion)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (trabajo == null) R.string.dialog_new_work else R.string.dialog_edit_work)
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
                val titulo = etTitulo.text.toString().trim()
                if (titulo.isEmpty()) {
                    etTitulo.error = context.getString(R.string.dialog_required_field)
                    return@setOnClickListener
                }
                val avance = sliderAvance.value.toInt()
                val record = WorkEntity(
                    id = trabajo?.id ?: 0L,
                    clientId = cliente.id,
                    equipmentId = equipoSeleccionado?.id ?: 0L,
                    technicianId = tecnicoSeleccionado?.id ?: 0L,
                    titulo = titulo,
                    descripcion = etDescripcion.text.toString().trim(),
                    avance = avance,
                    estado = if (avance >= 100) "Completado" else etEstado.text.toString().trim(),
                    fecha = trabajo?.fecha ?: FechaUtil.ahoraIso()
                )
                fragment.lifecycleScope.launch {
                    val dao = AppDatabase.getInstance(context).workDao()
                    withContext(Dispatchers.IO) {
                        if (trabajo == null) dao.insert(record) else dao.update(record)
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

            if (trabajo != null) {
                val clientePrevio = clientes.firstOrNull { it.id == trabajo.clientId }
                clienteSeleccionado = clientePrevio
                etCliente.setText(clientePrevio?.nombre.orEmpty(), false)

                val equipoPrevio = equipos.firstOrNull { it.id == trabajo.equipmentId }
                equipoSeleccionado = equipoPrevio
                etEquipo.setText(
                    equipoPrevio?.let {
                        listOf(it.brand, it.model).filter { b -> b.isNotBlank() }.joinToString(" ")
                    }.orEmpty(),
                    false
                )

                val tecnicoPrevio = tecnicos.firstOrNull { it.id == trabajo.technicianId }
                tecnicoSeleccionado = tecnicoPrevio
                etTecnico.setText(tecnicoPrevio?.nombre.orEmpty(), false)
            }
        }
    }
}
