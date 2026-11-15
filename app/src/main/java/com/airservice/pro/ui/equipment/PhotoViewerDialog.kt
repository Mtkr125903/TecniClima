package com.airservice.pro.ui.equipment

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import coil.load
import com.airservice.pro.databinding.DialogFotoBinding

/** Vista ampliada de una foto. */
class PhotoViewerDialog : DialogFragment() {

    private var uri: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        uri = arguments?.getString(ARG_URI)
        val binding = DialogFotoBinding.inflate(layoutInflater)
        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            binding.ivFoto.load(uri?.let { android.net.Uri.parse(it) })
            binding.btnCerrar.setOnClickListener { dismiss() }
        }
    }

    companion object {
        private const val ARG_URI = "uri"

        fun newInstance(uri: String) = PhotoViewerDialog().apply {
            arguments = Bundle().apply { putString(ARG_URI, uri) }
        }
    }
}
