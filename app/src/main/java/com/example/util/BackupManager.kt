package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Customer
import com.example.data.model.DuePayment
import com.example.data.model.Product
import com.example.data.model.Sale
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupDataPayload(
    val products: List<Product>,
    val sales: List<Sale>,
    val customers: List<Customer>,
    val duePayments: List<DuePayment>,
    val exportDate: String,
    val totalRecords: Int
)

object BackupManager {

    fun createBackupJsonFile(
        context: Context,
        products: List<Product>,
        sales: List<Sale>,
        customers: List<Customer>,
        duePayments: List<DuePayment>
    ): Intent? {
        return try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("appName", "RetailPOS")
            root.put("platform", "Android Room 100% Offline")
            root.put("exportTimestamp", System.currentTimeMillis())
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            root.put("exportDate", dateStr)

            // Counts
            val counts = JSONObject()
            counts.put("productsCount", products.size)
            counts.put("salesCount", sales.size)
            counts.put("customersCount", customers.size)
            counts.put("duePaymentsCount", duePayments.size)
            root.put("counts", counts)

            // Products Array
            val prodArray = JSONArray()
            for (p in products) {
                val pObj = JSONObject()
                pObj.put("id", p.id)
                pObj.put("name", p.name)
                pObj.put("size", p.size)
                pObj.put("price", p.price)
                pObj.put("costPrice", p.costPrice)
                pObj.put("stockQuantity", p.stockQuantity)
                pObj.put("category", p.category)
                pObj.put("barcode", p.barcode)
                pObj.put("createdAt", p.createdAt)
                if (p.imagePath != null) pObj.put("imagePath", p.imagePath)
                prodArray.put(pObj)
            }
            root.put("products", prodArray)

            // Sales Array
            val saleArray = JSONArray()
            for (s in sales) {
                val sObj = JSONObject()
                sObj.put("id", s.id)
                if (s.productId != null) sObj.put("productId", s.productId)
                sObj.put("productName", s.productName)
                sObj.put("productSize", s.productSize)
                sObj.put("unitPrice", s.unitPrice)
                sObj.put("quantity", s.quantity)
                sObj.put("totalAmount", s.totalAmount)
                if (s.customerId != null) sObj.put("customerId", s.customerId)
                sObj.put("customerName", s.customerName)
                sObj.put("customerPhone", s.customerPhone)
                sObj.put("paidAmount", s.paidAmount)
                sObj.put("dueAmount", s.dueAmount)
                sObj.put("isDue", s.isDue)
                sObj.put("discount", s.discount)
                sObj.put("notes", s.notes)
                sObj.put("paymentMethod", s.paymentMethod)
                sObj.put("saleTimestamp", s.saleTimestamp)
                if (s.receiptImagePath != null) sObj.put("receiptImagePath", s.receiptImagePath)
                saleArray.put(sObj)
            }
            root.put("sales", saleArray)

            // Customers Array
            val custArray = JSONArray()
            for (c in customers) {
                val cObj = JSONObject()
                cObj.put("id", c.id)
                cObj.put("name", c.name)
                cObj.put("phone", c.phone)
                cObj.put("address", c.address)
                cObj.put("totalDue", c.totalDue)
                cObj.put("createdAt", c.createdAt)
                custArray.put(cObj)
            }
            root.put("customers", custArray)

            // Due Payments Array
            val dueArray = JSONArray()
            for (d in duePayments) {
                val dObj = JSONObject()
                dObj.put("id", d.id)
                dObj.put("customerId", d.customerId)
                dObj.put("customerName", d.customerName)
                dObj.put("amount", d.amount)
                dObj.put("note", d.note)
                dObj.put("paymentMethod", d.paymentMethod)
                dObj.put("paymentDate", d.paymentDate)
                dueArray.put(dObj)
            }
            root.put("duePayments", dueArray)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(context.cacheDir, "retailpos_backup_$timeStamp.json")
            FileOutputStream(backupFile).bufferedWriter().use { writer ->
                writer.write(root.toString(2))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "RetailPOS Complete Backup ($dateStr)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(shareIntent, "ব্যাকআপ ফাইল সংরক্ষণ বা শেয়ার করুন")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseBackupJson(context: Context, uri: Uri): BackupDataPayload? {
        return try {
            val content = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        content.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }

            val root = JSONObject(content.toString())
            val exportDate = root.optString("exportDate", "Unknown Date")

            val productList = mutableListOf<Product>()
            val prodArray = root.optJSONArray("products")
            if (prodArray != null) {
                for (i in 0 until prodArray.length()) {
                    val p = prodArray.getJSONObject(i)
                    productList.add(
                        Product(
                            id = p.optLong("id", 0L),
                            name = p.optString("name", "Product"),
                            size = p.optString("size", "Standard"),
                            price = p.optDouble("price", 0.0),
                            costPrice = p.optDouble("costPrice", 0.0),
                            stockQuantity = p.optInt("stockQuantity", 0),
                            category = p.optString("category", "General"),
                            barcode = p.optString("barcode", ""),
                            imagePath = if (p.has("imagePath")) p.optString("imagePath") else null,
                            createdAt = p.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val salesList = mutableListOf<Sale>()
            val saleArray = root.optJSONArray("sales")
            if (saleArray != null) {
                for (i in 0 until saleArray.length()) {
                    val s = saleArray.getJSONObject(i)
                    salesList.add(
                        Sale(
                            id = s.optLong("id", 0L),
                            productId = if (s.has("productId") && !s.isNull("productId")) s.optLong("productId") else null,
                            productName = s.optString("productName", "Item"),
                            productSize = s.optString("productSize", "Standard"),
                            unitPrice = s.optDouble("unitPrice", 0.0),
                            quantity = s.optInt("quantity", 1),
                            totalAmount = s.optDouble("totalAmount", 0.0),
                            customerId = if (s.has("customerId") && !s.isNull("customerId")) s.optLong("customerId") else null,
                            customerName = s.optString("customerName", "Walk-in Customer"),
                            customerPhone = s.optString("customerPhone", ""),
                            paidAmount = s.optDouble("paidAmount", 0.0),
                            dueAmount = s.optDouble("dueAmount", 0.0),
                            isDue = s.optBoolean("isDue", false),
                            discount = s.optDouble("discount", 0.0),
                            notes = s.optString("notes", ""),
                            paymentMethod = s.optString("paymentMethod", "Cash"),
                            receiptImagePath = if (s.has("receiptImagePath")) s.optString("receiptImagePath") else null,
                            saleTimestamp = s.optLong("saleTimestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val customerList = mutableListOf<Customer>()
            val custArray = root.optJSONArray("customers")
            if (custArray != null) {
                for (i in 0 until custArray.length()) {
                    val c = custArray.getJSONObject(i)
                    customerList.add(
                        Customer(
                            id = c.optLong("id", 0L),
                            name = c.optString("name", "Customer"),
                            phone = c.optString("phone", ""),
                            address = c.optString("address", ""),
                            totalDue = c.optDouble("totalDue", 0.0),
                            createdAt = c.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val duePaymentList = mutableListOf<DuePayment>()
            val dueArray = root.optJSONArray("duePayments")
            if (dueArray != null) {
                for (i in 0 until dueArray.length()) {
                    val d = dueArray.getJSONObject(i)
                    duePaymentList.add(
                        DuePayment(
                            id = d.optLong("id", 0L),
                            customerId = d.optLong("customerId", 0L),
                            customerName = d.optString("customerName", "Customer"),
                            amount = d.optDouble("amount", 0.0),
                            note = d.optString("note", ""),
                            paymentMethod = d.optString("paymentMethod", "Cash"),
                            paymentDate = d.optLong("paymentDate", System.currentTimeMillis())
                        )
                    )
                }
            }

            val total = productList.size + salesList.size + customerList.size + duePaymentList.size
            BackupDataPayload(
                products = productList,
                sales = salesList,
                customers = customerList,
                duePayments = duePaymentList,
                exportDate = exportDate,
                totalRecords = total
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
