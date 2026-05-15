package com.lucas.predictaapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lucas.predictaapp.data.model.Category
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.data.model.Fixtures
import com.lucas.predictaapp.data.model.Notification
import com.lucas.predictaapp.data.model.Subscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Expense::class, Subscription::class, Notification::class, Category::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE expenses ADD COLUMN dateMillis INTEGER NOT NULL DEFAULT 0")
        }

        private val MIGRATION_2_3 = Migration(2, 3) { db ->
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    emoji TEXT NOT NULL,
                    type TEXT NOT NULL,
                    isCustom INTEGER NOT NULL DEFAULT 0
                )"""
            )
        }

        private val MIGRATION_3_4 = Migration(3, 4) { db ->
            db.execSQL("ALTER TABLE categories ADD COLUMN color TEXT NOT NULL DEFAULT '#636E72'")
            db.execSQL("ALTER TABLE categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            // Backfill colors for seed categories
            val colorUpdates = mapOf(
                "Supermercado"    to "#E8A83E",
                "Delivery"        to "#E07A7A",
                "Salidas"         to "#E66B57",
                "Café"            to "#C58F6F",
                "Transporte"      to "#6A92E8",
                "Combustible"     to "#C58F6F",
                "Salud"           to "#E87AB6",
                "Farmacia"        to "#6FD3C5",
                "Educación"       to "#9DE8A0",
                "Entretenimiento" to "#9D7AE8",
                "Ropa"            to "#8AA8E8",
                "Tecnología"      to "#6A92E8",
                "Hogar"           to "#6FC58B",
                "Servicios"       to "#8AA8E8",
                "Suscripciones"   to "#9D7AE8",
                "Viajes"          to "#6FD3C5",
                "Deporte"         to "#6FC58B",
                "Mascotas"        to "#6FD3C5",
                "Regalos"         to "#E8D14A",
                "Otros"           to "#636E72",
                "Salario"         to "#6FC58B",
                "Freelance"       to "#E8A83E",
                "Inversiones"     to "#6FC58B",
                "Otros ingresos"  to "#E66B57",
            )
            colorUpdates.forEach { (name, hex) ->
                db.execSQL("UPDATE categories SET color = '$hex' WHERE name = '$name'")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "predicta.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    database.subscriptionDao().upsertAll(Fixtures.subscriptions)
                                    database.notificationDao().upsertAll(Fixtures.notifications)
                                    database.categoryDao().insertAll(ExpenseCategories.seed)
                                }
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
