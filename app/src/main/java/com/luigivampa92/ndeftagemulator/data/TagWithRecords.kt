package com.luigivampa92.ndeftagemulator.data

import androidx.room.Embedded
import androidx.room.Relation

data class TagWithRecords(
    @Embedded val tag: SavedTag,
    @Relation(parentColumn = "id", entityColumn = "tag_id")
    val records: List<SavedTagRecord>
)
