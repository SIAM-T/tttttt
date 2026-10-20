package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Customer
import com.example.data.model.DuePayment
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY totalDue DESC, name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE totalDue > 0 ORDER BY totalDue DESC")
    fun getCustomersWithDue(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("UPDATE customers SET totalDue = totalDue + :delta WHERE id = :customerId")
    suspend fun adjustCustomerDue(customerId: Long, delta: Double)

    @Query("SELECT SUM(totalDue) FROM customers WHERE totalDue > 0")
    fun getTotalDue(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuePayment(payment: DuePayment): Long

    @Query("SELECT * FROM due_payments WHERE customerId = :customerId ORDER BY paymentDate DESC")
    fun getDuePaymentsForCustomer(customerId: Long): Flow<List<DuePayment>>

    @Query("SELECT * FROM due_payments ORDER BY paymentDate DESC")
    fun getAllDuePayments(): Flow<List<DuePayment>>

    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllCustomersList(): List<Customer>

    @Query("SELECT * FROM due_payments ORDER BY id ASC")
    suspend fun getAllDuePaymentsList(): List<DuePayment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomers(customers: List<Customer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDuePayments(payments: List<DuePayment>)

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    @Query("DELETE FROM due_payments")
    suspend fun deleteAllDuePayments()
}
