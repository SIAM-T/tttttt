package com.example.data.repository

import com.example.data.local.ProductDao
import com.example.data.local.SaleDao
import com.example.data.model.Product
import com.example.data.model.Sale
import kotlinx.coroutines.flow.Flow

class RetailRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()

    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)

    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)

    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)

    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)

    suspend fun completeSale(sale: Sale): Long {
        val saleId = saleDao.insertSale(sale)
        sale.productId?.let { prodId ->
            // Decrement inventory stock
            productDao.adjustStock(prodId, -sale.quantity)
        }
        return saleId
    }

    suspend fun updateSale(oldSale: Sale, updatedSale: Sale) {
        saleDao.updateSale(updatedSale)

        // Adjust stock difference
        if (oldSale.productId != null && updatedSale.productId != null) {
            if (oldSale.productId == updatedSale.productId) {
                val difference = oldSale.quantity - updatedSale.quantity
                if (difference != 0) {
                    productDao.adjustStock(oldSale.productId, difference)
                }
            } else {
                // Product changed: refund old product and decrement new product
                productDao.adjustStock(oldSale.productId, oldSale.quantity)
                productDao.adjustStock(updatedSale.productId, -updatedSale.quantity)
            }
        } else if (oldSale.productId != null && updatedSale.productId == null) {
            productDao.adjustStock(oldSale.productId, oldSale.quantity)
        } else if (oldSale.productId == null && updatedSale.productId != null) {
            productDao.adjustStock(updatedSale.productId, -updatedSale.quantity)
        }
    }

    suspend fun deleteSale(sale: Sale) {
        saleDao.deleteSale(sale)
        sale.productId?.let { prodId ->
            // Restore inventory stock count
            productDao.adjustStock(prodId, sale.quantity)
        }
    }

    fun getSalesBetween(start: Long, end: Long): Flow<List<Sale>> =
        saleDao.getSalesBetween(start, end)

    fun getRevenueBetween(start: Long, end: Long): Flow<Double?> =
        saleDao.getRevenueBetween(start, end)

    fun getQuantitySoldBetween(start: Long, end: Long): Flow<Int?> =
        saleDao.getQuantitySoldBetween(start, end)

    fun getAllTimeRevenue(): Flow<Double?> = saleDao.getAllTimeRevenue()

    fun getAllTimeQuantitySold(): Flow<Int?> = saleDao.getAllTimeQuantitySold()
}
