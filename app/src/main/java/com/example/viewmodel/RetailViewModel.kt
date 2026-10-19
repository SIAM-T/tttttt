package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.DuePayment
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

enum class TimeframeFilter(val label: String, val banglaLabel: String) {
    TODAY("Today", "আজকের"),
    THIS_WEEK("This Week", "এই সপ্তাহের"),
    THIS_MONTH("This Month", "এই মাসের"),
    ALL_TIME("All Time", "সব সময়ের")
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
    val totalDueInMarket: Double = 0.0,
    val lowStockCount: Int = 0
)

class RetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RetailRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = RetailRepository(database.productDao(), database.saleDao(), database.customerDao())
    }

    // --- Reactive Data Streams ---
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customersWithDue: StateFlow<List<Customer>> = repository.customersWithDue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMarketDue: StateFlow<Double> = repository.totalDueAmount
        .combine(MutableStateFlow(0.0)) { due, _ -> due ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allDuePayments: StateFlow<List<DuePayment>> = repository.allDuePayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback messages
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // --- Search & Filters ---
    private val _inventorySearch = MutableStateFlow("")
    val inventorySearch = _inventorySearch.asStateFlow()

    private val _inventorySizeFilter = MutableStateFlow("All")
    val inventorySizeFilter = _inventorySizeFilter.asStateFlow()

    private val _customerSearch = MutableStateFlow("")
    val customerSearch = _customerSearch.asStateFlow()

    private val _salesTimeframe = MutableStateFlow(TimeframeFilter.TODAY)
    val salesTimeframe = _salesTimeframe.asStateFlow()

    private val _salesSearch = MutableStateFlow("")
    val salesSearch = _salesSearch.asStateFlow()

    // --- POS State (With / Without Customer, Cash / Due) ---
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct = _selectedProduct.asStateFlow()

    private val _posQuantity = MutableStateFlow(1)
    val posQuantity = _posQuantity.asStateFlow()

    private val _posSelectedCustomer = MutableStateFlow<Customer?>(null)
    val posSelectedCustomer = _posSelectedCustomer.asStateFlow()

    private val _posIsDue = MutableStateFlow(false)
    val posIsDue = _posIsDue.asStateFlow()

    private val _posPaidAmountText = MutableStateFlow("")
    val posPaidAmountText = _posPaidAmountText.asStateFlow()

    private val _posNotes = MutableStateFlow("")
    val posNotes = _posNotes.asStateFlow()

    private val _posReceiptPath = MutableStateFlow<String?>(null)
    val posReceiptPath = _posReceiptPath.asStateFlow()

    private val _posPaymentMethod = MutableStateFlow("Cash")
    val posPaymentMethod = _posPaymentMethod.asStateFlow()

    // --- Filtered Inventory ---
    val filteredProducts: StateFlow<List<Product>> = combine(
        products,
        inventorySearch,
        inventorySizeFilter
    ) { prodList, query, sizeFilter ->
        prodList.filter { product ->
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.category.contains(query, ignoreCase = true)
            val matchesSize = sizeFilter == "All" || product.size.equals(sizeFilter, ignoreCase = true)
            matchesQuery && matchesSize
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Filtered Customers ---
    val filteredCustomers: StateFlow<List<Customer>> = combine(
        customers,
        customerSearch
    ) { customerList, query ->
        if (query.isBlank()) customerList
        else customerList.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.phone.contains(query, ignoreCase = true) ||
                    it.address.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Filtered Sales ---
    val filteredSales: StateFlow<List<Sale>> = combine(
        sales,
        salesTimeframe,
        salesSearch
    ) { saleList, timeframe, query ->
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
                    sale.customerName.contains(query, ignoreCase = true) ||
                    sale.productSize.contains(query, ignoreCase = true) ||
                    sale.notes.contains(query, ignoreCase = true)
            inTimeframe && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dashboard Aggregations ---
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        products,
        sales,
        totalMarketDue
    ) { prodList, saleList, marketDue ->
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
            totalDueInMarket = marketDue,
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

    // --- Actions: Inventory ---
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
                _userMessage.emit("Product '${product.name}' যোগ করা হয়েছে!")
            } else {
                repository.updateProduct(product)
                _userMessage.emit("Product '${product.name}' আপডেট করা হয়েছে!")
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProduct(product)
            StorageHelper.deleteFileIfExist(product.imagePath)
            _userMessage.emit("Product '${product.name}' ডিলিট করা হয়েছে।")
        }
    }

    // --- Actions: Customer Management ---
    fun setCustomerSearch(query: String) {
        _customerSearch.value = query
    }

    fun saveCustomer(
        id: Long = 0,
        name: String,
        phone: String,
        address: String,
        initialDue: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = Customer(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                totalDue = initialDue
            )
            if (id == 0L) {
                repository.insertCustomer(customer)
                _userMessage.emit("কাস্টমার '${customer.name}' যুক্ত হয়েছে!")
            } else {
                repository.updateCustomer(customer)
                _userMessage.emit("কাস্টমার তথ্য আপডেট হয়েছে!")
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomer(customer)
            _userMessage.emit("কাস্টমার '${customer.name}' ডিলিট করা হয়েছে।")
        }
    }

    fun recordDuePayment(
        customerId: Long,
        customerName: String,
        amount: Double,
        note: String,
        paymentMethod: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordDueCollection(customerId, customerName, amount, note, paymentMethod)
            _userMessage.emit("${StorageHelper.formatCurrency(amount)} টাকা বকেয়া আদায় রেকর্ড হয়েছে!")
        }
    }

    // --- Actions: POS (Sell with / without customer, with / without due) ---
    fun selectProductForSale(product: Product) {
        _selectedProduct.value = product
        _posQuantity.value = 1
        _posIsDue.value = false
        _posPaidAmountText.value = ""
    }

    fun clearSelectedProduct() {
        _selectedProduct.value = null
        _posQuantity.value = 1
        _posSelectedCustomer.value = null
        _posIsDue.value = false
        _posPaidAmountText.value = ""
        _posNotes.value = ""
        _posReceiptPath.value = null
    }

    fun updatePosQuantity(qty: Int) {
        _posQuantity.value = qty.coerceAtLeast(1)
    }

    fun setPosCustomer(customer: Customer?) {
        _posSelectedCustomer.value = customer
    }

    fun setPosIsDue(isDue: Boolean) {
        _posIsDue.value = isDue
    }

    fun setPosPaidAmountText(amountText: String) {
        _posPaidAmountText.value = amountText
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

        val customer = _posSelectedCustomer.value
        val isDue = _posIsDue.value

        val paid = if (isDue) {
            val entered = _posPaidAmountText.value.toDoubleOrNull() ?: 0.0
            entered.coerceIn(0.0, total)
        } else {
            total
        }
        val due = (total - paid).coerceAtLeast(0.0)

        viewModelScope.launch(Dispatchers.IO) {
            val sale = Sale(
                productId = product.id,
                productName = product.name,
                productSize = product.size,
                unitPrice = product.price,
                quantity = qty,
                totalAmount = total,
                customerId = customer?.id,
                customerName = customer?.name ?: "Walk-in Customer (নগদ খরিদ্দার)",
                customerPhone = customer?.phone ?: "",
                paidAmount = paid,
                dueAmount = due,
                isDue = due > 0,
                notes = _posNotes.value.trim(),
                receiptImagePath = _posReceiptPath.value,
                paymentMethod = _posPaymentMethod.value,
                saleTimestamp = System.currentTimeMillis()
            )

            repository.completeSale(sale)

            val msg = if (due > 0) {
                "বিক্রি সম্পন্ন! আদায়: ${StorageHelper.formatCurrency(paid)}, বাকি: ${StorageHelper.formatCurrency(due)}"
            } else {
                "বিক্রি সম্পন্ন! মোট আদায়: ${StorageHelper.formatCurrency(total)}"
            }
            _userMessage.emit(msg)

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
        newPaidAmount: Double,
        newNotes: String,
        newReceiptPath: String?,
        newPaymentMethod: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newTotal = newQuantity * newUnitPrice
            val newDue = (newTotal - newPaidAmount).coerceAtLeast(0.0)

            val updatedSale = oldSale.copy(
                quantity = newQuantity,
                unitPrice = newUnitPrice,
                totalAmount = newTotal,
                paidAmount = newPaidAmount,
                dueAmount = newDue,
                isDue = newDue > 0,
                notes = newNotes.trim(),
                receiptImagePath = newReceiptPath,
                paymentMethod = newPaymentMethod
            )
            repository.updateSale(oldSale, updatedSale)
            _userMessage.emit("Sale #${oldSale.id} আপডেট করা হয়েছে!")
        }
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSale(sale)
            StorageHelper.deleteFileIfExist(sale.receiptImagePath)
            _userMessage.emit("Sale #${sale.id} ডিলিট ও স্টক রিফান্ড হয়েছে!")
        }
    }

    // --- Export Sales Report (100% Offline) ---
    fun exportSalesCsv(context: Context): Intent? {
        val currentSales = filteredSales.value
        if (currentSales.isEmpty()) return null

        try {
            val file = File(context.cacheDir, "sales_report_${System.currentTimeMillis()}.csv")
            FileOutputStream(file).bufferedWriter().use { writer ->
                writer.write("Sale ID,Date & Time,Product Name,Size,Unit Price,Quantity,Total Amount,Customer,Paid,Due,Payment Method,Notes\n")
                for (s in currentSales) {
                    val dateStr = StorageHelper.formatDate(s.saleTimestamp).replace(",", " ")
                    val safeName = s.productName.replace(",", " ")
                    val safeCust = s.customerName.replace(",", " ")
                    val safeNotes = s.notes.replace(",", " ").replace("\n", " ")
                    writer.write("${s.id},$dateStr,$safeName,${s.productSize},${s.unitPrice},${s.quantity},${s.totalAmount},$safeCust,${s.paidAmount},${s.dueAmount},${s.paymentMethod},$safeNotes\n")
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
            return Intent.createChooser(shareIntent, "শেয়ার করুন সেলস স্টেটমেন্ট")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

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
