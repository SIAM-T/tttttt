package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Product
import com.example.data.model.Sale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Product::class, Sale::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "retail_pos_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.productDao(), database.saleDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(productDao: ProductDao, saleDao: SaleDao) {
            if (productDao.getProductCount() == 0) {
                val now = System.currentTimeMillis()
                val dayMillis = 24 * 60 * 60 * 1000L

                val initialProducts = listOf(
                    Product(
                        name = "Classic Cotton Crewneck T-Shirt",
                        size = "L",
                        price = 24.99,
                        costPrice = 11.50,
                        stockQuantity = 45,
                        category = "T-Shirts"
                    ),
                    Product(
                        name = "Classic Cotton Crewneck T-Shirt",
                        size = "M",
                        price = 24.99,
                        costPrice = 11.50,
                        stockQuantity = 38,
                        category = "T-Shirts"
                    ),
                    Product(
                        name = "Classic Cotton Crewneck T-Shirt",
                        size = "XL",
                        price = 26.99,
                        costPrice = 12.00,
                        stockQuantity = 25,
                        category = "T-Shirts"
                    ),
                    Product(
                        name = "Slim Fit Stretch Denim Jeans",
                        size = "32/32",
                        price = 59.90,
                        costPrice = 28.00,
                        stockQuantity = 22,
                        category = "Pants"
                    ),
                    Product(
                        name = "Heavyweight Fleece Pullover Hoodie",
                        size = "XL",
                        price = 48.00,
                        costPrice = 22.00,
                        stockQuantity = 19,
                        category = "Hoodies"
                    ),
                    Product(
                        name = "Heavyweight Fleece Pullover Hoodie",
                        size = "XXL",
                        price = 52.00,
                        costPrice = 24.00,
                        stockQuantity = 14,
                        category = "Hoodies"
                    ),
                    Product(
                        name = "Premium Oxford Button-Down Shirt",
                        size = "M",
                        price = 39.50,
                        costPrice = 18.00,
                        stockQuantity = 16,
                        category = "Shirts"
                    ),
                    Product(
                        name = "Breathable Athletic Mesh Shorts",
                        size = "L",
                        price = 19.99,
                        costPrice = 8.50,
                        stockQuantity = 30,
                        category = "Activewear"
                    )
                )
                productDao.insertAll(initialProducts)

                // Add starter sales history for today and this week to demonstrate the dashboard
                val sampleSales = listOf(
                    Sale(
                        productName = "Classic Cotton Crewneck T-Shirt",
                        productSize = "L",
                        unitPrice = 24.99,
                        quantity = 2,
                        totalAmount = 49.98,
                        notes = "Customer walk-in, cash",
                        paymentMethod = "Cash",
                        saleTimestamp = now - 3600 * 1000L * 2 // 2 hours ago
                    ),
                    Sale(
                        productName = "Slim Fit Stretch Denim Jeans",
                        productSize = "32/32",
                        unitPrice = 59.90,
                        quantity = 1,
                        totalAmount = 59.90,
                        notes = "Fitting room tested",
                        paymentMethod = "Card",
                        saleTimestamp = now - 3600 * 1000L * 4 // 4 hours ago
                    ),
                    Sale(
                        productName = "Heavyweight Fleece Pullover Hoodie",
                        productSize = "XL",
                        unitPrice = 48.00,
                        quantity = 1,
                        totalAmount = 48.00,
                        notes = "Winter discount inquiry",
                        paymentMethod = "Cash",
                        saleTimestamp = now - dayMillis // Yesterday
                    ),
                    Sale(
                        productName = "Premium Oxford Button-Down Shirt",
                        productSize = "M",
                        unitPrice = 39.50,
                        quantity = 2,
                        totalAmount = 79.00,
                        notes = "Gift purchase",
                        paymentMethod = "Card",
                        saleTimestamp = now - (dayMillis * 3) // 3 days ago
                    )
                )
                saleDao.insertAll(sampleSales)
            }
        }
    }
}
