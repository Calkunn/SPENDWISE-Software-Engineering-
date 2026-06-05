package com.example.tugasmobile

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


@Dao
interface TransactionDao {
    @Query("SELECT * from transactions")
    fun getAll(): List<Transaction>

    @Insert
    fun insertAll(transactions: List<Transaction>)

    @Delete
    fun delete(transactions: List<Transaction>)

    @Update
    fun update(transactions: List<Transaction>)
}