package com.luigivampa92.ndeftagemulator.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_tag_records",
    foreignKeys = [ForeignKey(
        entity = SavedTag::class,
        parentColumns = ["id"],
        childColumns = ["tag_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tag_id")]
)
data class SavedTagRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "tag_id")
    val tagId: Long,
    val tnf: Short,
    val type: ByteArray,
    @ColumnInfo(name = "record_id")
    val recordId: ByteArray,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SavedTagRecord
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
