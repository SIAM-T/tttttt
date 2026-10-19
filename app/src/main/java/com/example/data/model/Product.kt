package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val size: String,
    val price: Double,
    val costPrice: Double = 0.0,
    val stockQuantity: Int = 0,
    val imagePath: String? = null,
    val category: String = "Apparel",
    val barcode: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
