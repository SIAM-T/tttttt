package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "due_payments")
data class DuePayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val amount: Double,
    val note: String = "",
    val paymentMethod: String = "Cash",
    val paymentDate: Long = System.currentTimeMillis()
)
