package com.airservice.pro.ui.receipt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.ComprobanteRow
import com.airservice.pro.data.ReceiptEntity
import com.airservice.pro.databinding.FragmentComprobantesBinding
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

class ReceiptsFragment : Fragment() {

    private var _binding: FragmentComprobantesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReceiptsAdapter
    private val dao by lazy { AppDatabase.getInstance(requireContext()).receiptDao() }
    private var ultimaLista: List<ComprobanteRow> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComprobantesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ReceiptsAdapter(
            onClick = { abrirOpciones(it) },
            onWhatsApp = { enviarPorWhatsApp(it) },
            onPdf = { generarPdf(it) }
        )
        binding.rvComprobantes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComprobantes.adapter = adapter

        binding.fabAgregar.setOnClickListener { ReceiptForm.show(this, null) }
        binding.fabPdf.setOnClickListener { elegirParaPdf() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.observeAll().collect { lista ->
                    ultimaLista = lista
                    adapter.submitList(lista)
                    UiHelpers.emptyState(binding.tvEmpty, lista.isEmpty())
                }
            }
        }
    }

    private fun abrirOpciones(row: ComprobanteRow) {
        val comprobante = row.comprobante
        val opciones = arrayOf(
            getString(R.string.action_edit),
            getString(R.string.action_mark_paid),
            getString(R.string.action_mark_sent),
            getString(R.string.action_delete)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(comprobante.numero)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> ReceiptForm.show(this, comprobante)
                    1 -> cambiarEstado(comprobante, "Pagado", R.string.msg_receipt_paid)
                    2 -> cambiarEstado(comprobante, "Enviado", R.string.msg_receipt_sent)
                    3 -> confirmarEliminar(comprobante)
                }
            }
            .show()
    }

    private fun cambiarEstado(comprobante: ReceiptEntity, estado: String, mensaje: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val actualizado = comprobante.copy(estado = estado)
            withContext(Dispatchers.IO) { dao.update(actualizado) }
            Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun confirmarEliminar(comprobante: ReceiptEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, comprobante.numero))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { dao.delete(comprobante) }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun enviarPorWhatsApp(row: ComprobanteRow) {
        val comprobante = row.comprobante
        val cuerpo = getString(
            R.string.whatsapp_receipt_body,
            row.clienteNombre,
            comprobante.numero,
            NumeroUtil.moneda(comprobante.monto)
        )
        if (!WhatsAppHelper.enviarWhatsApp(requireContext(), row.clienteTelefono, cuerpo)) {
            Snackbar.make(binding.root, R.string.msg_whatsapp_error, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun elegirParaPdf() {
        if (ultimaLista.isEmpty()) {
            Snackbar.make(binding.root, R.string.empty_receipt, Snackbar.LENGTH_SHORT).show()
            return
        }
        val etiquetas = ultimaLista.map { "${it.comprobante.numero} · ${it.clienteNombre}" }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_pdf)
            .setItems(etiquetas) { _, which -> generarPdf(ultimaLista[which]) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun generarPdf(row: ComprobanteRow) {
        val comprobante = row.comprobante
        viewLifecycleOwner.lifecycleScope.launch {
            val contexto = requireContext()
            val db = AppDatabase.getInstance(contexto)
            val empresa = withContext(Dispatchers.IO) {
                db.companyProfileDao().get()
            }?.let {
                PdfGenerator.Empresa(
                    nombre = it.nombre,
                    matricula = it.matricula,
                    cuit = it.cuit,
                    telefono = it.telefono,
                    direccion = it.direccion
                )
            }
            val doc = PdfGenerator.Documento(
                titulo = getString(R.string.pdf_title_receipt),
                numero = comprobante.numero,
                cliente = row.clienteNombre,
                equipo = row.equipoDescripcion,
                tipo = comprobante.metodoPago,
                tecnico = "",
                fecha = FechaUtil.mostrar(comprobante.fecha),
                detalle = comprobante.detalle,
                observacion = comprobante.observacion,
                monto = comprobante.monto,
                empresa = empresa
            )
            val destino = PdfStorage.archivo(contexto, "comprobante", comprobante.numero)
            val ok = withContext(Dispatchers.IO) {
                runCatching { PdfGenerator.generarComprobante(doc, destino) }.isSuccess
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
