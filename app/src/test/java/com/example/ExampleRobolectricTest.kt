package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Product
import com.example.data.model.Sale
import org.junit.Assert.assertEquals
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
  fun `calculate sale total correctly`() {
    val product = Product(
      name = "Cotton T-Shirt",
      size = "L",
      price = 25.0,
      stockQuantity = 10
    )
    val qty = 3
    val total = product.price * qty
    assertEquals(75.0, total, 0.001)

    val sale = Sale(
      productName = product.name,
      productSize = product.size,
      unitPrice = product.price,
      quantity = qty,
      totalAmount = total
    )
    assertEquals(75.0, sale.totalAmount, 0.001)
    assertEquals(3, sale.quantity)
  }
}
