package com.airservice.pro.ui.equipment

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
import com.airservice.pro.ui.common.UiHelpers.bindDropdown
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Formulario de alta / edicion de equipos. */
object EquipmentForm {

    fun show(
        fragment: Fragment,
        equipo: EquipmentEntity?,
        clienteInicial: ClientEntity? = null,
        onSaved: (EquipmentEntity) -> Unit = {}
    ) {
        val context = fragment.requireContext()
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.fragment_equipo_detalle, null)

        val etCliente = dialogView.findViewById<AutoCompleteTextView>(R.id.etCliente)
        val etType = dialogView.findViewById<EditText>(R.id.etType)
        val etBrand = dialogView.findViewById<EditText>(R.id.etBrand)
        val etModel = dialogView.findViewById<EditText>(R.id.etModel)
        val etSerial = dialogView.findViewById<EditText>(R.id.etSerial)
        val etInstall = dialogView.findViewById<EditText>(R.id.etInstall)
        val etCapW = dialogView.findViewById<EditText>(R.id.etCapW)
        val etCapBtu = dialogView.findViewById<EditText>(R.id.etCapBtu)
        val etGas = dialogView.findViewById<EditText>(R.id.etGas)
        val etLocation = dialogView.findViewById<EditText>(R.id.etLocation)
        val etDireccion = dialogView.findViewById<EditText>(R.id.etDireccion)

        // En edicion se precargan los datos actuales para no perderlos al guardar.
        equipo?.let {
            etType.setText(it.type)
            etBrand.setText(it.brand)
            etModel.setText(it.model)
            etSerial.setText(it.serial)
            etInstall.setText(it.installDate)
            etCapW.setText(it.capacityW)
            etCapBtu.setText(it.capacityBtu)
            etGas.setText(it.gas)
            etLocation.setText(it.location)
            etDireccion.setText(it.direccion)
        }

        var clienteSeleccionado: ClientEntity? = null

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (equipo == null) R.string.dialog_new_equipment else R.string.dialog_edit_equipment)
            .setView(dialogView)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val marca = etBrand.text.toString().trim()
                if (marca.isEmpty()) {
                    etBrand.error = context.getString(R.string.dialog_required_field)
                    return@setOnClickListener
                }
                val record = EquipmentEntity(
                    id = equipo?.id ?: 0L,
                    clientId = clienteSeleccionado?.id ?: equipo?.clientId ?: 0L,
                    type = etType.text.toString().trim(),
                    brand = marca,
                    model = etModel.text.toString().trim(),
                    serial = etSerial.text.toString().trim(),
                    installDate = etInstall.text.toString().trim(),
                    capacityW = etCapW.text.toString().trim(),
                    capacityBtu = etCapBtu.text.toString().trim(),
                    gas = etGas.text.toString().trim(),
                    location = etLocation.text.toString().trim(),
                    direccion = etDireccion.text.toString().trim()
                )
                fragment.lifecycleScope.launch {
                    val dao = AppDatabase.getInstance(context).equipmentDao()
                    withContext(Dispatchers.IO) {
                        if (equipo == null) dao.insert(record) else dao.update(record)
                    }
                    onSaved(record)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()

        // Los clientes se cargan en segundo plano para no bloquear el dialogo.
        fragment.lifecycleScope.launch {
            val clientes = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(context).clientDao().getAll()
            }
            etCliente.bindDropdown(clientes, { it.nombre }) { elegido ->
                clienteSeleccionado = elegido
                elegido?.let { etDireccion.setText(it.direccion) }
            }
            if (equipo != null) {
                val previo = clientes.firstOrNull { it.id == equipo.clientId }
                clienteSeleccionado = previo
                etCliente.setText(previo?.nombre.orEmpty(), false)
            } else if (clienteInicial != null) {
                val previo = clientes.firstOrNull { it.id == clienteInicial.id } ?: clienteInicial
                clienteSeleccionado = previo
                etCliente.setText(previo.nombre, false)
                etDireccion.setText(previo.direccion)
            }
        }
    }
}
