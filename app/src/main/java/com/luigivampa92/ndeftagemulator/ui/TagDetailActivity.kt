package com.luigivampa92.ndeftagemulator.ui

import android.nfc.NdefRecord
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.luigivampa92.ndeftagemulator.NdefTagEmulatorApp
import com.luigivampa92.ndeftagemulator.R
import com.luigivampa92.ndeftagemulator.data.SavedTagRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TAG_ID = "tag_id"
        const val EXTRA_TAG_NAME = "tag_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = intent.getStringExtra(EXTRA_TAG_NAME) ?: "Tag"
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.records_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val tagId = intent.getLongExtra(EXTRA_TAG_ID, -1)
        if (tagId == -1L) { finish(); return }

        val app = application as NdefTagEmulatorApp
        lifecycleScope.launch {
            val tagWithRecords = withContext(Dispatchers.IO) {
                app.database.tagDao().getTagWithRecords(tagId)
            }
            tagWithRecords?.let { recyclerView.adapter = SavedRecordAdapter(it.records) }
        }
    }
}

class SavedRecordAdapter(private val records: List<SavedTagRecord>) :
    RecyclerView.Adapter<SavedRecordAdapter.ViewHolder>() {

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
        holder.typeText.text = "Type: ${tryDecodeType(record)}"
        holder.payloadText.text = "Payload: ${formatPayload(record)}"
    }

    override fun getItemCount() = records.size

    private fun tryDecodeType(record: SavedTagRecord): String {
        if (record.type.isEmpty()) return "(none)"
        return try {
            val str = String(record.type, Charsets.US_ASCII)
            if (str.all { it in ' '..'~' }) str
            else record.type.joinToString("") { "%02X".format(it) }
        } catch (_: Exception) {
            record.type.joinToString("") { "%02X".format(it) }
        }
    }

    private fun tnfToString(tnf: Short): String = when (tnf) {
        NdefRecord.TNF_EMPTY -> "Empty"
        NdefRecord.TNF_WELL_KNOWN -> "Well-Known"
        NdefRecord.TNF_MIME_MEDIA -> "MIME Media"
        NdefRecord.TNF_ABSOLUTE_URI -> "Absolute URI"
        NdefRecord.TNF_EXTERNAL_TYPE -> "External Type"
        NdefRecord.TNF_UNKNOWN -> "Unknown"
        else -> "0x${tnf.toString(16)}"
    }
}
