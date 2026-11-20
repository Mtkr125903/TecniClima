package com.airservice.pro.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.BudgetEntity
import com.airservice.pro.data.PresupuestoRow
import com.airservice.pro.databinding.FragmentPresupuestosBinding
import com.airservice.pro.ui.common.UiHelpers
import com.airservice.pro.util.FechaUtil
import com.airservice.pro.util.NumeroUtil
import com.airservice.pro.util.PdfGenerator
import com.airservice.pro.util.PdfStorage
import com.airservice.pro.util.ShareHelper
import com.airservice.pro.util.WhatsAppHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PresupuestosFragment : Fragment() {

    private var _binding: FragmentPresupuestosBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PresupuestosAdapter
    private val dao by lazy { AppDatabase.getInstance(requireContext()).budgetDao() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPresupuestosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PresupuestosAdapter(
            onClick = { abrirOpciones(it) },
            onAprobar = { cambiarEstado(it, "Aprobado", R.string.msg_budget_approved) },
            onRechazar = { cambiarEstado(it, "Rechazado", R.string.msg_budget_rejected) },
            onWhatsApp = { enviarPorWhatsApp(it) },
            onPdf = { generarPdf(it) }
        )
        binding.rvPresupuestos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPresupuestos.adapter = adapter

        binding.fabAgregar.setOnClickListener { abrirEditor(0L) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.observeAll().collect { lista ->
                    adapter.submitList(lista)
                    UiHelpers.emptyState(binding.tvEmpty, lista.isEmpty())
                }
            }
        }
    }

    private fun abrirOpciones(row: PresupuestoRow) {
        val presupuesto = row.presupuesto
        val opciones = arrayOf(
            getString(R.string.action_edit),
            getString(R.string.action_delete)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(presupuesto.numero)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirEditor(presupuesto.id)
                    1 -> confirmarEliminar(presupuesto)
                }
            }
            .show()
    }

    private fun abrirEditor(budgetId: Long) {
        findNavController().navigate(
            R.id.budgetEditFragment,
            bundleOf(BudgetEditFragment.ARG_BUDGET_ID to budgetId)
        )
    }

    private fun cambiarEstado(row: PresupuestoRow, estado: String, mensaje: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val actualizado = row.presupuesto.copy(estado = estado)
            withContext(Dispatchers.IO) { dao.update(actualizado) }
            Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun confirmarEliminar(presupuesto: BudgetEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, presupuesto.numero))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { dao.delete(presupuesto) }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun enviarPorWhatsApp(row: PresupuestoRow) {
        val presupuesto = row.presupuesto
        val cuerpo = getString(
            R.string.whatsapp_budget_body,
            row.clienteNombre,
            presupuesto.numero,
            NumeroUtil.moneda(presupuesto.monto)
        )
        if (!WhatsAppHelper.enviarWhatsApp(requireContext(), row.clienteTelefono, cuerpo)) {
            Snackbar.make(binding.root, R.string.msg_whatsapp_error, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun generarPdf(row: PresupuestoRow) {
        val presupuesto = row.presupuesto
        viewLifecycleOwner.lifecycleScope.launch {
            val contexto = requireContext()
            val doc = PdfGenerator.Documento(
                titulo = getString(R.string.pdf_title_budget),
                numero = presupuesto.numero,
                cliente = row.clienteNombre,
                equipo = row.equipoDescripcion,
                tipo = presupuesto.descripcion,
                tecnico = "",
                fecha = FechaUtil.mostrar(presupuesto.fecha),
                detalle = presupuesto.detalle,
                observacion = "",
                monto = presupuesto.monto
            )
            val destino = PdfStorage.archivo(contexto, "presupuesto", presupuesto.numero)
            val ok = withContext(Dispatchers.IO) {
                runCatching { PdfGenerator.generarPresupuesto(doc, destino) }.isSuccess
            }
            if (ok) {
                Snackbar.make(binding.root, R.string.msg_pdf_generated, Snackbar.LENGTH_SHORT).show()
                ShareHelper.compartirPdf(contexto, destino)
            } else {
                Snackbar.make(binding.root, R.string.msg_pdf_error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
