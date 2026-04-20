package com.example.gstbillingapp.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.gstbillingapp.data.local.entities.InvoiceEntity
import com.example.gstbillingapp.ui.viewmodel.InvoiceViewModel
import com.example.gstbillingapp.ui.viewmodel.SortOrder
import com.example.gstbillingapp.utils.BackupUtils
import com.example.gstbillingapp.utils.PdfGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceHistoryScreen(
    viewModel: InvoiceViewModel, 
    onBack: () -> Unit,
    onViewInvoice: (Long) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()
    val settings by viewModel.businessSettings.collectAsState()
    
    var filterStatus by remember { mutableStateOf<String?>(null) }
    
    val filteredInvoices = remember(invoices, filterStatus) {
        if (filterStatus == null) invoices 
        else invoices.filter { it.paymentStatus == filterStatus }
    }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSearching by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var invoiceToDelete by remember { mutableStateOf<InvoiceEntity?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val createCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            scope.launch {
                val data = viewModel.getAllDataForExport()
                val success = BackupUtils.exportToCsv(context, it, data.first, data.second)
                Toast.makeText(context, if (success) "Exported successfully" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                val data = viewModel.getAllDataForExport()
                val success = BackupUtils.backupToJson(context, it, data.first, data.second)
                Toast.makeText(context, if (success) "Backup created" else "Backup failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                val data = BackupUtils.restoreFromJson(context, it)
                if (data != null) {
                    viewModel.restoreData(data.first, data.second)
                    Toast.makeText(context, "Data restored successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Restore failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (isSearching) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearching = false; viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Invoice History") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    },
                    actions = {
                        IconButton(onClick = { isSearching = true }) { Icon(Icons.Default.Search, null) }
                        IconButton(onClick = { showDatePicker = true }) { 
                            Icon(Icons.Default.DateRange, null, tint = if (dateRange != null) MaterialTheme.colorScheme.primary else LocalContentColor.current) 
                        }
                        IconButton(onClick = { showSortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, null) }
                        
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Latest First") },
                                onClick = { viewModel.setSortOrder(SortOrder.LATEST); showSortMenu = false },
                                trailingIcon = { if (sortOrder == SortOrder.LATEST) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Highest Amount") },
                                onClick = { viewModel.setSortOrder(SortOrder.HIGHEST_AMOUNT); showSortMenu = false },
                                trailingIcon = { if (sortOrder == SortOrder.HIGHEST_AMOUNT) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Lowest Amount") },
                                onClick = { viewModel.setSortOrder(SortOrder.LOWEST_AMOUNT); showSortMenu = false },
                                trailingIcon = { if (sortOrder == SortOrder.LOWEST_AMOUNT) Icon(Icons.Default.Check, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = { createCsvLauncher.launch("invoices.csv"); showSortMenu = false },
                                leadingIcon = { Icon(Icons.Default.FileDownload, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Backup (JSON)") },
                                onClick = { createBackupLauncher.launch("backup.json"); showSortMenu = false },
                                leadingIcon = { Icon(Icons.Default.Backup, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Restore (JSON)") },
                                onClick = { restoreBackupLauncher.launch(arrayOf("application/json")); showSortMenu = false },
                                leadingIcon = { Icon(Icons.Default.Restore, null) }
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterStatus == null,
                        onClick = { filterStatus = null },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = filterStatus == "PAID",
                        onClick = { filterStatus = "PAID" },
                        label = { Text("Paid") }
                    )
                    FilterChip(
                        selected = filterStatus == "PARTIAL",
                        onClick = { filterStatus = "PARTIAL" },
                        label = { Text("Partial") }
                    )
                    FilterChip(
                        selected = filterStatus == "UNPAID",
                        onClick = { filterStatus = "UNPAID" },
                        label = { Text("Unpaid") }
                    )
                }

                if (dateRange != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                            Text(
                                "Filtering: ${sdf.format(Date(dateRange!!.first))} - ${sdf.format(Date(dateRange!!.second))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            IconButton(onClick = { viewModel.clearDateRange() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (filteredInvoices.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(if (searchQuery.isEmpty()) "No invoices found" else "No matches for '$searchQuery'", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxSize()) {
                        items(filteredInvoices, key = { it.id }) { invoice ->
                            InvoiceItemRow(
                                invoice = invoice,
                                onClick = { onViewInvoice(invoice.id) },
                                onShare = {
                                    scope.launch {
                                        val data = viewModel.getInvoiceWithItems(invoice.id)
                                        sharePdf(context, PdfGenerator(context).generateInvoicePdf(data.invoice, data.items, settings))
                                    }
                                },
                                onView = {
                                    scope.launch {
                                        val data = viewModel.getInvoiceWithItems(invoice.id)
                                        openPdf(context, PdfGenerator(context).generateInvoicePdf(data.invoice, data.items, settings))
                                    }
                                },
                                onDelete = { invoiceToDelete = invoice }
                            )
                        }
                    }
                }
            }
            
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (showDatePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        viewModel.setDateRange(start, end)
                    }
                    showDatePicker = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(state = dateRangePickerState, modifier = Modifier.weight(1f))
        }
    }

    if (invoiceToDelete != null) {
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = { Text("Delete Invoice") },
            text = { Text("Are you sure you want to delete invoice ${invoiceToDelete?.invoiceNumber}?") },
            confirmButton = {
                TextButton(onClick = {
                    invoiceToDelete?.let { viewModel.deleteInvoice(it) }
                    invoiceToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun InvoiceItemRow(
    invoice: InvoiceEntity, 
    onClick: () -> Unit,
    onShare: () -> Unit, 
    onView: () -> Unit, 
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(invoice.invoiceNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    PaymentStatusSmallBadge(invoice.paymentStatus)
                }
                Text(invoice.customerName, style = MaterialTheme.typography.bodyMedium)
                Text("₹${String.format(Locale.getDefault(), "%.2f", invoice.grandTotal)}", 
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Row {
                IconButton(onClick = onShare) { Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun PaymentStatusSmallBadge(status: String) {
    val color = when (status) {
        "PAID" -> Color(0xFF4CAF50)
        "PARTIAL" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(status, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

private fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Invoice"))
}

private fun openPdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}
