package com.example.gstbillingapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gstbillingapp.ui.viewmodel.InvoiceViewModel
import com.example.gstbillingapp.utils.PdfGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceScreen(viewModel: InvoiceViewModel, onViewHistory: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var customerName by remember { mutableStateOf("") }
    var customerGstin by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var gstRate by remember { mutableStateOf("18") }
    
    var paidAmount by remember { mutableStateOf("") }
    var dueDate by remember { mutableLongStateOf(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000) }

    val currentItems by viewModel.currentInvoiceItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var itemSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var customerSuggestions by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(itemName) {
        if (itemName.length >= 2) {
            itemSuggestions = viewModel.getProductSuggestions(itemName)
        } else {
            itemSuggestions = emptyList()
        }
    }

    LaunchedEffect(customerName) {
        if (customerName.length >= 2) {
            customerSuggestions = viewModel.getCustomerSuggestions(customerName)
        } else {
            customerSuggestions = emptyList()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New Tax Invoice", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Customer Section
            item {
                Column {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (customerSuggestions.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            customerSuggestions.forEach { suggestion ->
                                Text(
                                    suggestion,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        customerName = suggestion
                                        customerSuggestions = emptyList()
                                    }.padding(12.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customerGstin,
                        onValueChange = { customerGstin = it },
                        label = { Text("Customer GSTIN (Optional)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }

            // Item Input Card
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column {
                            OutlinedTextField(
                                value = itemName,
                                onValueChange = { itemName = it },
                                label = { Text("Item Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (itemSuggestions.isNotEmpty()) {
                                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                                    itemSuggestions.forEach { suggestion ->
                                        Text(
                                            suggestion,
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                itemName = suggestion
                                                itemSuggestions = emptyList()
                                                scope.launch {
                                                    viewModel.getLastGstRate(suggestion)?.let { rate ->
                                                        gstRate = rate.toString()
                                                    }
                                                }
                                            }.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                            OutlinedTextField(value = costPrice, onValueChange = { costPrice = it }, label = { Text("Cost") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Qty") }, modifier = Modifier.weight(0.7f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = gstRate, onValueChange = { gstRate = it }, label = { Text("GST %") }, modifier = Modifier.weight(0.7f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                        
                        Button(onClick = {
                            if (itemName.isNotBlank() && price.isNotBlank()) {
                                viewModel.addItem(
                                    itemName, 
                                    price.toDoubleOrNull() ?: 0.0, 
                                    costPrice.toDoubleOrNull() ?: 0.0,
                                    quantity.toIntOrNull() ?: 1, 
                                    gstRate.toDoubleOrNull() ?: 18.0
                                )
                                itemName = ""; price = ""; costPrice = ""; quantity = ""
                            }
                        }, modifier = Modifier.align(Alignment.End)) {
                            Icon(Icons.Default.Add, null)
                            Text("Add to Invoice")
                        }
                    }
                }
            }

            // Payment Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = paidAmount,
                            onValueChange = { paidAmount = it },
                            label = { Text("Amount Paid") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { /* DatePickerDialog */ }) {
                            val dateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(dueDate))
                            Text("Due: $dateStr")
                        }
                    }
                }
            }

            // Items List
            items(currentItems) { item ->
                ListItem(
                    headlineContent = { Text(item.itemName) },
                    supportingContent = { Text("${item.quantity} x ₹${item.price} (${item.gstRate}% GST)") },
                    trailingContent = {
                        IconButton(onClick = { viewModel.removeItem(item) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
            
            if (currentItems.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text("No items added yet", color = Color.Gray)
                    }
                }
            }

            // Summary & Actions
            item {
                val subtotal = currentItems.sumOf { it.price * it.quantity }
                val gst = currentItems.sumOf { (it.price * it.quantity) * (it.gstRate / 100) }
                
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal")
                        Text("₹${String.format(Locale.getDefault(), "%.2f", subtotal)}")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GST Amount")
                        Text("₹${String.format(Locale.getDefault(), "%.2f", gst)}")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Grand Total", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("₹${String.format(Locale.getDefault(), "%.2f", subtotal + gst)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            val paid = paidAmount.toDoubleOrNull() ?: 0.0
                            viewModel.saveInvoice(customerName, customerGstin, paid, dueDate) { invoice, items ->
                                val pdfGenerator = PdfGenerator(context)
                                val file = pdfGenerator.generateInvoicePdf(invoice, items)
                                Toast.makeText(context, "Invoice Saved: ${file.name}", Toast.LENGTH_LONG).show()
                                customerName = ""; customerGstin = ""; paidAmount = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        enabled = currentItems.isNotEmpty() && customerName.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(12.dp))
                            Text("Generate & Save PDF", fontSize = 16.sp)
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
