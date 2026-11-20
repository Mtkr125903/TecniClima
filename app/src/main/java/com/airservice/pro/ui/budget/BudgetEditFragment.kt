package com.airservice.pro.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.BudgetChargeEntity
import com.airservice.pro.data.BudgetEntity
import com.airservice.pro.data.BudgetItemEntity
import com.airservice.pro.data.CatalogItemEntity
import com.airservice.pro.data.ChargeCatalogEntity
import com.airservice.pro.data.ClientEntity
import com.airservice.pro.data.EquipmentEntity
import com.airservice.pro.databinding.DialogBudgetChargeBinding
import com.airservice.pro.databinding.DialogBudgetItemBinding
import com.airservice.pro.databinding.FragmentBudgetEditBinding
import com.airservice.pro.databinding.ItemBudgetEditRecargoBinding
import com.airservice.pro.databinding.ItemBudgetEditRenglonBinding
import com.airservice.pro.ui.common.UiHelpers.bindDropdown
import com.airservice.pro.util.FechaUtil
import com.airservice.pro.util.NumeroUtil
import com.airservice.pro.util.PresupuestoCalculador
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Alta y edicion de un presupuesto: renglones desde el catalogo, recargos,
 * condiciones comerciales y total calculado.
 *
 * Si no hay renglones el presupuesto funciona como antes (monto a mano).
 */
class BudgetEditFragment : Fragment() {

    private var _binding: FragmentBudgetEditBinding? = null
    private val binding get() = _binding!!

    private val db by lazy { AppDatabase.getInstance(requireContext()) }

    private var presupuesto: BudgetEntity? = null
    private var clienteSeleccionado: ClientEntity? = null
    private var equipoSeleccionado: EquipmentEntity? = null
    private var plantillaSeleccionada: com.airservice.pro.data.BudgetTemplateEntity? = null

    private val items = mutableListOf<LineaItem>()
    private val recargos = mutableListOf<LineaRecargo>()

    private var clientes: List<ClientEntity> = emptyList()
    private var equipos: List<EquipmentEntity> = emptyList()
    private var catalogo: List<CatalogItemEntity> = emptyList()
    private var catalogoRecargos: List<ChargeCatalogEntity> = emptyList()
    private var plantillas: List<com.airservice.pro.data.BudgetTemplateEntity> = emptyList()

    private data class LineaItem(val concepto: String, val cantidad: Double, val precio: Double)

    private data class LineaRecargo(
        val nombre: String,
        val modo: String,
        val valor: Double,
        val temporada: Boolean
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarEstado()
        configurarListeners()
        cargarDatos()
    }

    // ------------------------------------------------------------------
    // Carga y precarga del formulario
    // ------------------------------------------------------------------

    private fun configurarEstado() {
        val estados = listOf(
            "Pendiente de aprobación",
            "Aprobado",
            "Rechazado",
            "Enviado",
            "Cancelado"
        )
        binding.etEstado.bindDropdown(estados, { it }) { estado ->
            binding.etEstado.setText(estado.orEmpty(), false)
        }
        binding.etEstado.setText(estados.first(), false)
    }

    private fun configurarListeners() {
        binding.btnAgregarItem.setOnClickListener { dialogoItem(null) }
        binding.btnAgregarRecargo.setOnClickListener { dialogoRecargo() }
        binding.btnAplicarPlantilla.setOnClickListener { aplicarPlantilla() }
        binding.btnGuardar.setOnClickListener { guardar() }
        binding.etMonto.setOnFocusChangeListener { _, _ -> recalcular() }
    }

    private fun cargarDatos() {
        val args = arguments
        val budgetId = args?.getLong(ARG_BUDGET_ID) ?: 0L
        val clientIdInicial = args?.getLong(ARG_CLIENT_ID) ?: 0L
        val equipmentIdInicial = args?.getLong(ARG_EQUIPMENT_ID) ?: 0L

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                clientes = db.clientDao().getAll()
                equipos = db.equipmentDao().getAll()
                catalogo = db.catalogDao().getActivos()
                catalogoRecargos = db.chargeCatalogDao().getActivos()
                plantillas = db.budgetTemplateDao().getActivos()
                if (budgetId != 0L) {
                    presupuesto = db.budgetDao().getById(budgetId)
                    items.clear()
                    db.budgetItemDao().getByBudget(budgetId).forEach {
                        items += LineaItem(it.concepto, it.cantidad, NumeroUtil.aNumero(it.precio))
                    }
                    recargos.clear()
                    db.budgetChargeDao().getByBudget(budgetId).forEach { cargo ->
                        recargos += LineaRecargo(
                            nombre = cargo.nombre,
                            modo = cargo.modo,
                            valor = NumeroUtil.aNumero(cargo.valor),
                            temporada = catalogoRecargos.firstOrNull { it.nombre == cargo.nombre }?.temporada == true
                        )
                    }
                }
            }

