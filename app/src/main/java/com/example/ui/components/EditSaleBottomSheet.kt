package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Sale
import com.example.ui.theme.BkashPink
import com.example.ui.theme.DokanGreen
import com.example.ui.theme.DueAmber
import com.example.ui.theme.ErrorRed
import com.example.util.StorageHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSaleBottomSheet(
    sheetState: SheetState,
    sale: Sale,
    onDismiss: () -> Unit,
    onSave: (
        oldSale: Sale,
        newQuantity: Int,
        newUnitPrice: Double,
        newPaidAmount: Double,
        newNotes: String,
        newReceiptPath: String?,
        newPaymentMethod: String
    ) -> Unit
) {
    val context = LocalContext.current

    var quantityText by remember(sale) { mutableStateOf(sale.quantity.toString()) }
    var unitPriceText by remember(sale) { mutableStateOf(sale.unitPrice.toString()) }
    var paidAmountText by remember(sale) { mutableStateOf(sale.paidAmount.toString()) }
    var notes by remember(sale) { mutableStateOf(sale.notes) }
    var paymentMethod by remember(sale) { mutableStateOf(sale.paymentMethod) }
    var receiptPath by remember(sale) { mutableStateOf(sale.receiptImagePath) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = StorageHelper.saveUriToInternalStorage(context, it, "receipt")
            if (savedPath != null) {
                receiptPath = savedPath
            }
        }
    }

    val currentQty = quantityText.toIntOrNull() ?: 1
    val currentPrice = unitPriceText.toDoubleOrNull() ?: 0.0
    val recalculatedTotal = currentQty * currentPrice
    val currentPaid = paidAmountText.toDoubleOrNull() ?: recalculatedTotal
    val recalculatedDue = (recalculatedTotal - currentPaid).coerceAtLeast(0.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "বিক্রি হিসেব এডিট #${sale.id}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${sale.productName} • সাইজ: ${sale.productSize} • ক্রেতা: ${sale.customerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-time calculation highlight card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (recalculatedDue > 0) DueAmber.copy(alpha = 0.12f) else DokanGreen.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "পুনঃগণনাকৃত মোট বিল",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = StorageHelper.formatCurrency(recalculatedTotal),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (recalculatedDue > 0) DueAmber else DokanGreen
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "জমা: ${StorageHelper.formatCurrency(currentPaid)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (recalculatedDue > 0) {
                                Text(
                                    text = "বাকি: ${StorageHelper.formatCurrency(recalculatedDue)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DueAmber
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity with steppers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it; errorMessage = null },
                    label = { Text("বিক্রি পরিমাণ (Quantity)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val q = quantityText.toIntOrNull() ?: 1
                        if (q > 1) quantityText = (q - 1).toString()
                    }
                ) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(
                    onClick = {
                        val q = quantityText.toIntOrNull() ?: 0
                        quantityText = (q + 1).toString()
                    }
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Unit Price
            OutlinedTextField(
                value = unitPriceText,
                onValueChange = { unitPriceText = it; errorMessage = null },
                label = { Text("প্রতি পিস মূল্য (Unit Price ৳)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Paid Amount
            OutlinedTextField(
                value = paidAmountText,
                onValueChange = { paidAmountText = it },
                label = { Text("জমা টাকা (Paid Amount ৳)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method
            Text(
                text = "পেমেন্ট মাধ্যম (Payment Method)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Cash", "bKash", "Nagad", "Card").forEach { method ->
                    val isSelected = paymentMethod.equals(method, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            if (method == "bKash") BkashPink else DokanGreen
                        } else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { paymentMethod = method }
                    ) {
                        Text(
                            text = if (method == "Cash") "নগদ" else method,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("নোট / মেমো বিবরণ") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Receipt photo
            Text(
                text = "রসিদ / মেমো ছবি (Offline)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val file = receiptPath?.let { File(it) }
                    if (file != null && file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Receipt photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Add Receipt",
                                tint = DokanGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "ছবি তুলুন",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                if (receiptPath != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { receiptPath = null }
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "ছবি মুছুন", style = MaterialTheme.typography.labelMedium, color = ErrorRed)
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("বাতিল")
                }
                Button(
                    onClick = {
                        val qty = quantityText.toIntOrNull()
                        val price = unitPriceText.toDoubleOrNull()
                        val paid = paidAmountText.toDoubleOrNull() ?: 0.0

                        if (qty == null || qty <= 0) {
                            errorMessage = "সঠিক পরিমাণ লিখুন (কমপক্ষে ১)"
                            return@Button
                        }
                        if (price == null || price < 0) {
                            errorMessage = "সঠিক ইউনিট মূল্য লিখুন"
                            return@Button
                        }

                        onSave(sale, qty, price, paid, notes, receiptPath, paymentMethod)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_sale_edit_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DokanGreen)
                ) {
                    Text("সংরক্ষণ করুন")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
