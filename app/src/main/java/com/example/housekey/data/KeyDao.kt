package com.example.housekey.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyDao {

    @Query("SELECT * FROM keys ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<KeyEntity>>

    @Query("SELECT * FROM keys WHERE id = :id")
    suspend fun getById(id: Long): KeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: KeyEntity): Long

    @Update
    suspend fun update(key: KeyEntity)

    @Delete
    suspend fun delete(key: KeyEntity)

    @Query("DELETE FROM keys WHERE id = :id")
    suspend fun deleteById(id: Long)
}
