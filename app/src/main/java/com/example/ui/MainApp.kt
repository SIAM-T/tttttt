package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.ui.components.AddEditCustomerBottomSheet
import com.example.ui.components.AddEditProductBottomSheet
import com.example.ui.components.CurvedBubbleBottomNav
import com.example.ui.components.EditSaleBottomSheet
import com.example.ui.screens.CustomersDueScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DetailReportScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.SalesHistoryScreen
import com.example.viewmodel.RetailViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class ScreenTab(
    val title: String,
    val banglaTitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    DASHBOARD("Dashboard", "হিসাব", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
    POS("POS", "বিক্রি", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale, "tab_pos"),
    INVENTORY("Inventory", "স্টক", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "tab_inventory"),
    CUSTOMERS("Customers", "বাকি খাতা", Icons.Filled.People, Icons.Outlined.People, "tab_customers"),
    SALES("Sales Log", "হিস্ট্রি", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_sales")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: RetailViewModel) {
    var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
    var isDetailReportOpen by remember { mutableStateOf(false) }

    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Bottom sheet states
    val productSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isProductSheetOpen by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    val customerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isCustomerSheetOpen by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }

    val saleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSaleSheetOpen by remember { mutableStateOf(false) }
    var saleToEdit by remember { mutableStateOf<Sale?>(null) }

    // Collect snackbar messages
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Android Back button handling
    BackHandler(enabled = isDetailReportOpen || currentTab != ScreenTab.DASHBOARD) {
        if (isDetailReportOpen) {
            isDetailReportOpen = false
        } else {
            currentTab = ScreenTab.DASHBOARD
        }
    }

    if (isDetailReportOpen) {
        DetailReportScreen(
            viewModel = viewModel,
            onBack = { isDetailReportOpen = false }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                CurvedBubbleBottomNav(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    hasDueAlert = dashboardSummary.totalDueInMarket > 0,
                    hasLowStockAlert = dashboardSummary.lowStockCount > 0
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) {
                            AnimatedContentTransitionScope.SlideDirection.Left
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.Right
                        }
                        (slideIntoContainer(direction, animationSpec = tween(280)) + fadeIn(animationSpec = tween(280)))
                            .togetherWith(slideOutOfContainer(direction, animationSpec = tween(280)) + fadeOut(animationSpec = tween(280)))
                    },
                    label = "TabContent"
                ) { tab ->
                    when (tab) {
                        ScreenTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToPos = { currentTab = ScreenTab.POS },
                            onNavigateToInventory = { currentTab = ScreenTab.INVENTORY },
                            onNavigateToCustomers = { currentTab = ScreenTab.CUSTOMERS },
                            onNavigateToSales = { currentTab = ScreenTab.SALES },
                            onOpenAddProduct = {
                                productToEdit = null
                                isProductSheetOpen = true
                            },
                            onNavigateToDetailReport = {
                                isDetailReportOpen = true
                            }
                        )

                        ScreenTab.POS -> PosScreen(
                            viewModel = viewModel
                        )

                        ScreenTab.INVENTORY -> InventoryScreen(
                            viewModel = viewModel,
                            onAddProductClick = {
                                productToEdit = null
                                isProductSheetOpen = true
                            },
                            onEditProductClick = { prod ->
                                productToEdit = prod
                                isProductSheetOpen = true
                            }
                        )

                        ScreenTab.CUSTOMERS -> CustomersDueScreen(
                            viewModel = viewModel,
                            onAddCustomerClick = {
                                customerToEdit = null
                                isCustomerSheetOpen = true
                            },
                            onEditCustomerClick = { cust ->
                                customerToEdit = cust
                                isCustomerSheetOpen = true
                            }
                        )

                        ScreenTab.SALES -> SalesHistoryScreen(
                            viewModel = viewModel,
                            onEditSaleClick = { sale ->
                                saleToEdit = sale
                                isSaleSheetOpen = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Product Bottom Sheet
    if (isProductSheetOpen) {
        AddEditProductBottomSheet(
            sheetState = productSheetState,
            productToEdit = productToEdit,
            onDismiss = {
                scope.launch { productSheetState.hide() }.invokeOnCompletion {
                    isProductSheetOpen = false
                    productToEdit = null
                }
            },
            onSave = { id, name, size, price, costPrice, stock, category, imagePath ->
                viewModel.saveProduct(
                    id = id,
                    name = name,
                    size = size,
                    price = price,
                    costPrice = costPrice,
                    stock = stock,
                    category = category,
                    imagePath = imagePath
                )
            }
        )
    }

    // Add / Edit Customer Bottom Sheet
    if (isCustomerSheetOpen) {
        AddEditCustomerBottomSheet(
            sheetState = customerSheetState,
            customerToEdit = customerToEdit,
            onDismiss = {
                scope.launch { customerSheetState.hide() }.invokeOnCompletion {
                    isCustomerSheetOpen = false
                    customerToEdit = null
                }
            },
            onSave = { id, name, phone, address, initialDue ->
                viewModel.saveCustomer(
                    id = id,
                    name = name,
                    phone = phone,
                    address = address,
                    initialDue = initialDue
                )
            }
        )
    }

    // Edit Sale Bottom Sheet
    if (isSaleSheetOpen && saleToEdit != null) {
        EditSaleBottomSheet(
            sheetState = saleSheetState,
            sale = saleToEdit!!,
            onDismiss = {
                scope.launch { saleSheetState.hide() }.invokeOnCompletion {
                    isSaleSheetOpen = false
                    saleToEdit = null
                }
            },
            onSave = { oldSale, newQty, newPrice, newPaid, newNotes, newReceipt, newPayment ->
                viewModel.updateSale(
                    oldSale = oldSale,
                    newQuantity = newQty,
                    newUnitPrice = newPrice,
                    newPaidAmount = newPaid,
                    newNotes = newNotes,
                    newReceiptPath = newReceipt,
                    newPaymentMethod = newPayment
                )
            }
        )
    }
}
