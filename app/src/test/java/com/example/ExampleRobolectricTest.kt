package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.data.model.Sale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RetailPOS", appName)
  }

  @Test
  fun `calculate sale total and due correctly`() {
    val product = Product(
      name = "Cotton T-Shirt",
      size = "L",
      price = 500.0,
      stockQuantity = 10
    )
    val qty = 2
    val total = product.price * qty
    assertEquals(1000.0, total, 0.001)

    // Sale with partial payment (Due sale)
    val paid = 600.0
    val due = total - paid
    val customer = Customer(name = "Rahim", phone = "01711111111", totalDue = 0.0)

    val sale = Sale(
      productId = product.id,
      productName = product.name,
      productSize = product.size,
      unitPrice = product.price,
      quantity = qty,
      totalAmount = total,
      customerId = customer.id,
      customerName = customer.name,
      paidAmount = paid,
      dueAmount = due,
      isDue = due > 0
    )

    assertEquals(1000.0, sale.totalAmount, 0.001)
    assertEquals(600.0, sale.paidAmount, 0.001)
    assertEquals(400.0, sale.dueAmount, 0.001)
    assertTrue(sale.isDue)
  }
}
