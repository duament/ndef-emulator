package com.luigivampa92.ndeftagemulator.ui

import android.content.Intent
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
import com.google.android.material.textfield.TextInputEditText
import com.luigivampa92.ndeftagemulator.NdefTagEmulatorApp
import com.luigivampa92.ndeftagemulator.R
import com.luigivampa92.ndeftagemulator.data.TagWithRecords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.tags_recycler_view)
        emptyText = view.findViewById(R.id.empty_text)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val app = requireActivity().application as NdefTagEmulatorApp
        app.database.tagDao().getAllTagsWithRecords().observe(viewLifecycleOwner) { tags ->
            if (tags.isEmpty()) {
                emptyText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                recyclerView.adapter = TagsAdapter(tags,
                    onRename = { showRenameDialog(it) },
                    onDelete = { showDeleteDialog(it) },
                    onClick = { openTagDetail(it) }
                )
            }
        }
    }

    private fun showRenameDialog(tag: TagWithRecords) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_tag, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.tag_name_input)
        nameInput.setText(tag.tag.name)
        AlertDialog.Builder(requireContext())
            .setTitle("Rename Tag")
            .setView(dialogView)
            .setPositiveButton("Rename") { _, _ ->
                val name = nameInput.text?.toString()?.trim()
                if (name.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show()
                } else {
                    renameTag(tag.tag.id, name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameTag(id: Long, name: String) {
        val app = requireActivity().application as NdefTagEmulatorApp
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { app.database.tagDao().renameTag(id, name) }
            Toast.makeText(requireContext(), "Tag renamed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteDialog(tag: TagWithRecords) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Tag")
            .setMessage("Delete \"${tag.tag.name}\"?")
            .setPositiveButton("Delete") { _, _ -> deleteTag(tag.tag.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTag(id: Long) {
        val app = requireActivity().application as NdefTagEmulatorApp
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { app.database.tagDao().deleteTag(id) }
            Toast.makeText(requireContext(), "Tag deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTagDetail(tag: TagWithRecords) {
        startActivity(Intent(requireContext(), TagDetailActivity::class.java).apply {
            putExtra(TagDetailActivity.EXTRA_TAG_ID, tag.tag.id)
            putExtra(TagDetailActivity.EXTRA_TAG_NAME, tag.tag.name)
        })
    }
}

class TagsAdapter(
    private val tags: List<TagWithRecords>,
    private val onRename: (TagWithRecords) -> Unit,
    private val onDelete: (TagWithRecords) -> Unit,
    private val onClick: (TagWithRecords) -> Unit
) : RecyclerView.Adapter<TagsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.tag_name)
        val infoText: TextView = view.findViewById(R.id.tag_info)
        val renameButton: View = view.findViewById(R.id.btn_rename)
        val deleteButton: View = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saved_tag, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tag = tags[position]
        holder.nameText.text = tag.tag.name
        holder.infoText.text = "${tag.records.size} record(s)"
        holder.renameButton.setOnClickListener { onRename(tag) }
        holder.deleteButton.setOnClickListener { onDelete(tag) }
        holder.itemView.setOnClickListener { onClick(tag) }
    }

    override fun getItemCount() = tags.size
}
