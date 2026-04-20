package com.example.gstbillingapp.data.local.dao

import androidx.room.*
import com.example.gstbillingapp.data.local.entities.InvoiceEntity
import com.example.gstbillingapp.data.local.entities.InvoiceItemEntity
import com.example.gstbillingapp.data.local.entities.InvoiceWithItems
import com.example.gstbillingapp.data.local.entities.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Query("SELECT * FROM invoices WHERE userId = :userId ORDER BY date DESC")
    fun getAllInvoices(userId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE userId = :userId AND (customerName LIKE '%' || :query || '%' OR invoiceNumber LIKE '%' || :query || '%') ORDER BY date DESC")
    fun searchInvoices(userId: String, query: String): Flow<List<InvoiceEntity>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :invoiceId")
    suspend fun getInvoiceWithItems(invoiceId: Long): InvoiceWithItems

    @Query("SELECT COUNT(*) FROM invoices WHERE userId = :userId")
    suspend fun getInvoiceCount(userId: String): Int

    @Query("SELECT SUM(grandTotal) FROM invoices WHERE userId = :userId")
    fun getTotalRevenue(userId: String): Flow<Double?>

    @Query("SELECT SUM(cgst + sgst) FROM invoices WHERE userId = :userId")
    fun getTotalGstCollected(userId: String): Flow<Double?>

    @Query("SELECT itemName as name, SUM(quantity) as count FROM invoice_items WHERE userId = :userId GROUP BY itemName ORDER BY count DESC LIMIT 5")
    fun getTopProducts(userId: String): Flow<List<ProductStats>>

    @Query("SELECT DISTINCT itemName FROM invoice_items WHERE userId = :userId AND itemName LIKE :query || '%'")
    suspend fun getProductNameSuggestions(userId: String, query: String): List<String>

    @Query("SELECT DISTINCT customerName FROM invoices WHERE userId = :userId AND customerName LIKE :query || '%'")
    suspend fun getCustomerNameSuggestions(userId: String, query: String): List<String>

    @Query("SELECT gstRate FROM invoice_items WHERE userId = :userId AND itemName = :itemName ORDER BY id DESC LIMIT 1")
    suspend fun getLastGstRateForItem(userId: String, itemName: String): Double?

    @Query("SELECT * FROM invoices WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getInvoicesByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT strftime('%m-%Y', date / 1000, 'unixepoch') as month, SUM(grandTotal) as total FROM invoices WHERE userId = :userId GROUP BY month ORDER BY date ASC")
    fun getMonthlyRevenue(userId: String): Flow<List<MonthlyRevenue>>

    // Profit Analytics
    @Query("SELECT SUM((price - costPrice) * quantity) FROM invoice_items WHERE userId = :userId")
    fun getTotalProfit(userId: String): Flow<Double?>

    @Query("SELECT strftime('%m-%Y', i.date / 1000, 'unixepoch') as month, SUM((it.price - it.costPrice) * it.quantity) as total FROM invoices i JOIN invoice_items it ON i.id = it.invoiceId WHERE i.userId = :userId GROUP BY month ORDER BY i.date ASC")
    fun getMonthlyProfit(userId: String): Flow<List<MonthlyProfit>>

    @Query("SELECT itemName as name, SUM((price - costPrice) * quantity) as profit FROM invoice_items WHERE userId = :userId GROUP BY itemName ORDER BY profit DESC LIMIT 5")
    fun getTopProfitableProducts(userId: String): Flow<List<ProductProfit>>

    // Customer Intelligence
    @Query("SELECT customerName as name, SUM(grandTotal) as totalSpending, COUNT(*) as invoiceCount, MAX(date) as lastPurchaseDate FROM invoices WHERE userId = :userId GROUP BY customerName ORDER BY totalSpending DESC LIMIT 5")
    fun getTopCustomers(userId: String): Flow<List<CustomerInsight>>

    @Query("SELECT SUM(dueAmount) FROM invoices WHERE userId = :userId")
    fun getTotalPendingAmount(userId: String): Flow<Double?>

    @Query("SELECT COUNT(*) FROM invoices WHERE userId = :userId AND dueAmount > 0 AND dueDate < :today")
    fun getOverdueCount(userId: String, today: Long): Flow<Int>

    @Query("SELECT * FROM invoices WHERE userId = :userId AND dueAmount > 0 AND dueDate < :today")
    fun getOverdueInvoices(userId: String, today: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT customerName as name, SUM(dueAmount) as totalDue FROM invoices WHERE userId = :userId AND dueAmount > 0 GROUP BY customerName")
    fun getCustomerDues(userId: String): Flow<List<CustomerDue>>

    @Query("SELECT itemName as name, SUM((price - costPrice) * quantity) as profit, SUM(quantity) as totalSales, SUM(price * quantity) as totalRevenue FROM invoice_items WHERE userId = :userId GROUP BY itemName HAVING profit > 0 AND totalSales < 10 ORDER BY profit DESC")
    fun getHighProfitLowSalesProducts(userId: String): Flow<List<ProductInsight>>

    @Query("SELECT customerName as name, MAX(date) as lastDate FROM invoices WHERE userId = :userId GROUP BY customerName HAVING lastDate < :threshold")
    fun getInactiveCustomers(userId: String, threshold: Long): Flow<List<InactiveCustomer>>

    @Query("SELECT * FROM invoices WHERE userId = :userId AND grandTotal > 50000")
    fun getLargeInvoices(userId: String): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY paymentDate DESC")
    fun getPaymentsForInvoice(invoiceId: Long): Flow<List<PaymentEntity>>

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Transaction
    suspend fun recordPayment(payment: PaymentEntity) {
        insertPayment(payment)
        val data = getInvoiceWithItems(payment.invoiceId)
        val invoice = data.invoice
        val payments = getAllPaymentsForInvoiceSync(payment.invoiceId)
        
        val totalPaid = payments.sumOf { it.amountPaid }
        val newDueAmount = invoice.grandTotal - totalPaid
        
        val newStatus = when {
            newDueAmount <= 0 -> "PAID"
            totalPaid <= 0 -> "UNPAID"
            else -> "PARTIAL"
        }
        
        updateInvoice(invoice.copy(
            paidAmount = totalPaid,
            dueAmount = if (newDueAmount < 0) 0.0 else newDueAmount,
            paymentStatus = newStatus
        ))
    }

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId")
    suspend fun getAllPaymentsForInvoiceSync(invoiceId: Long): List<PaymentEntity>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :invoiceId")
    fun getInvoiceWithItemsFlow(invoiceId: Long): Flow<InvoiceWithItems>

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteInvoiceItems(invoiceId: Long)

    @Transaction
    suspend fun deleteFullInvoice(invoice: InvoiceEntity) {
        deleteInvoiceItems(invoice.id)
        deleteInvoice(invoice)
    }

    @Query("SELECT * FROM invoice_items WHERE userId = :userId")
    suspend fun getAllInvoiceItems(userId: String): List<InvoiceItemEntity>
}

data class ProductStats(val name: String, val count: Int)
data class MonthlyRevenue(val month: String, val total: Double)
data class MonthlyProfit(val month: String, val total: Double)
data class ProductProfit(val name: String, val profit: Double)
data class CustomerInsight(
    val name: String,
    val totalSpending: Double,
    val invoiceCount: Int,
    val lastPurchaseDate: Long
)
data class CustomerDue(val name: String, val totalDue: Double)
data class ProductInsight(val name: String, val profit: Double, val totalSales: Int, val totalRevenue: Double)
data class InactiveCustomer(val name: String, val lastDate: Long)
