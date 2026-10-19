package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.ui.components.ProductImageThumb
import com.example.ui.components.SizeBadge
import com.example.ui.components.StockBadge
import com.example.ui.theme.BkashPink
import com.example.ui.theme.DokanGreen
import com.example.ui.theme.DueAmber
import com.example.ui.theme.ErrorRed
import com.example.util.StorageHelper
import com.example.viewmodel.RetailViewModel

@Composable
fun PosScreen(
    viewModel: RetailViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val allProducts by viewModel.products.collectAsStateWithLifecycle()
    val searchQuery by viewModel.inventorySearch.collectAsStateWithLifecycle()
    val selectedSizeFilter by viewModel.inventorySizeFilter.collectAsStateWithLifecycle()

    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val quantity by viewModel.posQuantity.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.posSelectedCustomer.collectAsStateWithLifecycle()
    val isDue by viewModel.posIsDue.collectAsStateWithLifecycle()
    val paidAmountText by viewModel.posPaidAmountText.collectAsStateWithLifecycle()
    val notes by viewModel.posNotes.collectAsStateWithLifecycle()
    val receiptPath by viewModel.posReceiptPath.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.posPaymentMethod.collectAsStateWithLifecycle()

    val customers by viewModel.customers.collectAsStateWithLifecycle()

    val sizeFilters = listOf("All", "S", "M", "L", "XL", "XXL", "XXXL")

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val path = StorageHelper.saveUriToInternalStorage(context, it, "receipt")
            viewModel.setPosReceiptPath(path)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // POS Header
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "বিক্রি / ক্যাশ কাউন্টার (POS)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "নগদ ও বাকি বিক্রি সম্পন্ন করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        tint = DokanGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setInventorySearch(it) },
                    placeholder = { Text("পণ্যের নাম বা ক্যাটাগরি খুঁজুন...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setInventorySearch("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Size Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sizeFilters.forEach { sz ->
                        val isSelected = selectedSizeFilter == sz
                        SizeBadge(
                            size = sz,
                            isSelected = isSelected,
                            onClick = { viewModel.setInventorySizeFilter(sz) }
                        )
                    }
                }
            }
        }

        // Product Catalog Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (filteredProducts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (allProducts.isEmpty()) "কোন পণ্য স্টকে নেই" else "মিল পাওয়া যায়নি",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (allProducts.isEmpty()) "ইনভেন্টরি ট্যাবে গিয়ে নতুন পণ্য যোগ করুন" else "অন্য কোনো নামে সার্চ করুন",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val isSelected = selectedProduct?.id == product.id
                        ProductPosCard(
                            product = product,
                            isSelected = isSelected,
                            onSelect = { viewModel.selectProductForSale(product) }
                        )
                    }
                }
            }
        }

        // Checkout Bottom Drawer / Active Sale Panel
        AnimatedVisibility(
            visible = selectedProduct != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            selectedProduct?.let { product ->
                ActiveSaleCheckoutPanel(
                    product = product,
                    quantity = quantity,
                    selectedCustomer = selectedCustomer,
                    customersList = customers,
                    isDue = isDue,
                    paidAmountText = paidAmountText,
                    notes = notes,
                    receiptPath = receiptPath,
                    paymentMethod = paymentMethod,
                    onQuantityChange = { viewModel.updatePosQuantity(it) },
                    onCustomerSelect = { viewModel.setPosCustomer(it) },
                    onIsDueChange = { viewModel.setPosIsDue(it) },
                    onPaidAmountTextChange = { viewModel.setPosPaidAmountText(it) },
                    onNotesChange = { viewModel.setPosNotes(it) },
                    onPaymentMethodChange = { viewModel.setPosPaymentMethod(it) },
                    onAttachReceipt = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveReceipt = { viewModel.setPosReceiptPath(null) },
                    onClose = { viewModel.clearSelectedProduct() },
                    onCompleteSale = {
                        viewModel.completeSale(onSuccess = {})
                    }
                )
            }
        }
    }
}

@Composable
private fun ProductPosCard(
    product: Product,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) DokanGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val containerBg = if (isSelected) DokanGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            borderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("pos_product_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                ProductImageThumb(
                    imagePath = product.imagePath,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = product.size,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StorageHelper.formatCurrency(product.price),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DokanGreen
                )
                StockBadge(stock = product.stockQuantity)
            }
        }
    }
}