            bindCliente(clientIdInicial)
            bindPlantillas()
            precargar()
            render()
        }
    }

    private fun bindCliente(clientIdInicial: Long) {
        binding.etCliente.bindDropdown(clientes, { it.nombre }) { elegido ->
            clienteSeleccionado = elegido
            binding.etCliente.setText(elegido?.nombre.orEmpty(), false)
            val filtrados = equipos.filter { it.clientId == elegido?.id }
            bindEquipo(filtrados)
        }

        val inicial = presupuesto?.clientId?.takeIf { it != 0L } ?: clientIdInicial
        val cliente = clientes.firstOrNull { it.id == inicial }
        if (cliente != null) {
            clienteSeleccionado = cliente
            binding.etCliente.setText(cliente.nombre, false)
            bindEquipo(equipos.filter { it.clientId == cliente.id })
        } else {
            bindEquipo(emptyList())
        }
    }

    private fun bindEquipo(lista: List<EquipmentEntity>) {
        binding.etEquipo.bindDropdown(lista, { descripcionEquipo(it) }) { equipo ->
            equipoSeleccionado = equipo
            binding.etEquipo.setText(equipo?.let { descripcionEquipo(it) }.orEmpty(), false)
        }
        if (lista.isEmpty()) {
            equipoSeleccionado = null
            binding.etEquipo.setText("", false)
        }
    }

    private fun bindPlantillas() {
        binding.etPlantilla.bindDropdown(plantillas, { it.nombre }) { plantilla ->
            plantillaSeleccionada = plantilla
            binding.etPlantilla.setText(plantilla?.nombre.orEmpty(), false)
        }
    }

    private fun precargar() {
        val actual = presupuesto ?: return
        binding.etDescripcion.setText(actual.descripcion)
        binding.etDetalle.setText(actual.detalle)
        binding.etEstado.setText(actual.estado, false)
        binding.etValidezDias.setText(actual.validezDias.toString())
        binding.etSena.setText(actual.sena)
        binding.etGarantia.setText(actual.garantia)
        binding.etIncluye.setText(actual.incluye)
        binding.etNoIncluye.setText(actual.noIncluye)
        binding.etMonto.setText(NumeroUtil.normalizar(actual.monto))

        val equipo = equipos.firstOrNull { it.id == actual.equipmentId }
        if (equipo != null) {
            equipoSeleccionado = equipo
            binding.etEquipo.setText(descripcionEquipo(equipo), false)
        }
    }

    // ------------------------------------------------------------------
    // Renglones y recargos
    // ------------------------------------------------------------------

    private fun dialogoItem(existente: LineaItem?) {
        val vista = DialogBudgetItemBinding.inflate(layoutInflater)
        val sugerencias = catalogo.map { it.concepto }
        vista.etConceptoItem.setAdapter(
            ArrayAdapter(requireContext(), R.layout.item_dropdown, sugerencias)
        )
        vista.etConceptoItem.setOnItemClickListener { _, _, position, _ ->
            catalogo.getOrNull(position)?.let { vista.etPrecioItem.setText(it.precio) }
        }

        existente?.let {
            vista.etConceptoItem.setText(it.concepto, false)
            vista.etCantidadItem.setText(formatearCantidad(it.cantidad))
            vista.etPrecioItem.setText(NumeroUtil.normalizar(it.precio.toString()))
        } ?: vista.etCantidadItem.setText("1")

        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existente == null) R.string.dialog_add_item else R.string.dialog_edit_item)
            .setView(vista.root)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val concepto = vista.etConceptoItem.text.toString().trim()
                val cantidad = NumeroUtil.aNumero(vista.etCantidadItem.text.toString())
                val precio = NumeroUtil.aNumero(vista.etPrecioItem.text.toString())
                if (concepto.isBlank() || cantidad <= 0.0 || precio < 0.0) {
                    Snackbar.make(binding.root, R.string.msg_item_required, Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val nueva = LineaItem(concepto, cantidad, precio)
                if (existente == null) {
                    items += nueva
                } else {
                    items[items.indexOf(existente)] = nueva
                }
                render()
                dialogo.dismiss()
            }
        }
        dialogo.show()
    }

    private fun dialogoRecargo() {
        val vista = DialogBudgetChargeBinding.inflate(layoutInflater)
        var elegido: ChargeCatalogEntity? = null

        vista.etRecargoNombre.bindDropdown(catalogoRecargos, { it.nombre }) { recargo ->
            elegido = recargo
            vista.etRecargoNombre.setText(recargo?.nombre.orEmpty(), false)
            vista.tvRecargoModo.text = getString(
                if (recargo?.modo == PresupuestoCalculador.MODO_MONTO) R.string.charge_mode_amount
                else R.string.charge_mode_percentage
            )
            vista.etRecargoValor.setText(recargo?.valor.orEmpty())
        }

        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_add_charge)
            .setView(vista.root)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val recargo = elegido
                val valor = NumeroUtil.aNumero(vista.etRecargoValor.text.toString())
                if (recargo == null || valor <= 0.0) {
                    Snackbar.make(binding.root, R.string.msg_charge_required, Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                recargos += LineaRecargo(recargo.nombre, recargo.modo, valor, recargo.temporada)
                render()
                dialogo.dismiss()
            }
        }
        dialogo.show()
    }

    private fun aplicarPlantilla() {
        val plantilla = plantillaSeleccionada
        if (plantilla == null) {
            Snackbar.make(binding.root, R.string.msg_select_template, Snackbar.LENGTH_SHORT).show()
            return
        }

        fun cargar() {
            viewLifecycleOwner.lifecycleScope.launch {
                val renglones = withContext(Dispatchers.IO) {
                    db.budgetTemplateDao().getItems(plantilla.id)
                }
                items.clear()
                renglones.forEach {
                    items += LineaItem(it.concepto, it.cantidad, NumeroUtil.aNumero(it.precio))
                }
                if (binding.etDescripcion.text.isNullOrBlank()) {
                    binding.etDescripcion.setText(plantilla.descripcion)
                }
                render()
                Snackbar.make(binding.root, R.string.msg_template_applied, Snackbar.LENGTH_SHORT).show()
            }
        }

        if (items.isEmpty()) {
            cargar()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.label_templates)
                .setMessage(R.string.msg_replace_items)
                .setPositiveButton(R.string.action_apply_template) { _, _ -> cargar() }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    }

    // ------------------------------------------------------------------
    // Render y totales
    // ------------------------------------------------------------------

    private fun render() {
        renderItems()
        renderRecargos()
        recalcular()
    }

    private fun renderItems() {
        binding.containerItems.removeAllViews()
        items.forEach { item ->
            val fila = ItemBudgetEditRenglonBinding.inflate(layoutInflater, binding.containerItems, false)
            fila.tvConceptoItem.text = item.concepto
            fila.tvDetalleItem.text = getString(
                R.string.item_detail_format,
                formatearCantidad(item.cantidad),
                NumeroUtil.moneda(item.precio.toString())
            )
            fila.tvSubtotalItem.text = NumeroUtil.moneda((item.cantidad * item.precio).toString())
            fila.btnBorrarItem.setOnClickListener {
                items.remove(item)
                render()
            }
            binding.containerItems.addView(fila.root)
        }
        binding.tvSinItems.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun renderRecargos() {
        binding.containerRecargos.removeAllViews()
        val subtotal = items.sumOf { it.cantidad * it.precio }
        recargos.forEach { recargo ->
            val fila = ItemBudgetEditRecargoBinding.inflate(layoutInflater, binding.containerRecargos, false)
            fila.tvNombreRecargo.text = recargo.nombre
            fila.tvDetalleRecargo.text = describirRecargo(recargo)
            fila.tvImporteRecargo.text = NumeroUtil.moneda(importeRecargo(recargo, subtotal).toString())
            fila.btnBorrarRecargo.setOnClickListener {
                recargos.remove(recargo)
                render()
            }
            binding.containerRecargos.addView(fila.root)
        }
    }

    private fun recalcular() {
        val lleno = items.isNotEmpty()
        binding.etMonto.isEnabled = !lleno
        binding.tvAyudaMonto.text = if (lleno) {
            getString(R.string.msg_amount_auto)
        } else {
            getString(R.string.label_valid_until, fechaVencimiento())
        }

        val resultado = PresupuestoCalculador.calcular(
            items.map { PresupuestoCalculador.Item(it.concepto, it.cantidad, it.precio) },
            recargos.map {
                PresupuestoCalculador.Charge(
                    it.nombre,
                    it.modo,
                    it.valor,
                    importeRecargo(it, items.sumOf { i -> i.cantidad * i.precio })
                )
            }
        )

        binding.tvSubtotal.text = NumeroUtil.moneda(resultado.subtotal.toString())
        binding.tvRecargos.text = NumeroUtil.moneda(resultado.totalRecargos.toString())

        val total = if (lleno) resultado.total else NumeroUtil.aNumero(binding.etMonto.text.toString())
        binding.tvTotal.text = NumeroUtil.moneda(total.toString())
        if (lleno) binding.etMonto.setText(NumeroUtil.normalizar(total.toString()))
    }

    private fun importeRecargo(recargo: LineaRecargo, subtotal: Double): Double =
        PresupuestoCalculador.importeRecargo(subtotal, recargo.modo, recargo.valor, recargo.temporada)

    private fun describirRecargo(recargo: LineaRecargo): String = if (recargo.modo == PresupuestoCalculador.MODO_MONTO) {
        NumeroUtil.moneda(recargo.valor.toString())
    } else {
        getString(R.string.charge_percentage_format, formatearCantidad(recargo.valor))
    }

    private fun fechaVencimiento(): String {
        val dias = binding.etValidezDias.text.toString().trim().toIntOrNull() ?: 15
        return FechaUtil.mostrar(FechaUtil.sumarDias(FechaUtil.hoyIso(), dias))
    }

    // ------------------------------------------------------------------
    // Guardar
    // ------------------------------------------------------------------

    private fun guardar() {
        val cliente = clienteSeleccionado
        if (cliente == null) {
            Snackbar.make(binding.root, R.string.msg_no_client_selected, Snackbar.LENGTH_SHORT).show()
            return
        }

        val lleno = items.isNotEmpty()
        val validezDias = binding.etValidezDias.text.toString().trim().toIntOrNull() ?: 15
        val monto = if (lleno) {
            val subtotal = items.sumOf { it.cantidad * it.precio }
            subtotal + recargos.sumOf { importeRecargo(it, subtotal) }
        } else {
            NumeroUtil.aNumero(binding.etMonto.text.toString())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val contexto = requireContext()
            val guardado = withContext(Dispatchers.IO) {
                val numero = presupuesto?.numero
                    ?: db.counterDao().siguienteNumero("presupuesto", "PRE-", 4)

                val registro = BudgetEntity(
                    id = presupuesto?.id ?: 0L,
                    clientId = cliente.id,
                    equipmentId = equipoSeleccionado?.id ?: 0L,
                    numero = numero,
                    monto = NumeroUtil.normalizar(monto.toString()),
                    descripcion = binding.etDescripcion.text.toString().trim(),
                    detalle = binding.etDetalle.text.toString().trim(),
                    estado = binding.etEstado.text.toString().trim().ifBlank { "Pendiente de aprobación" },
                    fecha = presupuesto?.fecha ?: FechaUtil.ahoraIso(),
                    validezDias = validezDias,
                    vence = FechaUtil.sumarDias(FechaUtil.hoyIso(), validezDias),
                    sena = binding.etSena.text.toString().trim(),
                    garantia = binding.etGarantia.text.toString().trim(),
                    incluye = binding.etIncluye.text.toString().trim(),
                    noIncluye = binding.etNoIncluye.text.toString().trim()
                )

                val id = if (presupuesto == null) {
                    db.budgetDao().insert(registro)
                } else {
                    db.budgetDao().update(registro)
                    registro.id
                }

                db.budgetItemDao().deleteByBudget(id)
                items.forEachIndexed { indice, item ->
                    db.budgetItemDao().insert(
                        BudgetItemEntity(
                            budgetId = id,
                            concepto = item.concepto,
                            cantidad = item.cantidad,
                            precio = NumeroUtil.normalizar(item.precio.toString()),
                            subtotal = NumeroUtil.normalizar((item.cantidad * item.precio).toString()),
                            orden = indice
                        )
                    )
                }

                val subtotal = items.sumOf { it.cantidad * it.precio }
                db.budgetChargeDao().deleteByBudget(id)
                recargos.forEachIndexed { indice, recargo ->
                    db.budgetChargeDao().insert(
                        BudgetChargeEntity(
                            budgetId = id,
                            nombre = recargo.nombre,
                            modo = recargo.modo,
                            valor = NumeroUtil.normalizar(recargo.valor.toString()),
                            importe = NumeroUtil.normalizar(importeRecargo(recargo, subtotal).toString()),
                            orden = indice
                        )
                    )
                }
                true
            }

            if (guardado) {
                Snackbar.make(binding.root, R.string.msg_budget_saved, Snackbar.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Snackbar.make(binding.root, R.string.msg_error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private fun descripcionEquipo(equipo: EquipmentEntity): String {
        val base = listOf(equipo.brand, equipo.model).filter { it.isNotBlank() }.joinToString(" ")
        return base.ifBlank { equipo.type }
    }

    private fun formatearCantidad(valor: Double): String =
        if (valor == valor.toLong().toDouble()) valor.toLong().toString() else valor.toString()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_BUDGET_ID = "budgetId"
        const val ARG_CLIENT_ID = "clientId"
        const val ARG_EQUIPMENT_ID = "equipmentId"
    }
}