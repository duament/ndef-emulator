package com.luigivampa92.ndeftagemulator.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TagDao {

    @Transaction
    @Query("SELECT * FROM saved_tags ORDER BY created_at DESC")
    fun getAllTagsWithRecords(): LiveData<List<TagWithRecords>>

    @Query("SELECT * FROM saved_tags ORDER BY created_at DESC")
    fun getAllTags(): LiveData<List<SavedTag>>

    @Query("SELECT * FROM saved_tags ORDER BY created_at DESC")
    suspend fun getAllTagsList(): List<SavedTag>

    @Insert
    suspend fun insertTag(tag: SavedTag): Long

    @Insert
    suspend fun insertRecords(records: List<SavedTagRecord>)

    @Query("UPDATE saved_tags SET name = :name WHERE id = :id")
    suspend fun renameTag(id: Long, name: String)

    @Query("DELETE FROM saved_tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Transaction
    @Query("SELECT * FROM saved_tags WHERE id = :id")
    suspend fun getTagWithRecords(id: Long): TagWithRecords?
}
