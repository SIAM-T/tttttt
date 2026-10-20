package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.DuePayment
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.repository.RetailRepository
import com.example.util.BackupDataPayload
import com.example.util.BackupManager
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimeframeFilter(val label: String, val banglaLabel: String) {
    TODAY("Today", "আজকের"),
    YESTERDAY("Yesterday", "গতকালকের"),
    THIS_WEEK("This Week", "এই সপ্তাহের"),
    THIS_MONTH("This Month", "এই মাসের"),
    LAST_MONTH("Last Month", "গত মাসের"),
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

data class ReportTopProduct(
    val name: String,
    val size: String,
    val quantitySold: Int,
    val totalRevenue: Double,
    val totalCost: Double = 0.0,
    val estimatedProfit: Double = 0.0
)

data class DetailedReportData(
    val timeframe: TimeframeFilter = TimeframeFilter.THIS_MONTH,
    val totalRevenue: Double = 0.0,
    val totalCost: Double = 0.0,
    val grossProfit: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val totalCashCollected: Double = 0.0,
    val realizedCashProfit: Double = 0.0,
    val totalDueGiven: Double = 0.0,
    val totalItemsSold: Int = 0,
    val totalInvoices: Int = 0,
    val averageOrderValue: Double = 0.0,
    val cashSalesAmount: Double = 0.0,
    val bkashSalesAmount: Double = 0.0,
    val nagadSalesAmount: Double = 0.0,
    val otherPaymentAmount: Double = 0.0,
    val topProducts: List<ReportTopProduct> = emptyList(),
    val salesList: List<Sale> = emptyList()
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

    // --- POS State (Listed / Unlisted Product, With / Without Customer, Cash / Due) ---
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct = _selectedProduct.asStateFlow()

    // Unlisted / Instant Product State (selling products physically in store but not yet added in app)
    private val _isUnlistedProduct = MutableStateFlow(false)
    val isUnlistedProduct = _isUnlistedProduct.asStateFlow()

    private val _unlistedName = MutableStateFlow("")
    val unlistedName = _unlistedName.asStateFlow()

    private val _unlistedSize = MutableStateFlow("Standard")
    val unlistedSize = _unlistedSize.asStateFlow()

    private val _unlistedPriceText = MutableStateFlow("")
    val unlistedPriceText = _unlistedPriceText.asStateFlow()

    private val _unlistedCategory = MutableStateFlow("General")
    val unlistedCategory = _unlistedCategory.asStateFlow()

    private val _unlistedSaveToInventory = MutableStateFlow(false)
    val unlistedSaveToInventory = _unlistedSaveToInventory.asStateFlow()

    private val _unlistedStockQtyText = MutableStateFlow("10")
    val unlistedStockQtyText = _unlistedStockQtyText.asStateFlow()

    private val _unlistedCostPriceText = MutableStateFlow("")
    val unlistedCostPriceText = _unlistedCostPriceText.asStateFlow()

    // Shared POS Transaction Settings
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

    // --- Detailed Report State ---
    private val _reportTimeframe = MutableStateFlow(TimeframeFilter.THIS_MONTH)
    val reportTimeframe = _reportTimeframe.asStateFlow()

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
        val range = getTimeRange(timeframe)

        saleList.filter { sale ->
            val inTimeframe = sale.saleTimestamp in range.first..range.second
            val matchesQuery = query.isBlank() ||
                    sale.productName.contains(query, ignoreCase = true) ||
                    sale.customerName.contains(query, ignoreCase = true) ||
                    sale.productSize.contains(query, ignoreCase = true) ||
                    sale.notes.contains(query, ignoreCase = true)
            inTimeframe && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Detailed Report Analytics Stream ---
    val detailedReport: StateFlow<DetailedReportData> = combine(
        sales,
        reportTimeframe,
        products
    ) { saleList, timeframe, prodList ->
        val range = getTimeRange(timeframe)
        val filtered = saleList.filter { it.saleTimestamp in range.first..range.second }
        val prodMap = prodList.associateBy { it.id }

        var totalRev = 0.0
        var totalCost = 0.0
        var totalPaid = 0.0
        var totalDue = 0.0
        var totalUnits = 0
        var cashAmount = 0.0
        var bkashAmount = 0.0
        var nagadAmount = 0.0
        var otherAmount = 0.0

        for (s in filtered) {
            totalRev += s.totalAmount
            totalPaid += s.paidAmount
            totalDue += s.dueAmount
            totalUnits += s.quantity

            val costPerUnit = s.productId?.let { prodMap[it]?.costPrice } ?: 0.0
            totalCost += (costPerUnit * s.quantity)

            when {
                s.paymentMethod.contains("bKash", ignoreCase = true) -> bkashAmount += s.paidAmount
                s.paymentMethod.contains("Nagad", ignoreCase = true) -> nagadAmount += s.paidAmount
                s.paymentMethod.contains("Cash", ignoreCase = true) || s.paymentMethod.contains("নগদ", ignoreCase = true) -> cashAmount += s.paidAmount
                else -> otherAmount += s.paidAmount
            }
        }

        val grossProfit = totalRev - totalCost
        val profitMargin = if (totalRev > 0) (grossProfit / totalRev) * 100 else 0.0
        val realizedCashProfit = totalPaid - totalCost
        val aov = if (filtered.isNotEmpty()) totalRev / filtered.size else 0.0

        val topProds = filtered.groupBy { "${it.productName}|||${it.productSize}|||${it.productId ?: -1}" }
            .map { entry ->
                val parts = entry.key.split("|||")
                val name = parts.getOrNull(0) ?: "Unknown"
                val size = parts.getOrNull(1) ?: ""
                val pId = parts.getOrNull(2)?.toLongOrNull()
                val unitCost = pId?.let { prodMap[it]?.costPrice } ?: 0.0
                val qty = entry.value.sumOf { it.quantity }
                val rev = entry.value.sumOf { it.totalAmount }
                val cost = unitCost * qty
                val profit = rev - cost
                ReportTopProduct(
                    name = name,
                    size = size,
                    quantitySold = qty,
                    totalRevenue = rev,
                    totalCost = cost,
                    estimatedProfit = profit
                )
            }
            .sortedByDescending { it.totalRevenue }
            .take(6)

        DetailedReportData(
            timeframe = timeframe,
            totalRevenue = totalRev,
            totalCost = totalCost,
            grossProfit = grossProfit,
            profitMarginPercent = profitMargin,
            totalCashCollected = totalPaid,
            realizedCashProfit = realizedCashProfit,
            totalDueGiven = totalDue,
            totalItemsSold = totalUnits,
            totalInvoices = filtered.size,
            averageOrderValue = aov,
            cashSalesAmount = cashAmount,
            bkashSalesAmount = bkashAmount,
            nagadSalesAmount = nagadAmount,
            otherPaymentAmount = otherAmount,
            topProducts = topProds,
            salesList = filtered
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailedReportData())

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

    // --- Actions: POS (Listed & Unlisted Sales) ---
    fun selectProductForSale(product: Product) {
        _isUnlistedProduct.value = false
        _selectedProduct.value = product
        _posQuantity.value = 1
        _posIsDue.value = false
        _posPaidAmountText.value = ""
    }

    fun enableUnlistedProductSale() {
        _selectedProduct.value = null
        _isUnlistedProduct.value = true
        _unlistedName.value = ""
        _unlistedSize.value = "Standard"
        _unlistedPriceText.value = ""
        _unlistedCategory.value = "General"
        _unlistedSaveToInventory.value = false
        _unlistedStockQtyText.value = "10"
        _posQuantity.value = 1
        _posIsDue.value = false
        _posPaidAmountText.value = ""
    }

    fun setUnlistedName(name: String) { _unlistedName.value = name }
    fun setUnlistedSize(size: String) { _unlistedSize.value = size }
    fun setUnlistedPriceText(price: String) { _unlistedPriceText.value = price }
    fun setUnlistedCategory(cat: String) { _unlistedCategory.value = cat }
    fun setUnlistedSaveToInventory(save: Boolean) { _unlistedSaveToInventory.value = save }
    fun setUnlistedStockQtyText(qty: String) { _unlistedStockQtyText.value = qty }

    fun clearSelectedProduct() {
        _selectedProduct.value = null
        _isUnlistedProduct.value = false
        _unlistedName.value = ""
        _unlistedPriceText.value = ""
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
        val isUnlisted = _isUnlistedProduct.value
        val product = _selectedProduct.value

        if (!isUnlisted && product == null) return

        val qty = _posQuantity.value

        val (prodName, prodSize, unitPrice) = if (isUnlisted) {
            val name = _unlistedName.value.trim().ifEmpty { "কাস্টম পণ্য (Custom Item)" }
            val size = _unlistedSize.value.trim().ifEmpty { "Standard" }
            val price = _unlistedPriceText.value.toDoubleOrNull() ?: 0.0
            if (price <= 0.0) {
                viewModelScope.launch { _userMessage.emit("অনুগ্রহ করে পণ্যের সঠিক মূল্য লিখুন!") }
                return
            }
            Triple(name, size, price)
        } else {
            Triple(product!!.name, product.size, product.price)
        }

        val total = unitPrice * qty
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
            var associatedProductId: Long? = product?.id

            // If selling an unlisted product and user wants to save it to inventory too:
            if (isUnlisted && _unlistedSaveToInventory.value) {
                val stockCount = _unlistedStockQtyText.value.toIntOrNull() ?: qty
                val newProduct = Product(
                    name = prodName,
                    size = prodSize,
                    price = unitPrice,
                    stockQuantity = stockCount, // repository.completeSale will adjust -qty
                    category = _unlistedCategory.value.trim().ifEmpty { "General" }
                )
                associatedProductId = repository.insertProduct(newProduct)
            }

            val sale = Sale(
                productId = associatedProductId,
                productName = prodName,
                productSize = prodSize,
                unitPrice = unitPrice,
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

    fun setReportTimeframe(timeframe: TimeframeFilter) {
        _reportTimeframe.value = timeframe
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

            val uri = FileProvider.getUriForFile(
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

    // --- Share Detailed Report Summary as WhatsApp/SMS Text ---
    fun shareDetailedReportText(context: Context, data: DetailedReportData): Intent {
        val sb = StringBuilder()
        sb.append("📊 *দোকানের বিস্তারিত হিসাব ও বিক্রয় রিপোর্ট*\n")
        sb.append("📅 সময়কাল: ${data.timeframe.banglaLabel}\n")
        sb.append("---------------------------------\n")
        sb.append("💰 মোট বিক্রি: ${StorageHelper.formatCurrency(data.totalRevenue)}\n")
        sb.append("📦 মোট ক্রয়/কেনা খরচ: ${StorageHelper.formatCurrency(data.totalCost)}\n")
        sb.append("📈 মোট লাভ (Gross Profit): ${StorageHelper.formatCurrency(data.grossProfit)} (${String.format(java.util.Locale.US, "%.1f", data.profitMarginPercent)}% মার্জিন)\n")
        sb.append("💵 নগদ আদায়: ${StorageHelper.formatCurrency(data.totalCashCollected)}\n")
        sb.append("⚠️ বাকি দেওয়া: ${StorageHelper.formatCurrency(data.totalDueGiven)}\n")
        sb.append("📦 মোট পণ্য বিক্রি: ${data.totalItemsSold} টি\n")
        sb.append("🧾 মোট মেমো/রসিদ: ${data.totalInvoices} টি\n")
        sb.append("📈 গড় বিক্রি প্রতি মেমো: ${StorageHelper.formatCurrency(data.averageOrderValue)}\n\n")

        sb.append("💳 *পেমেন্ট পদ্ধতি বিশ্লেষণ:*\n")
        sb.append("• ক্যাশ: ${StorageHelper.formatCurrency(data.cashSalesAmount)}\n")
        if (data.bkashSalesAmount > 0) sb.append("• বিকাশ: ${StorageHelper.formatCurrency(data.bkashSalesAmount)}\n")
        if (data.nagadSalesAmount > 0) sb.append("• নগদ: ${StorageHelper.formatCurrency(data.nagadSalesAmount)}\n")
        if (data.otherPaymentAmount > 0) sb.append("• অন্যান্য: ${StorageHelper.formatCurrency(data.otherPaymentAmount)}\n")

        if (data.topProducts.isNotEmpty()) {
            sb.append("\n🏆 *শীর্ষ বিক্রিত পণ্যসমূহ:*\n")
            data.topProducts.forEachIndexed { i, prod ->
                sb.append("${i + 1}. ${prod.name} (${prod.size}) - ${prod.quantitySold} টি (${StorageHelper.formatCurrency(prod.totalRevenue)})\n")
            }
        }

        sb.append("---------------------------------\n")
        sb.append("📱 RetailPOS • 100% অফলাইন ও নিরাপদ হিসাব")

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        return Intent.createChooser(sendIntent, "রিপোর্ট পাঠান (হোয়াটসঅ্যাপ / মেসেজ)")
    }

    // --- Full Database Backup & Restore Operations ---
    fun exportFullBackup(context: Context, onResult: (Intent?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val prodList = repository.getAllProductsList()
            val saleList = repository.getAllSalesList()
            val custList = repository.getAllCustomersList()
            val dueList = repository.getAllDuePaymentsList()

            val intent = BackupManager.createBackupJsonFile(
                context = context,
                products = prodList,
                sales = saleList,
                customers = custList,
                duePayments = dueList
            )
            launch(Dispatchers.Main) {
                onResult(intent)
            }
        }
    }

    fun parseBackupFile(context: Context, uri: Uri, onResult: (BackupDataPayload?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val payload = BackupManager.parseBackupJson(context, uri)
            launch(Dispatchers.Main) {
                onResult(payload)
            }
        }
    }

    fun restoreBackupData(payload: BackupDataPayload, replaceAll: Boolean, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreDatabase(
                products = payload.products,
                sales = payload.sales,
                customers = payload.customers,
                duePayments = payload.duePayments,
                replaceAll = replaceAll
            )
            val msg = if (replaceAll) {
                "সফলভাবে ডাটাবেজ রিস্টোর করা হয়েছে (${payload.totalRecords} টি রেকর্ড)!"
            } else {
                "সফলভাবে ডাটা মার্জ করা হয়েছে (${payload.totalRecords} টি রেকর্ড)!"
            }
            _userMessage.emit(msg)
            launch(Dispatchers.Main) {
                onDone()
            }
        }
    }

    private fun getTimeRange(timeframe: TimeframeFilter): Pair<Long, Long> {
        val startOfToday = getStartOfDay()
        val endOfToday = startOfToday + 86400000L - 1
        return when (timeframe) {
            TimeframeFilter.TODAY -> Pair(startOfToday, endOfToday)
            TimeframeFilter.YESTERDAY -> {
                val startOfYesterday = startOfToday - 86400000L
                val endOfYesterday = startOfToday - 1
                Pair(startOfYesterday, endOfYesterday)
            }
            TimeframeFilter.THIS_WEEK -> Pair(getStartOfWeek(), Long.MAX_VALUE)
            TimeframeFilter.THIS_MONTH -> Pair(getStartOfMonth(), Long.MAX_VALUE)
            TimeframeFilter.LAST_MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val thisMonthStart = cal.timeInMillis
                cal.add(Calendar.MONTH, -1)
                val lastMonthStart = cal.timeInMillis
                Pair(lastMonthStart, thisMonthStart - 1)
            }
            TimeframeFilter.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
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
