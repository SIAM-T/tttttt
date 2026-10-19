package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Sale
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY saleTimestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE saleTimestamp >= :startTimestamp AND saleTimestamp <= :endTimestamp ORDER BY saleTimestamp DESC")
    fun getSalesBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY saleTimestamp DESC")
    fun getSalesByCustomer(customerId: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE isDue = 1 AND dueAmount > 0 ORDER BY saleTimestamp DESC")
    fun getDueSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): Sale?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sales: List<Sale>)

    @Update
    suspend fun updateSale(sale: Sale)

    @Delete
    suspend fun deleteSale(sale: Sale)

    @Query("SELECT SUM(totalAmount) FROM sales WHERE saleTimestamp >= :startTimestamp AND saleTimestamp <= :endTimestamp")
    fun getRevenueBetween(startTimestamp: Long, endTimestamp: Long): Flow<Double?>

    @Query("SELECT SUM(amountPaid) FROM sales WHERE saleTimestamp >= :startTimestamp AND saleTimestamp <= :endTimestamp")
    fun getCashCollectedBetween(startTimestamp: Long, endTimestamp: Long): Flow<Double?>

    @Query("SELECT SUM(dueAmount) FROM sales WHERE saleTimestamp >= :startTimestamp AND saleTimestamp <= :endTimestamp")
    fun getDueGivenBetween(startTimestamp: Long, endTimestamp: Long): Flow<Double?>

    @Query("SELECT SUM(quantity) FROM sales WHERE saleTimestamp >= :startTimestamp AND saleTimestamp <= :endTimestamp")
    fun getQuantitySoldBetween(startTimestamp: Long, endTimestamp: Long): Flow<Int?>

    @Query("SELECT SUM(totalAmount) FROM sales")
    fun getAllTimeRevenue(): Flow<Double?>

    @Query("SELECT SUM(quantity) FROM sales")
    fun getAllTimeQuantitySold(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun getSaleCount(): Int
}
