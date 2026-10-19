package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.ui.components.AddEditProductBottomSheet
import com.example.ui.components.EditSaleBottomSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.SalesHistoryScreen
import com.example.viewmodel.RetailViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class ScreenTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
    POS("POS Sale", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale, "tab_pos"),
    INVENTORY("Inventory", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "tab_inventory"),
    SALES("Sales Log", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_sales")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: RetailViewModel) {
    var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Bottom sheet states
    val productSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isProductSheetOpen by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

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
    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
        currentTab = ScreenTab.DASHBOARD
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                ScreenTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabContent"
            ) { tab ->
                when (tab) {
                    ScreenTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToPos = { currentTab = ScreenTab.POS },
                        onNavigateToInventory = { currentTab = ScreenTab.INVENTORY },
                        onNavigateToSales = { currentTab = ScreenTab.SALES },
                        onOpenAddProduct = {
                            productToEdit = null
                            isProductSheetOpen = true
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
            onSave = { oldSale, newQty, newPrice, newNotes, newReceipt, newPayment ->
                viewModel.updateSale(
                    oldSale = oldSale,
                    newQuantity = newQty,
                    newUnitPrice = newPrice,
                    newNotes = newNotes,
                    newReceiptPath = newReceipt,
                    newPaymentMethod = newPayment
                )
            }
        )
    }
}
