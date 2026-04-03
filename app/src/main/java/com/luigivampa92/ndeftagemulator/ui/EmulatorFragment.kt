package com.luigivampa92.ndeftagemulator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.luigivampa92.ndefemulation.NdefEmulation
import com.luigivampa92.ndefemulation.ndef.MultiRecordNdefData
import com.luigivampa92.ndefemulation.ndef.NdefRecordData
import com.luigivampa92.ndefemulation.ndef.TextNdefData
import com.luigivampa92.ndefemulation.ndef.UriNdefData
import com.luigivampa92.ndeftagemulator.NdefTagEmulatorApp
import com.luigivampa92.ndeftagemulator.R
import com.luigivampa92.ndeftagemulator.data.SavedTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmulatorFragment : Fragment() {

    private lateinit var ndefEmulation: NdefEmulation
    private lateinit var radioGroup: RadioGroup
    private lateinit var textInputLayout: TextInputLayout
    private lateinit var textInput: TextInputEditText
    private lateinit var savedTagSpinner: Spinner
    private lateinit var statusText: TextView

    private var savedTags: List<SavedTag> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_emulator, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ndefEmulation = NdefEmulation(requireContext())

        radioGroup = view.findViewById(R.id.radio_group_type)
        textInputLayout = view.findViewById(R.id.text_input_layout)
        textInput = view.findViewById(R.id.text_input)
        savedTagSpinner = view.findViewById(R.id.saved_tag_spinner)
        statusText = view.findViewById(R.id.status_text)

        val applyButton = view.findViewById<MaterialButton>(R.id.btn_apply)
        applyButton.setOnClickListener { applyEmulation() }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_uri -> {
                    textInputLayout.visibility = View.VISIBLE
                    textInputLayout.hint = "Enter URI"
                    textInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_URI
                    savedTagSpinner.visibility = View.GONE
                }
                R.id.radio_text -> {
                    textInputLayout.visibility = View.VISIBLE
                    textInputLayout.hint = "Enter text"
                    textInput.inputType = android.text.InputType.TYPE_CLASS_TEXT
                    savedTagSpinner.visibility = View.GONE
                }
                R.id.radio_saved_tag -> {
                    textInputLayout.visibility = View.GONE
                    savedTagSpinner.visibility = View.VISIBLE
                }
            }
        }

        val app = requireActivity().application as NdefTagEmulatorApp
        app.database.tagDao().getAllTags().observe(viewLifecycleOwner) { tags ->
            savedTags = tags
            updateSpinner()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateSpinner() {
        if (!isAdded) return
        val names = if (savedTags.isEmpty()) listOf("(no saved tags)") else savedTags.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        savedTagSpinner.adapter = adapter
    }

    fun applyEmulation() {
        if (!isAdded || view == null) {
            (activity as? MainActivity)?.updateEmulationSwitch(false)
            return
        }
        when (radioGroup.checkedRadioButtonId) {
            R.id.radio_uri -> {
                val uri = textInput.text?.toString()?.trim()
                if (uri.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Please enter a URI", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.updateEmulationSwitch(false)
                    return
                }
                ndefEmulation.currentEmulatedNdefData = UriNdefData(uri)
                (activity as? MainActivity)?.updateEmulationSwitch(true)
                Toast.makeText(requireContext(), "Emulating URI", Toast.LENGTH_SHORT).show()
            }
            R.id.radio_text -> {
                val text = textInput.text?.toString()?.trim()
                if (text.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Please enter text", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.updateEmulationSwitch(false)
                    return
                }
                ndefEmulation.currentEmulatedNdefData = TextNdefData(text)
                (activity as? MainActivity)?.updateEmulationSwitch(true)
                Toast.makeText(requireContext(), "Emulating text", Toast.LENGTH_SHORT).show()
            }
            R.id.radio_saved_tag -> {
                val position = savedTagSpinner.selectedItemPosition
                if (position < 0 || savedTags.isEmpty()) {
                    Toast.makeText(requireContext(), "No saved tag selected", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.updateEmulationSwitch(false)
                    return
                }
                emulateSelectedTag(savedTags[position].id)
            }
        }
        updateStatus()
    }

    private fun emulateSelectedTag(tagId: Long) {
        val app = requireActivity().application as NdefTagEmulatorApp
        viewLifecycleOwner.lifecycleScope.launch {
            val tagWithRecords = withContext(Dispatchers.IO) {
                app.database.tagDao().getTagWithRecords(tagId)
            }
            if (tagWithRecords != null && tagWithRecords.records.isNotEmpty()) {
                val ndefRecords = tagWithRecords.records.map { r ->
                    NdefRecordData(r.tnf, r.type, r.recordId, r.payload)
                }
                ndefEmulation.currentEmulatedNdefData = if (ndefRecords.size == 1) {
                    ndefRecords.first()
                } else {
                    MultiRecordNdefData(ndefRecords)
                }
                (activity as? MainActivity)?.updateEmulationSwitch(true)
                Toast.makeText(requireContext(), "Emulating: ${tagWithRecords.tag.name}", Toast.LENGTH_SHORT).show()
            } else {
                (activity as? MainActivity)?.updateEmulationSwitch(false)
                Toast.makeText(requireContext(), "Tag has no records", Toast.LENGTH_SHORT).show()
            }
            updateStatus()
        }
    }

    fun refreshStatus() {
        if (isAdded && view != null) updateStatus()
    }

    private fun updateStatus() {
        if (!isAdded || view == null) return
        val data = ndefEmulation.currentEmulatedNdefData
        statusText.text = if (data != null) {
            when (data) {
                is UriNdefData -> "Emulating URI: ${data.uri}"
                is TextNdefData -> "Emulating text: ${data.text}"
                is MultiRecordNdefData -> "Emulating tag (${data.records.size} records)"
                else -> "Emulating tag"
            }
        } else {
            "Not emulating"
        }
    }
}
