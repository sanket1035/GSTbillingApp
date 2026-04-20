package com.example.gstbillingapp.data.repository

import com.example.gstbillingapp.data.local.dao.*
import com.example.gstbillingapp.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val settingsDao: SettingsDao
) {
    fun getSettings(userId: String): Flow<BusinessSettings?> = settingsDao.getSettings(userId)
    
    suspend fun saveSettings(settings: BusinessSettings) = settingsDao.saveSettings(settings)

    fun getAllInvoices(userId: String): Flow<List<InvoiceEntity>> = invoiceDao.getAllInvoices(userId)
    
    fun searchInvoices(userId: String, query: String): Flow<List<InvoiceEntity>> = invoiceDao.searchInvoices(userId, query)
    
    suspend fun insertInvoice(invoice: InvoiceEntity): Long = invoiceDao.insertInvoice(invoice)
    
    suspend fun insertItems(items: List<InvoiceItemEntity>) = invoiceDao.insertItems(items)
    
    suspend fun getInvoiceWithItems(id: Long): InvoiceWithItems = invoiceDao.getInvoiceWithItems(id)
    
    fun getTotalRevenue(userId: String): Flow<Double?> = invoiceDao.getTotalRevenue(userId)
    
    fun getTotalGst(userId: String): Flow<Double?> = invoiceDao.getTotalGstCollected(userId)
    
    fun getTopProducts(userId: String): Flow<List<ProductStats>> = invoiceDao.getTopProducts(userId)
    
    fun getMonthlyRevenue(userId: String): Flow<List<MonthlyRevenue>> = invoiceDao.getMonthlyRevenue(userId)

    fun getTotalProfit(userId: String): Flow<Double?> = invoiceDao.getTotalProfit(userId)
    
    fun getMonthlyProfit(userId: String): Flow<List<MonthlyProfit>> = invoiceDao.getMonthlyProfit(userId)
    
    fun getTopProfitableProducts(userId: String): Flow<List<ProductProfit>> = invoiceDao.getTopProfitableProducts(userId)
    
    fun getTopCustomers(userId: String): Flow<List<CustomerInsight>> = invoiceDao.getTopCustomers(userId)
    
    suspend fun getProductNameSuggestions(userId: String, query: String): List<String> = invoiceDao.getProductNameSuggestions(userId, query)
    
    suspend fun getCustomerNameSuggestions(userId: String, query: String): List<String> = invoiceDao.getCustomerNameSuggestions(userId, query)
    
    suspend fun getLastGstRate(userId: String, itemName: String): Double? = invoiceDao.getLastGstRateForItem(userId, itemName)

    fun getInvoicesByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<InvoiceEntity>> = 
        invoiceDao.getInvoicesByDateRange(userId, startDate, endDate)

    suspend fun deleteInvoice(invoice: InvoiceEntity) = invoiceDao.deleteFullInvoice(invoice)

    fun getTotalPendingAmount(userId: String): Flow<Double?> = invoiceDao.getTotalPendingAmount(userId)
    
    fun getOverdueCount(userId: String, today: Long): Flow<Int> = invoiceDao.getOverdueCount(userId, today)
    
    fun getOverdueInvoices(userId: String, today: Long): Flow<List<InvoiceEntity>> = invoiceDao.getOverdueInvoices(userId, today)
    
    fun getCustomerDues(userId: String): Flow<List<CustomerDue>> = invoiceDao.getCustomerDues(userId)
    
    fun getHighProfitLowSales(userId: String): Flow<List<ProductInsight>> = invoiceDao.getHighProfitLowSalesProducts(userId)
    
    fun getInactiveCustomers(userId: String, threshold: Long): Flow<List<InactiveCustomer>> = invoiceDao.getInactiveCustomers(userId, threshold)
    
    fun getLargeInvoices(userId: String): Flow<List<InvoiceEntity>> = invoiceDao.getLargeInvoices(userId)
    
    fun getInvoiceWithItemsFlow(id: Long): Flow<InvoiceWithItems> = invoiceDao.getInvoiceWithItemsFlow(id)

    suspend fun recordPayment(payment: PaymentEntity) = invoiceDao.recordPayment(payment)

    fun getPaymentsForInvoice(invoiceId: Long): Flow<List<PaymentEntity>> = invoiceDao.getPaymentsForInvoice(invoiceId)

    suspend fun getAllInvoicesList(userId: String): List<InvoiceEntity> = invoiceDao.getAllInvoices(userId).first()

    suspend fun getAllInvoiceItems(userId: String): List<InvoiceItemEntity> = invoiceDao.getAllInvoiceItems(userId)

    suspend fun insertInvoices(invoices: List<InvoiceEntity>) {
        invoices.forEach { invoiceDao.insertInvoice(it) }
    }
    
    suspend fun getNextInvoiceNumber(userId: String): String {
        val count = invoiceDao.getInvoiceCount(userId)
        return "INV-${String.format(java.util.Locale.getDefault(), "%03d", count + 1)}"
    }
}
