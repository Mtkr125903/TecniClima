package com.airservice.pro.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.CompanyProfileEntity
import com.airservice.pro.databinding.FragmentCompanyProfileBinding
import com.airservice.pro.util.PhotoStorage
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla de configuracion de los datos de la empresa.
 * Se usa una sola fila en la base de datos (id = 1).
 */
class CompanyProfileFragment : Fragment() {

    private var _binding: FragmentCompanyProfileBinding? = null
    private val binding get() = _binding!!

    private val db by lazy { AppDatabase.getInstance(requireContext()) }
    private var logoUriActual: String = ""

    private val seleccionarLogo = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val nuevo = PhotoStorage.copiarDesde(requireContext(), uri)
            if (nuevo != null) {
                if (logoUriActual.isNotBlank()) {
                    PhotoStorage.borrarArchivo(requireContext(), logoUriActual)
                }
                logoUriActual = nuevo.toString()
                binding.ivLogo.load(nuevo)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompanyProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarListeners()
        cargarDatos()
    }

    private fun configurarListeners() {
        binding.btnSeleccionarLogo.setOnClickListener {
            seleccionarLogo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnBorrarLogo.setOnClickListener {
            if (logoUriActual.isNotBlank()) {
                PhotoStorage.borrarArchivo(requireContext(), logoUriActual)
                logoUriActual = ""
                binding.ivLogo.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }
        binding.btnGuardar.setOnClickListener { guardar() }
    }

    private fun cargarDatos() {
        viewLifecycleOwner.lifecycleScope.launch {
            val perfil = withContext(Dispatchers.IO) {
                db.companyProfileDao().get()
            } ?: CompanyProfileEntity()

            binding.apply {
                etNombre.setText(perfil.nombre)
                etMatricula.setText(perfil.matricula)
                etCuit.setText(perfil.cuit)
                etTelefono.setText(perfil.telefono)
                etWhatsapp.setText(perfil.whatsapp)
                etAliasPago.setText(perfil.aliasPago)
                etDireccion.setText(perfil.direccion)
                etValidezDias.setText(perfil.validezDias.toString())
                logoUriActual = perfil.logoPath
                if (logoUriActual.isNotBlank()) {
                    ivLogo.load(Uri.parse(logoUriActual))
                }
            }
        }
    }

    private fun guardar() {
        val nombre = binding.etNombre.text.toString().trim()
        if (nombre.isBlank()) {
            binding.tilNombre.error = getString(R.string.dialog_required_field)
            return
        }

        val dias = binding.etValidezDias.text.toString().toIntOrNull() ?: 15

        viewLifecycleOwner.lifecycleScope.launch {
            val exito = withContext(Dispatchers.IO) {
                try {
                    db.companyProfileDao().upsert(
                        CompanyProfileEntity(
                            id = 1L,
                            nombre = nombre,
                            matricula = binding.etMatricula.text.toString().trim(),
                            cuit = binding.etCuit.text.toString().trim(),
                            telefono = binding.etTelefono.text.toString().trim(),
                            whatsapp = binding.etWhatsapp.text.toString().trim(),
                            aliasPago = binding.etAliasPago.text.toString().trim(),
                            direccion = binding.etDireccion.text.toString().trim(),
                            logoPath = logoUriActual,
                            validezDias = dias
                        )
                    )
                    true
                } catch (e: Exception) {
                    false
                }
            }

            if (exito) {
                Snackbar.make(binding.root, R.string.msg_profile_saved, Snackbar.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Snackbar.make(binding.root, R.string.msg_profile_error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
