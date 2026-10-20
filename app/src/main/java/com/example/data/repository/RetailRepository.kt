package com.example.data.repository

import com.example.data.local.CustomerDao
import com.example.data.local.ProductDao
import com.example.data.local.SaleDao
import com.example.data.model.Customer
import com.example.data.model.DuePayment
import com.example.data.model.Product
import com.example.data.model.Sale
import kotlinx.coroutines.flow.Flow

class RetailRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val customerDao: CustomerDao
) {
    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)
    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)

    // Sales
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()

    suspend fun completeSale(sale: Sale): Long {
        val saleId = saleDao.insertSale(sale)

        // Adjust inventory stock
        sale.productId?.let { prodId ->
            productDao.adjustStock(prodId, -sale.quantity)
        }

        // Adjust customer due balance if applicable
        if (sale.customerId != null && sale.dueAmount > 0) {
            customerDao.adjustCustomerDue(sale.customerId, sale.dueAmount)
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
                productDao.adjustStock(oldSale.productId, oldSale.quantity)
                productDao.adjustStock(updatedSale.productId, -updatedSale.quantity)
            }
        } else if (oldSale.productId != null && updatedSale.productId == null) {
            productDao.adjustStock(oldSale.productId, oldSale.quantity)
        } else if (oldSale.productId == null && updatedSale.productId != null) {
            productDao.adjustStock(updatedSale.productId, -updatedSale.quantity)
        }

        // Adjust customer due difference
        val oldDue = oldSale.dueAmount
        val newDue = updatedSale.dueAmount
        if (oldSale.customerId != null && updatedSale.customerId != null && oldSale.customerId == updatedSale.customerId) {
            val dueDiff = newDue - oldDue
            if (dueDiff != 0.0) {
                customerDao.adjustCustomerDue(oldSale.customerId, dueDiff)
            }
        } else {
            if (oldSale.customerId != null && oldDue > 0) {
                customerDao.adjustCustomerDue(oldSale.customerId, -oldDue)
            }
            if (updatedSale.customerId != null && newDue > 0) {
                customerDao.adjustCustomerDue(updatedSale.customerId, newDue)
            }
        }
    }

    suspend fun deleteSale(sale: Sale) {
        saleDao.deleteSale(sale)

        // Restore inventory stock count
        sale.productId?.let { prodId ->
            productDao.adjustStock(prodId, sale.quantity)
        }

        // Deduct due from customer if it was a due sale
        if (sale.customerId != null && sale.dueAmount > 0) {
            customerDao.adjustCustomerDue(sale.customerId, -sale.dueAmount)
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

    // Customers & Due Khata
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val customersWithDue: Flow<List<Customer>> = customerDao.getCustomersWithDue()
    val totalDueAmount: Flow<Double?> = customerDao.getTotalDue()

    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)
    suspend fun getCustomerById(id: Long): Customer? = customerDao.getCustomerById(id)

    suspend fun recordDueCollection(
        customerId: Long,
        customerName: String,
        amount: Double,
        note: String,
        paymentMethod: String
    ): Long {
        val payment = DuePayment(
            customerId = customerId,
            customerName = customerName,
            amount = amount,
            note = note,
            paymentMethod = paymentMethod,
            paymentDate = System.currentTimeMillis()
        )
        val id = customerDao.insertDuePayment(payment)
        // Deduct amount from customer's due
        customerDao.adjustCustomerDue(customerId, -amount)
        return id
    }

    fun getDuePaymentsForCustomer(customerId: Long): Flow<List<DuePayment>> =
        customerDao.getDuePaymentsForCustomer(customerId)

    val allDuePayments: Flow<List<DuePayment>> = customerDao.getAllDuePayments()

    // Backup & Restore
    suspend fun getAllProductsList(): List<Product> = productDao.getAllProductsList()
    suspend fun getAllSalesList(): List<Sale> = saleDao.getAllSalesList()
    suspend fun getAllCustomersList(): List<Customer> = customerDao.getAllCustomersList()
    suspend fun getAllDuePaymentsList(): List<DuePayment> = customerDao.getAllDuePaymentsList()

    suspend fun restoreDatabase(
        products: List<Product>,
        sales: List<Sale>,
        customers: List<Customer>,
        duePayments: List<DuePayment>,
        replaceAll: Boolean
    ) {
        if (replaceAll) {
            saleDao.deleteAllSales()
            customerDao.deleteAllDuePayments()
            customerDao.deleteAllCustomers()
            productDao.deleteAllProducts()
        }
        if (products.isNotEmpty()) {
            productDao.insertAll(products)
        }
        if (customers.isNotEmpty()) {
            customerDao.insertAllCustomers(customers)
        }
        if (sales.isNotEmpty()) {
            saleDao.insertAll(sales)
        }
        if (duePayments.isNotEmpty()) {
            customerDao.insertAllDuePayments(duePayments)
        }
    }
}