@Composable
private fun ActiveSaleCheckoutPanel(
    product: Product,
    quantity: Int,
    selectedCustomer: Customer?,
    customersList: List<Customer>,
    isDue: Boolean,
    paidAmountText: String,
    notes: String,
    receiptPath: String?,
    paymentMethod: String,
    onQuantityChange: (Int) -> Unit,
    onCustomerSelect: (Customer?) -> Unit,
    onIsDueChange: (Boolean) -> Unit,
    onPaidAmountTextChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onAttachReceipt: () -> Unit,
    onRemoveReceipt: () -> Unit,
    onClose: () -> Unit,
    onCompleteSale: () -> Unit
) {
    val totalAmount = product.price * quantity
    val enteredPaid = paidAmountText.toDoubleOrNull() ?: 0.0
    val actualPaid = if (isDue) enteredPaid.coerceIn(0.0, totalAmount) else totalAmount
    val dueAmount = (totalAmount - actualPaid).coerceAtLeast(0.0)

    var customerDropdownOpen by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ProductImageThumb(
                        imagePath = product.imagePath,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SizeBadge(size = product.size)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${StorageHelper.formatCurrency(product.price)} • স্টক: ${product.stockQuantity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-Time Total Calculation Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDue && dueAmount > 0) DueAmber.copy(alpha = 0.12f) else DokanGreen.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "মোট বিল (TOTAL BILL)",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$quantity × ${StorageHelper.formatCurrency(product.price)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = StorageHelper.formatCurrency(totalAmount),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isDue && dueAmount > 0) DueAmber else DokanGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quantity Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "পরিমাণ (Quantity):",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        text = "$quantity",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FilledIconButton(
                        onClick = { onQuantityChange(quantity + 1) },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Customer Selector (Sell with or without customer) ---
            Text(
                text = "কাস্টমার নির্বাচন (Customer)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { customerDropdownOpen = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = DokanGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedCustomer?.name ?: "সাধারণ খরিদ্দার (Walk-in Customer - No Customer)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (selectedCustomer != null) {
                            Text(
                                text = "পরিবর্তন",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = DokanGreen
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = customerDropdownOpen,
                    onDismissRequest = { customerDropdownOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("সাধারণ খরিদ্দার (Walk-in Customer - No selection)") },
                        onClick = {
                            onCustomerSelect(null)
                            customerDropdownOpen = false
                        }
                    )
                    customersList.forEach { cust ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(cust.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "ফোন: ${cust.phone.ifEmpty { "N/A" }} • বাকি: ${StorageHelper.formatCurrency(cust.totalDue)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onCustomerSelect(cust)
                                customerDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Due / Bakir Khata Toggle (Sell with Due or Cash) ---
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDue) DueAmber.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isDue) "বাকি বিক্রি (Due / Credit Sale)" else "সম্পূর্ণ নগদ পরিশোধ (Full Paid)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDue) DueAmber else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDue) "বকেয়া কাস্টমারের বাকি খাতায় যোগ হবে" else "কোন বাকি নেই, পুরো বিল পেইড",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDue,
                        onCheckedChange = onIsDueChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = DueAmber, checkedTrackColor = DueAmber.copy(alpha = 0.5f))
                    )
                }
            }

            // If Due is enabled: show Paid amount & Remaining Due preview
            if (isDue) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = onPaidAmountTextChange,
                        label = { Text("জমা টাকা (Paid Amount)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DueAmber.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "বাকি থাকবে (Due)",
                                style = MaterialTheme.typography.labelSmall,
                                color = DueAmber
                            )
                            Text(
                                text = StorageHelper.formatCurrency(dueAmount),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DueAmber
                            )
                        }
                    }
                }

                if (selectedCustomer == null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "পরামর্শ: বাকি বিক্রি করতে ওপর থেকে একজন কাস্টমার নির্বাচন করুন যাতে বাকি খাতা ট্র্যাক করা যায়।",
                        style = MaterialTheme.typography.labelSmall,
                        color = DueAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Method Selector
            Text(
                text = "পেমেন্ট মাধ্যম (Payment Method)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Cash", "bKash", "Nagad", "Card").forEach { method ->
                    val isSelected = paymentMethod == method
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            when (method) {
                                "bKash" -> BkashPink
                                else -> DokanGreen
                            }
                        } else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPaymentMethodChange(method) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = if (method == "Cash") "নগদ" else method,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Optional Receipt & Notes Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onAttachReceipt() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (receiptPath != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                            contentDescription = "Attach Receipt",
                            tint = if (receiptPath != null) DokanGreen else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (receiptPath != null) "রসিদ যুক্ত" else "রসিদ ছবি",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (receiptPath != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = ErrorRed,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onRemoveReceipt() }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    placeholder = { Text("নোট / মেমো নম্বর...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Complete Sale Button
            Button(
                onClick = onCompleteSale,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("complete_sale_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDue && dueAmount > 0) DueAmber else DokanGreen
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isDue && dueAmount > 0)
                        "বিক্রি সম্পন্ন (জমা: ${StorageHelper.formatCurrency(actualPaid)} • বাকি: ${StorageHelper.formatCurrency(dueAmount)})"
                    else "বিক্রি সম্পন্ন করুন • ${StorageHelper.formatCurrency(totalAmount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}
