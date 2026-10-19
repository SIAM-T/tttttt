package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["customerId"]),
        Index(value = ["saleTimestamp"])
    ]
)
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long? = null,
    val productName: String,
    val productSize: String,
    val unitPrice: Double,
    val quantity: Int,
    val totalAmount: Double,
    val customerId: Long? = null,
    val customerName: String = "Walk-in Customer",
    val customerPhone: String = "",
    val paidAmount: Double = totalAmount,
    val dueAmount: Double = 0.0,
    val isDue: Boolean = false,
    val discount: Double = 0.0,
    val receiptImagePath: String? = null,
    val notes: String = "",
    val paymentMethod: String = "Cash",
    val saleTimestamp: Long = System.currentTimeMillis()
)
