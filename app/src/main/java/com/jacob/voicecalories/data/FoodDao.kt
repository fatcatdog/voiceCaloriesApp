package com.jacob.voicecalories.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query(
        "SELECT * FROM food_entries " +
            "WHERE timestamp >= :startMillis AND timestamp < :endMillis " +
            "ORDER BY timestamp ASC"
    )
    fun entriesBetween(startMillis: Long, endMillis: Long): Flow<List<FoodEntry>>

    @Query(
        "SELECT * FROM food_entries " +
            "WHERE timestamp >= :startMillis AND timestamp < :endMillis " +
            "ORDER BY timestamp ASC"
    )
    suspend fun getEntriesBetween(startMillis: Long, endMillis: Long): List<FoodEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<FoodEntry>): List<Long>

    @Update
    suspend fun update(entry: FoodEntry)

    @Delete
    suspend fun delete(entry: FoodEntry)

    @Query("DELETE FROM food_entries WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM food_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FoodEntry?
}
