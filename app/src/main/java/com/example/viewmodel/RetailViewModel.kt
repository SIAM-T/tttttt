package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.repository.RetailRepository
import com.example.util.StorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

enum class TimeframeFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

data class DashboardSummary(
    val todayRevenue: Double = 0.0,
    val todayItemsSold: Int = 0,
    val todayTransactions: Int = 0,
    val weekRevenue: Double = 0.0,
    val weekItemsSold: Int = 0,
    val monthRevenue: Double = 0.0,
    val monthItemsSold: Int = 0,
    val allTimeRevenue: Double = 0.0,
    val allTimeItemsSold: Int = 0,
    val lowStockCount: Int = 0
)

class RetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RetailRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = RetailRepository(database.productDao(), database.saleDao())
    }

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Event flow for one-off alerts / snacks
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // --- Search & Filters ---
    private val _inventorySearch = MutableStateFlow("")
    val inventorySearch = _inventorySearch.asStateFlow()

    private val _inventorySizeFilter = MutableStateFlow("All")
    val inventorySizeFilter = _inventorySizeFilter.asStateFlow()

    private val _salesTimeframe = MutableStateFlow(TimeframeFilter.TODAY)
    val salesTimeframe = _salesTimeframe.asStateFlow()

    private val _salesSearch = MutableStateFlow("")
    val salesSearch = _salesSearch.asStateFlow()

    // --- POS State ---
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct = _selectedProduct.asStateFlow()

    private val _posQuantity = MutableStateFlow(1)
    val posQuantity = _posQuantity.asStateFlow()

    private val _posNotes = MutableStateFlow("")
    val posNotes = _posNotes.asStateFlow()

    private val _posReceiptPath = MutableStateFlow<String?>(null)
    val posReceiptPath = _posReceiptPath.asStateFlow()

    private val _posPaymentMethod = MutableStateFlow("Cash")
    val posPaymentMethod = _posPaymentMethod.asStateFlow()

    // --- Filtered Inventory Stream ---
    val filteredProducts: StateFlow<List<Product>> = combine(
        products,
        inventorySearch,
        inventorySizeFilter
    ) { prodList, query, sizeFilter ->
        prodList.filter { product ->
            val matchesQuery = query.isBlank() || product.name.contains(query, ignoreCase = true) ||
                    product.category.contains(query, ignoreCase = true)
            val matchesSize = sizeFilter == "All" || product.size.equals(sizeFilter, ignoreCase = true)
            matchesQuery && matchesSize
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Filtered Sales Stream ---
    val filteredSales: StateFlow<List<Sale>> = combine(
        sales,
        salesTimeframe,
        salesSearch
    ) { saleList, timeframe, query ->
        val now = System.currentTimeMillis()
        val startOfToday = getStartOfDay()
        val startOfWeek = getStartOfWeek()
        val startOfMonth = getStartOfMonth()

        saleList.filter { sale ->
            val inTimeframe = when (timeframe) {
                TimeframeFilter.TODAY -> sale.saleTimestamp >= startOfToday
                TimeframeFilter.THIS_WEEK -> sale.saleTimestamp >= startOfWeek
                TimeframeFilter.THIS_MONTH -> sale.saleTimestamp >= startOfMonth
                TimeframeFilter.ALL_TIME -> true
            }
            val matchesQuery = query.isBlank() ||
                    sale.productName.contains(query, ignoreCase = true) ||
                    sale.productSize.contains(query, ignoreCase = true) ||
                    sale.notes.contains(query, ignoreCase = true)
            inTimeframe && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dashboard Aggregations ---
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        products,
        sales
    ) { prodList, saleList ->
        val startOfToday = getStartOfDay()
        val startOfWeek = getStartOfWeek()
        val startOfMonth = getStartOfMonth()

        var todayRev = 0.0
        var todayItems = 0
        var todayTrans = 0

        var weekRev = 0.0
        var weekItems = 0

        var monthRev = 0.0
        var monthItems = 0

        var allRev = 0.0
        var allItems = 0

        for (sale in saleList) {
            allRev += sale.totalAmount
            allItems += sale.quantity

            if (sale.saleTimestamp >= startOfToday) {
                todayRev += sale.totalAmount
                todayItems += sale.quantity
                todayTrans++
            }
            if (sale.saleTimestamp >= startOfWeek) {
                weekRev += sale.totalAmount
                weekItems += sale.quantity
            }
            if (sale.saleTimestamp >= startOfMonth) {
                monthRev += sale.totalAmount
                monthItems += sale.quantity
            }
        }

        val lowStock = prodList.count { it.stockQuantity <= 5 }

        DashboardSummary(
            todayRevenue = todayRev,
            todayItemsSold = todayItems,
            todayTransactions = todayTrans,
            weekRevenue = weekRev,
            weekItemsSold = weekItems,
            monthRevenue = monthRev,
            monthItemsSold = monthItems,
            allTimeRevenue = allRev,
            allTimeItemsSold = allItems,
            lowStockCount = lowStock
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    // --- Top Selling Products ---
    val topSellingProducts: StateFlow<List<Pair<String, Int>>> = sales.combine(products) { saleList, _ ->
        saleList.groupBy { it.productName }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Actions: Inventory Management ---
    fun setInventorySearch(query: String) {
        _inventorySearch.value = query
    }

    fun setInventorySizeFilter(size: String) {
        _inventorySizeFilter.value = size
    }

    fun saveProduct(
        id: Long = 0,
        name: String,
        size: String,
        price: Double,
        costPrice: Double,
        stock: Int,
        category: String,
        imagePath: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val product = Product(
                id = id,
                name = name.trim(),
                size = size.trim(),
                price = price,
                costPrice = costPrice,
                stockQuantity = stock,
                category = category.trim().ifEmpty { "General" },
                imagePath = imagePath
            )
            if (id == 0L) {
                repository.insertProduct(product)
                _userMessage.emit("Product '${product.name}' added to inventory!")
            } else {
                repository.updateProduct(product)
                _userMessage.emit("Product '${product.name}' updated successfully!")
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProduct(product)
            StorageHelper.deleteFileIfExist(product.imagePath)
            _userMessage.emit("Product '${product.name}' deleted.")
        }
    }

    // --- Actions: POS ---
    fun selectProductForSale(product: Product) {
        _selectedProduct.value = product
        _posQuantity.value = 1
    }

    fun clearSelectedProduct() {
        _selectedProduct.value = null
        _posQuantity.value = 1
        _posNotes.value = ""
        _posReceiptPath.value = null
    }

    fun updatePosQuantity(qty: Int) {
        val bounded = qty.coerceAtLeast(1)
        _posQuantity.value = bounded
    }

    fun setPosNotes(notes: String) {
        _posNotes.value = notes
    }

    fun setPosPaymentMethod(method: String) {
        _posPaymentMethod.value = method
    }

    fun setPosReceiptPath(path: String?) {
        _posReceiptPath.value = path
    }

    fun completeSale(onSuccess: () -> Unit) {
        val product = _selectedProduct.value ?: return
        val qty = _posQuantity.value
        val total = product.price * qty

        viewModelScope.launch(Dispatchers.IO) {
            val sale = Sale(
                productId = product.id,
                productName = product.name,
                productSize = product.size,
                unitPrice = product.price,
                quantity = qty,
                totalAmount = total,
                notes = _posNotes.value.trim(),
                receiptImagePath = _posReceiptPath.value,
                paymentMethod = _posPaymentMethod.value,
                saleTimestamp = System.currentTimeMillis()
            )

            repository.completeSale(sale)
            _userMessage.emit("Sale completed! Recorded ${StorageHelper.formatCurrency(total)}")
            launch(Dispatchers.Main) {
                clearSelectedProduct()
                onSuccess()
            }
        }
    }

    // --- Actions: Sales History Management ---
    fun setSalesTimeframe(timeframe: TimeframeFilter) {
        _salesTimeframe.value = timeframe
    }

    fun setSalesSearch(query: String) {
        _salesSearch.value = query
    }

    fun updateSale(
        oldSale: Sale,
        newQuantity: Int,
        newUnitPrice: Double,
        newNotes: String,
        newReceiptPath: String?,
        newPaymentMethod: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedSale = oldSale.copy(
                quantity = newQuantity,
                unitPrice = newUnitPrice,
                totalAmount = newQuantity * newUnitPrice,
                notes = newNotes.trim(),
                receiptImagePath = newReceiptPath,
                paymentMethod = newPaymentMethod
            )
            repository.updateSale(oldSale, updatedSale)
            _userMessage.emit("Sale #${oldSale.id} updated! Daily income recalculated.")
        }
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSale(sale)
            StorageHelper.deleteFileIfExist(sale.receiptImagePath)
            _userMessage.emit("Sale #${sale.id} deleted. Restored ${sale.quantity} items to stock!")
        }
    }

    // --- Export Sales Report (100% Offline) ---
    fun exportSalesCsv(context: Context): Intent? {
        val currentSales = filteredSales.value
        if (currentSales.isEmpty()) return null

        try {
            val file = File(context.cacheDir, "sales_report_${System.currentTimeMillis()}.csv")
            FileOutputStream(file).bufferedWriter().use { writer ->
                writer.write("Sale ID,Date & Time,Product Name,Size,Unit Price,Quantity,Total Amount,Payment Method,Notes\n")
                for (s in currentSales) {
                    val dateStr = StorageHelper.formatDate(s.saleTimestamp).replace(",", " ")
                    val safeName = s.productName.replace(",", " ")
                    val safeNotes = s.notes.replace(",", " ").replace("\n", " ")
                    writer.write("${s.id},$dateStr,$safeName,${s.productSize},${s.unitPrice},${s.quantity},${s.totalAmount},${s.paymentMethod},$safeNotes\n")
                }
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            return Intent.createChooser(shareIntent, "Share Sales Report")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // --- Time helpers ---
    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
