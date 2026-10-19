package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer
import com.example.ui.theme.DokanGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerBottomSheet(
    sheetState: SheetState,
    customerToEdit: Customer?,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, phone: String, address: String, initialDue: Double) -> Unit
) {
    var name by remember(customerToEdit) { mutableStateOf(customerToEdit?.name ?: "") }
    var phone by remember(customerToEdit) { mutableStateOf(customerToEdit?.phone ?: "") }
    var address by remember(customerToEdit) { mutableStateOf(customerToEdit?.address ?: "") }
    var dueText by remember(customerToEdit) {
        mutableStateOf(if (customerToEdit != null && customerToEdit.totalDue > 0) customerToEdit.totalDue.toString() else "")
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = DokanGreen)
                    Text(
                        text = if (customerToEdit == null) " নতুন কাস্টমার যোগ করুন" else " কাস্টমার তথ্য এডিট",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = { Text("কাস্টমারের নাম (Customer Name) *") },
                placeholder = { Text("যেমন: রফিক উদ্দিন") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("মোবাইল নম্বর (Phone Number)") },
                placeholder = { Text("যেমন: 017xxxxxxxx") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("ঠিকানা / দোকান (Address / Area)") },
                placeholder = { Text("যেমন: ধানমন্ডি, ঢাকা") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = dueText,
                onValueChange = { dueText = it },
                label = { Text("পূর্বের বকেয়া/বাকি (Previous Due, if any)") },
                placeholder = { Text("৳ 0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        if (name.isBlank()) {
                            errorMessage = "দয়া করে কাস্টমারের নাম লিখুন"
                            return@Button
                        }
                        val initialDue = dueText.toDoubleOrNull() ?: 0.0
                        onSave(customerToEdit?.id ?: 0L, name, phone, address, initialDue)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_customer_button"),
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
