package com.airservice.pro.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.ClientEntity
import com.airservice.pro.databinding.FragmentClienteBinding
import com.airservice.pro.ui.common.UiHelpers
import com.airservice.pro.util.WhatsAppHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class ClientFragment : Fragment() {

    private var _binding: FragmentClienteBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ClientAdapter
    private val dao by lazy { AppDatabase.getInstance(requireContext()).clientDao() }
    private val busqueda = MutableStateFlow("")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ClientAdapter(
            onClick = { abrirDetalle(it) },
            onCall = { WhatsAppHelper.abrirLlamada(requireContext(), it.telefono) },
            onWhatsApp = {
                WhatsAppHelper.enviarWhatsApp(
                    requireContext(),
                    it.telefono,
                    getString(R.string.whatsapp_generic_body, it.nombre)
                )
            }
        )
        binding.rvClientes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvClientes.adapter = adapter

        binding.fabAgregar.setOnClickListener { ClientForm.show(this, null) }
        binding.etSearch.doAfterTextChanged { busqueda.value = it?.toString().orEmpty() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                busqueda
                    .debounce(150)
                    .flatMapLatest { q ->
                        if (q.isBlank()) dao.observeAll() else dao.search(q)
                    }
                    .collect { lista ->
                        adapter.submitList(lista)
                        UiHelpers.emptyState(binding.tvEmpty, lista.isEmpty())
                    }
            }
        }
    }

    private fun abrirDetalle(cliente: ClientEntity) {
        findNavController().navigate(
            R.id.action_clientes_to_detalle,
            bundleOf("clientId" to cliente.id)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
