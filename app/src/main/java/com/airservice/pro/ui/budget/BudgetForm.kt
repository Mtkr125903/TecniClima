package com.airservice.pro.ui.budget

import android.view.LayoutInflater
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.BudgetEntity
import com.airservice.pro.data.ClientEntity
import com.airservice.pro.data.EquipmentEntity
import com.airservice.pro.ui.common.UiHelpers.bindDropdown
import com.airservice.pro.util.FechaUtil
import com.airservice.pro.util.NumeroUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Formulario de alta / edicion de presupuestos. */
object BudgetForm {

    private val ESTADOS = listOf(
        "Pendiente de aprobación",
        "Aprobado",
        "Rechazado",
        "Enviado",
        "Cancelado"
    )

    fun show(fragment: Fragment, presupuesto: BudgetEntity?, onSaved: (BudgetEntity) -> Unit = {}) {
        val context = fragment.requireContext()
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.fragment_presupuesto_detalle, null)

        val etCliente = dialogView.findViewById<AutoCompleteTextView>(R.id.etCliente)
        val etEquipo = dialogView.findViewById<AutoCompleteTextView>(R.id.etEquipo)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcion)
        val etDetalle = dialogView.findViewById<EditText>(R.id.etDetalle)
        val etMonto = dialogView.findViewById<EditText>(R.id.etMonto)
        val etEstado = dialogView.findViewById<AutoCompleteTextView>(R.id.etEstado)

                        var clienteSeleccionado: ClientEntity? = null
        var equipoSeleccionado: EquipmentEntity? = null

        etEstado.bindDropdown(ESTADOS, { it }) { estado ->
            etEstado.setText(estado.orEmpty(), false)
        }
        etEstado.setText(presupuesto?.estado ?: ESTADOS.first(), false)
        presupuesto?.let {
            etDescripcion.setText(it.descripcion)
            etDetalle.setText(it.detalle)
            etMonto.setText(NumeroUtil.aNumero(it.monto).let { n ->
                if (n == 0.0) it.monto else NumeroUtil.normalizar(it.monto)
            })
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (presupuesto == null) R.string.dialog_new_budget else R.string.dialog_edit_budget)
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
                fragment.lifecycleScope.launch {
                    val db = AppDatabase.getInstance(context)
                    val numero = presupuesto?.numero ?: withContext(Dispatchers.IO) {
                        db.counterDao().siguienteNumero("presupuesto", "PRE-", 4)
                    }
                    val record = BudgetEntity(
                        id = presupuesto?.id ?: 0L,
                        clientId = cliente.id,
                        equipmentId = equipoSeleccionado?.id ?: 0L,
                        numero = numero,
                        monto = etMonto.text.toString().trim(),
                        descripcion = etDescripcion.text.toString().trim(),
                        detalle = etDetalle.text.toString().trim(),
                        estado = etEstado.text.toString().trim(),
                        fecha = presupuesto?.fecha ?: FechaUtil.ahoraIso()
                    )
                    withContext(Dispatchers.IO) {
                        if (presupuesto == null) db.budgetDao().insert(record) else db.budgetDao().update(record)
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

            if (presupuesto != null) {
                val clientePrevio = clientes.firstOrNull { it.id == presupuesto.clientId }
                clienteSeleccionado = clientePrevio
                etCliente.setText(clientePrevio?.nombre.orEmpty(), false)

                val equipoPrevio = equipos.firstOrNull { it.id == presupuesto.equipmentId }
                equipoSeleccionado = equipoPrevio
                etEquipo.setText(
                    equipoPrevio?.let {
                        listOf(it.brand, it.model).filter { b -> b.isNotBlank() }.joinToString(" ")
                    }.orEmpty(),
                    false
                )
            }
        }
    }
}
