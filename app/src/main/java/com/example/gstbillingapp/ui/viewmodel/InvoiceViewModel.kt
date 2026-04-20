package com.example.gstbillingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gstbillingapp.data.local.dao.*
import com.example.gstbillingapp.data.local.entities.*
import com.example.gstbillingapp.data.repository.InvoiceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOrder {
    LATEST, HIGHEST_AMOUNT, LOWEST_AMOUNT
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class InvoiceViewModel(private val repository: InvoiceRepository) : ViewModel() {

    private val _userId = MutableStateFlow("")
    private val userId: String get() = _userId.value

    fun setUserId(id: String) {
        _userId.value = id
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val dateRange = _dateRange.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.LATEST)
    val sortOrder = _sortOrder.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    @OptIn(FlowPreview::class)
    val invoices = combine(
        _searchQuery.debounce(300),
        _dateRange,
        _sortOrder,
        _userId
    ) { query, range, sort, id ->
        if (id.isEmpty()) return@combine emptyList<InvoiceEntity>()
        
        _isLoading.value = true
        val result = if (range != null) {
            repository.getInvoicesByDateRange(id, range.first, range.second).first()
        } else if (query.isEmpty()) {
            repository.getAllInvoices(id).first()
        } else {
            repository.searchInvoices(id, query).first()
        }

        val sortedResult = when (sort) {
            SortOrder.LATEST -> result.sortedByDescending { it.date }
            SortOrder.HIGHEST_AMOUNT -> result.sortedByDescending { it.grandTotal }
            SortOrder.LOWEST_AMOUNT -> result.sortedBy { it.grandTotal }
        }
        _isLoading.value = false
        sortedResult
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics
    val totalRevenue = _userId.flatMapLatest { id ->
        repository.getTotalRevenue(id).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalGst = _userId.flatMapLatest { id ->
        repository.getTotalGst(id).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val topProducts = _userId.flatMapLatest { id ->
        repository.getTopProducts(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyRevenue = _userId.flatMapLatest { id ->
        repository.getMonthlyRevenue(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Profit Analytics
    val totalProfit = _userId.flatMapLatest { id ->
        repository.getTotalProfit(id).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyProfit = _userId.flatMapLatest { id ->
        repository.getMonthlyProfit(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<MonthlyProfit>())

    val topProfitableProducts = _userId.flatMapLatest { id ->
        repository.getTopProfitableProducts(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<ProductProfit>())

    // Customer Intelligence
    val topCustomers = _userId.flatMapLatest { id ->
        repository.getTopCustomers(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<CustomerInsight>())

    // Payment Tracking
    val totalPendingAmount = _userId.flatMapLatest { id ->
        repository.getTotalPendingAmount(id).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val overdueCount = _userId.flatMapLatest { id ->
        repository.getOverdueCount(id, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Insights
    val customerDues = _userId.flatMapLatest { id ->
        repository.getCustomerDues(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val highProfitLowSales = _userId.flatMapLatest { id ->
        repository.getHighProfitLowSales(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inactiveCustomers = _userId.flatMapLatest { id ->
        repository.getInactiveCustomers(id, System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val largeInvoices = _userId.flatMapLatest { id ->
        repository.getLargeInvoices(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    val businessSettings = _userId.flatMapLatest { id ->
        repository.getSettings(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveSettings(settings: BusinessSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings.copy(userId = userId))
        }
    }

    private val _currentInvoiceItems = MutableStateFlow<List<InvoiceItemEntity>>(emptyList())
    val currentInvoiceItems = _currentInvoiceItems.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setDateRange(start: Long, end: Long) {
        _dateRange.value = Pair(start, end)
    }

    fun clearDateRange() {
        _dateRange.value = null
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun addItem(name: String, price: Double, costPrice: Double, qty: Int, gst: Double) {
        val newItem = InvoiceItemEntity(
            userId = userId,
            invoiceId = 0,
            itemName = name,
            quantity = qty,
            price = price,
            costPrice = costPrice,
            gstRate = gst
        )
        _currentInvoiceItems.value += newItem
    }

    fun removeItem(item: InvoiceItemEntity) {
        _currentInvoiceItems.value -= item
    }

    fun saveInvoice(
        customerName: String, 
        customerGstin: String, 
        paidAmount: Double, 
        dueDate: Long,
        onComplete: (InvoiceEntity, List<InvoiceItemEntity>) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val items = _currentInvoiceItems.value
            val subTotal = items.sumOf { it.price * it.quantity }
            val gst = items.sumOf { (it.price * it.quantity) * (it.gstRate / 100) }
            val grandTotal = subTotal + gst
            
            val paymentStatus = when {
                paidAmount >= grandTotal -> "PAID"
                paidAmount <= 0 -> "UNPAID"
                else -> "PARTIAL"
            }
            
            val invoice = InvoiceEntity(
                userId = userId,
                invoiceNumber = repository.getNextInvoiceNumber(userId),
                customerName = customerName,
                customerGstin = customerGstin,
                date = System.currentTimeMillis(),
                subTotal = subTotal,
                cgst = gst / 2,
                sgst = gst / 2,
                grandTotal = grandTotal,
                paymentStatus = paymentStatus,
                paidAmount = paidAmount,
                dueAmount = grandTotal - paidAmount,
                dueDate = dueDate
            )
            
            val id = repository.insertInvoice(invoice)
            val finalItems = items.map { it.copy(invoiceId = id, userId = userId) }
            repository.insertItems(finalItems)

            if (paidAmount > 0) {
                repository.recordPayment(
                    PaymentEntity(
                        userId = userId,
                        invoiceId = id,
                        amountPaid = paidAmount,
                        paymentDate = System.currentTimeMillis()
                    )
                )
            }
            
            _currentInvoiceItems.value = emptyList()
            _isLoading.value = false
            onComplete(invoice.copy(id = id), finalItems)
        }
    }

    fun getInvoiceWithItemsFlow(id: Long): Flow<InvoiceWithItems> = repository.getInvoiceWithItemsFlow(id)

    fun deleteInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
        }
    }

    fun recordPayment(invoiceId: Long, amount: Double) {
        viewModelScope.launch {
            val payment = PaymentEntity(
                userId = userId,
                invoiceId = invoiceId,
                amountPaid = amount,
                paymentDate = System.currentTimeMillis()
            )
            repository.recordPayment(payment)
        }
    }

    suspend fun getInvoiceWithItems(id: Long): InvoiceWithItems {
        return repository.getInvoiceWithItems(id)
    }

    suspend fun getProductSuggestions(query: String) = repository.getProductNameSuggestions(userId, query)
    suspend fun getCustomerSuggestions(query: String) = repository.getCustomerNameSuggestions(userId, query)
    suspend fun getLastGstRate(itemName: String) = repository.getLastGstRate(userId, itemName)

    suspend fun getAllDataForExport(): Pair<List<InvoiceEntity>, List<InvoiceItemEntity>> {
        return Pair(repository.getAllInvoicesList(userId), repository.getAllInvoiceItems(userId))
    }

    fun restoreData(invoices: List<InvoiceEntity>, items: List<InvoiceItemEntity>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.insertInvoices(invoices.map { it.copy(userId = userId) })
            repository.insertItems(items.map { it.copy(userId = userId) })
            _isLoading.value = false
        }
    }
}
