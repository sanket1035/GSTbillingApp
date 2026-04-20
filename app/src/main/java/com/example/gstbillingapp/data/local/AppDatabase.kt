package com.example.gstbillingapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gstbillingapp.data.local.dao.InvoiceDao
import com.example.gstbillingapp.data.local.dao.SettingsDao
import com.example.gstbillingapp.data.local.entities.BusinessSettings
import com.example.gstbillingapp.data.local.entities.InvoiceEntity
import com.example.gstbillingapp.data.local.entities.InvoiceItemEntity
import com.example.gstbillingapp.data.local.entities.PaymentEntity

@Database(entities = [InvoiceEntity::class, InvoiceItemEntity::class, PaymentEntity::class, BusinessSettings::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN paymentStatus TEXT NOT NULL DEFAULT 'UNPAID'")
                db.execSQL("ALTER TABLE invoices ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN dueAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN dueDate INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        invoiceId INTEGER NOT NULL, 
                        amountPaid REAL NOT NULL, 
                        paymentDate INTEGER NOT NULL, 
                        FOREIGN KEY(invoiceId) REFERENCES invoices(id) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_invoiceId ON payments (invoiceId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE invoice_items ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE payments ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS business_settings (
                        userId TEXT PRIMARY KEY NOT NULL, 
                        companyName TEXT NOT NULL DEFAULT '', 
                        gstNumber TEXT NOT NULL DEFAULT '', 
                        address TEXT NOT NULL DEFAULT '', 
                        phoneNumber TEXT NOT NULL DEFAULT '', 
                        email TEXT NOT NULL DEFAULT '', 
                        logoPath TEXT, 
                        signaturePath TEXT
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gst_billing_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
