package com.luigivampa92.ndeftagemulator.ui

import android.nfc.NdefRecord
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.luigivampa92.ndeftagemulator.NdefTagEmulatorApp
import com.luigivampa92.ndeftagemulator.R
import com.luigivampa92.ndeftagemulator.data.SavedTag
import com.luigivampa92.ndeftagemulator.data.SavedTagRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderFragment : Fragment() {

    private lateinit var instructionText: TextView
    private lateinit var recordsRecyclerView: RecyclerView
    private lateinit var saveButton: MaterialButton

    private var currentRecords: List<NdefRecord> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_reader, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        instructionText = view.findViewById(R.id.instruction_text)
        recordsRecyclerView = view.findViewById(R.id.records_recycler_view)
        saveButton = view.findViewById(R.id.btn_save)
        recordsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        saveButton.setOnClickListener { showSaveDialog() }
    }

    fun displayRecords(records: List<NdefRecord>) {
        currentRecords = records
        if (!isAdded || view == null) return
        instructionText.visibility = View.GONE
        recordsRecyclerView.visibility = View.VISIBLE
        saveButton.visibility = View.VISIBLE
        recordsRecyclerView.adapter = RecordAdapter(records)
    }

    private fun showSaveDialog() {
        if (currentRecords.isEmpty()) return
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_tag, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.tag_name_input)
        AlertDialog.Builder(requireContext())
            .setTitle("Save Tag")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text?.toString()?.trim()
                if (name.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show()
                } else {
                    saveTag(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveTag(name: String) {
        val app = requireActivity().application as NdefTagEmulatorApp
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val tagId = app.database.tagDao().insertTag(SavedTag(name = name))
                val records = currentRecords.map { record ->
                    SavedTagRecord(
                        tagId = tagId,
                        tnf = record.tnf,
                        type = record.type ?: byteArrayOf(),
                        recordId = record.id ?: byteArrayOf(),
                        payload = record.payload ?: byteArrayOf()
                    )
                }
                app.database.tagDao().insertRecords(records)
            }
            Toast.makeText(requireContext(), "Tag saved as \"$name\"", Toast.LENGTH_SHORT).show()
        }
    }
}

class RecordAdapter(private val records: List<NdefRecord>) :
    RecyclerView.Adapter<RecordAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tnfText: TextView = view.findViewById(R.id.record_tnf)
        val typeText: TextView = view.findViewById(R.id.record_type)
        val payloadText: TextView = view.findViewById(R.id.record_payload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.tnfText.text = "TNF: ${tnfToString(record.tnf)}"
        holder.typeText.text = "Type: ${String(record.type ?: byteArrayOf())}"
        holder.payloadText.text = "Payload: ${formatPayload(record)}"
    }

    override fun getItemCount() = records.size
}

fun tnfToString(tnf: Short): String = when (tnf) {
    NdefRecord.TNF_EMPTY -> "Empty"
    NdefRecord.TNF_WELL_KNOWN -> "Well-Known"
    NdefRecord.TNF_MIME_MEDIA -> "MIME Media"
    NdefRecord.TNF_ABSOLUTE_URI -> "Absolute URI"
    NdefRecord.TNF_EXTERNAL_TYPE -> "External Type"
    NdefRecord.TNF_UNKNOWN -> "Unknown"
    else -> "0x${tnf.toString(16)}"
}

fun formatPayload(record: NdefRecord): String {
    val payload = record.payload ?: return "(empty)"
    if (payload.isEmpty()) return "(empty)"
    return try {
        when {
            record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                val langLen = payload[0].toInt() and 0x3F
                String(payload, langLen + 1, payload.size - langLen - 1, Charsets.UTF_8)
            }
            record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI) -> {
                record.toUri()?.toString() ?: payload.drop(1).toByteArray().toString(Charsets.UTF_8)
            }
            record.tnf == NdefRecord.TNF_MIME_MEDIA -> {
                String(payload, Charsets.UTF_8)
            }
            else -> payload.joinToString("") { "%02X".format(it) }
        }
    } catch (_: Exception) {
        payload.joinToString("") { "%02X".format(it) }
    }
}

fun formatPayload(record: SavedTagRecord): String {
    if (record.payload.isEmpty()) return "(empty)"
    return try {
        when {
            record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                val langLen = record.payload[0].toInt() and 0x3F
                String(record.payload, langLen + 1, record.payload.size - langLen - 1, Charsets.UTF_8)
            }
            record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI) -> {
                val ndefRecord = NdefRecord(record.tnf, record.type, record.recordId, record.payload)
                ndefRecord.toUri()?.toString()
                    ?: record.payload.drop(1).toByteArray().toString(Charsets.UTF_8)
            }
            record.tnf == NdefRecord.TNF_MIME_MEDIA -> {
                String(record.payload, Charsets.UTF_8)
            }
            else -> record.payload.joinToString("") { "%02X".format(it) }
        }
    } catch (_: Exception) {
        record.payload.joinToString("") { "%02X".format(it) }
    }
}
