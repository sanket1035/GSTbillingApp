package com.example.gstbillingapp.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.gstbillingapp.data.local.entities.InvoiceItemEntity
import com.example.gstbillingapp.data.local.entities.PaymentEntity
import com.example.gstbillingapp.ui.viewmodel.InvoiceViewModel
import com.example.gstbillingapp.utils.PdfGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: Long,
    viewModel: InvoiceViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val invoiceWithItems by viewModel.getInvoiceWithItemsFlow(invoiceId).collectAsState(initial = null)
    val settings by viewModel.businessSettings.collectAsState()
    var showPaymentDialog by remember { mutableStateOf(false) }

    if (showPaymentDialog) {
        RecordPaymentDialog(
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount ->
                viewModel.recordPayment(invoiceId, amount)
                showPaymentDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    invoiceWithItems?.let { data ->
                        IconButton(onClick = { 
                            val file = PdfGenerator(context).generateInvoicePdf(data.invoice, data.items, settings)
                            openPdf(context, file)
                        }) { Icon(Icons.Default.PictureAsPdf, "View") }
                        
                        IconButton(onClick = {
                            val file = PdfGenerator(context).generateInvoicePdf(data.invoice, data.items, settings)
                            sharePdf(context, file)
                        }) { Icon(Icons.Default.Share, "Share") }
                    }
                }
            )
        }
    ) { padding ->
        invoiceWithItems?.let { data ->
            val invoice = data.invoice
            val items = data.items

            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Info
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(invoice.invoiceNumber, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                PaymentStatusBadge(invoice.paymentStatus)
                            }
                            Text(SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(invoice.date)), style = MaterialTheme.typography.bodySmall)
                            
                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                            
                            Text("Customer Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text(invoice.customerName, style = MaterialTheme.typography.titleMedium)
                            if (invoice.customerGstin.isNotBlank()) {
                                Text("GSTIN: ${invoice.customerGstin}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                        }
                    }
                }

                // Payment Info
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Payment Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                if (invoice.dueAmount > 0) {
                                    TextButton(onClick = { showPaymentDialog = true }) {
                                        Icon(Icons.Default.Add, null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Record Payment")
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            PaymentRow("Paid Amount", "₹${String.format("%.2f", invoice.paidAmount)}", Color(0xFF4CAF50))
                            PaymentRow("Due Amount", "₹${String.format("%.2f", invoice.dueAmount)}", if (invoice.dueAmount > 0) Color.Red else Color.Gray)
                            PaymentRow("Due Date", SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(invoice.dueDate)), Color.Gray)
                        }
                    }
                }

                // Payment History Section
                if (data.payments.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Payment History", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "${data.payments.size} Recorded",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    items(data.payments.sortedByDescending { it.paymentDate }) { payment ->
                        PaymentHistoryRow(payment)
                    }
                }

                // Items List
                item {
                    Text("Items", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                items(items) { item ->
                    ItemDetailRow(item)
                }

                // Financial Summary
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SummaryRow("Subtotal", "₹${String.format("%.2f", invoice.subTotal)}")
                            SummaryRow("CGST", "₹${String.format("%.2f", invoice.cgst)}")
                            SummaryRow("SGST", "₹${String.format("%.2f", invoice.sgst)}")
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            SummaryRow("Grand Total", "₹${String.format("%.2f", invoice.grandTotal)}", isBold = true)
                            
                            val profit = items.sumOf { (it.price - it.costPrice) * it.quantity }
                            if (profit != 0.0) {
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Text("Est. Profit: ₹${String.format("%.2f", profit)}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                item {
                    Button(
                        onClick = { viewModel.deleteInvoice(invoice); onBack() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Invoice")
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun PaymentHistoryRow(payment: PaymentEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("₹${String.format("%.2f", payment.amountPaid)}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                Text(
                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(payment.paymentDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun RecordPaymentDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                label = { Text("Amount Paid") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            )
        },
        confirmButton = {
            Button(onClick = { amount.toDoubleOrNull()?.let { onConfirm(it) } }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PaymentStatusBadge(status: String) {
    val color = when (status) {
        "PAID" -> Color(0xFF4CAF50)
        "PARTIAL" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(status, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun ItemDetailRow(item: InvoiceItemEntity) {
    Card(modifier = Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.itemName, fontWeight = FontWeight.Bold)
                Text("₹${String.format("%.2f", item.price * item.quantity)}")
            }
            Text("${item.quantity} x ₹${item.price} + ${item.gstRate}% GST", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PaymentRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun SummaryRow(label: String, value: String, isBold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, fontSize = if (isBold) 18.sp else 16.sp)
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
