package com.airservice.pro.ui.receipt

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
import com.airservice.pro.data.ReceiptEntity
import com.airservice.pro.ui.common.UiHelpers.bindDropdown
import com.airservice.pro.util.FechaUtil
import com.airservice.pro.util.NumeroUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Formulario de alta / edicion de comprobantes. */
object ReceiptForm {

    private val METODOS = listOf("Efectivo", "Transferencia", "Tarjeta", "Crédito", "Otro")

    fun show(fragment: Fragment, comprobante: ReceiptEntity?, onSaved: (ReceiptEntity) -> Unit = {}) {
        val context = fragment.requireContext()
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.fragment_comprobante_detalle, null)

        val etCliente = dialogView.findViewById<AutoCompleteTextView>(R.id.etCliente)
        val etPresupuesto = dialogView.findViewById<AutoCompleteTextView>(R.id.etPresupuesto)
        val etEquipo = dialogView.findViewById<AutoCompleteTextView>(R.id.etEquipo)
        val etMonto = dialogView.findViewById<EditText>(R.id.etMonto)
        val etMetodoPago = dialogView.findViewById<AutoCompleteTextView>(R.id.etMetodoPago)
        val etObservacion = dialogView.findViewById<EditText>(R.id.etObservacion)
        val etDetalle = dialogView.findViewById<EditText>(R.id.etDetalle)

                                var clienteSeleccionado: ClientEntity? = null
        var equipoSeleccionado: EquipmentEntity? = null
        var presupuestoSeleccionado: BudgetEntity? = null

        etMetodoPago.bindDropdown(METODOS, { it }) { metodo ->
            etMetodoPago.setText(metodo.orEmpty(), false)
        }
        etMetodoPago.setText(comprobante?.metodoPago ?: METODOS.first(), false)
        comprobante?.let {
            etMonto.setText(NumeroUtil.aNumero(it.monto).let { n ->
                if (n == 0.0) it.monto else NumeroUtil.normalizar(it.monto)
            })
            etObservacion.setText(it.observacion)
            etDetalle.setText(it.detalle)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (comprobante == null) R.string.dialog_new_receipt else R.string.action_edit)
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
                    val numero = comprobante?.numero ?: withContext(Dispatchers.IO) {
                        db.counterDao().siguienteNumero("comprobante", "REC-", 4)
                    }
                    val record = ReceiptEntity(
                        id = comprobante?.id ?: 0L,
                        clientId = cliente.id,
                        equipmentId = equipoSeleccionado?.id ?: 0L,
                        budgetId = presupuestoSeleccionado?.id ?: 0L,
                        numero = numero,
                        monto = etMonto.text.toString().trim(),
                        observacion = etObservacion.text.toString().trim(),
                        detalle = etDetalle.text.toString().trim(),
                        estado = comprobante?.estado ?: "Pendiente",
                        fecha = comprobante?.fecha ?: FechaUtil.ahoraIso(),
                        metodoPago = etMetodoPago.text.toString().trim(),
                        pdfPath = comprobante?.pdfPath.orEmpty()
                    )
                    withContext(Dispatchers.IO) {
                        if (comprobante == null) {
                            db.receiptDao().insert(record)
                        } else {
                            db.receiptDao().update(record)
                        }
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
            val presupuestos = withContext(Dispatchers.IO) { db.budgetDao().getAll() }

            etCliente.bindDropdown(clientes, { it.nombre }) { elegido ->
                clienteSeleccionado = elegido
                val equiposCliente = equipos.filter { it.clientId == elegido?.id }
                etEquipo.bindDropdown(equiposCliente, { equipo ->
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
                val presupuestosCliente = presupuestos.filter { it.clientId == elegido?.id }
                etPresupuesto.bindDropdown(presupuestosCliente, { it.numero }) { presupuesto ->
                    presupuestoSeleccionado = presupuesto
                    etPresupuesto.setText(presupuesto?.numero.orEmpty(), false)
                    presupuesto?.let {
                        etMonto.setText(NumeroUtil.aNumero(it.monto).toString())
                    }
                }
            }

            if (comprobante != null) {
                val clientePrevio = clientes.firstOrNull { it.id == comprobante.clientId }
                clienteSeleccionado = clientePrevio
                etCliente.setText(clientePrevio?.nombre.orEmpty(), false)

                val equipoPrevio = equipos.firstOrNull { it.id == comprobante.equipmentId }
                equipoSeleccionado = equipoPrevio
                etEquipo.setText(
                    equipoPrevio?.let {
                        listOf(it.brand, it.model).filter { b -> b.isNotBlank() }.joinToString(" ")
                    }.orEmpty(),
                    false
                )

                val presupuestoPrevio = presupuestos.firstOrNull { it.id == comprobante.budgetId }
                presupuestoSeleccionado = presupuestoPrevio
                etPresupuesto.setText(presupuestoPrevio?.numero.orEmpty(), false)
            }
        }
    }
}
