package com.example.gstbillingapp.ui.screens

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gstbillingapp.data.local.dao.CustomerDue
import com.example.gstbillingapp.data.local.dao.CustomerInsight
import com.example.gstbillingapp.data.local.dao.InactiveCustomer
import com.example.gstbillingapp.data.local.dao.MonthlyProfit
import com.example.gstbillingapp.data.local.dao.ProductInsight
import com.example.gstbillingapp.data.local.dao.ProductProfit
import com.example.gstbillingapp.ui.viewmodel.InvoiceViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: InvoiceViewModel) {
    val totalRevenue by viewModel.totalRevenue.collectAsState()
    val totalGst by viewModel.totalGst.collectAsState()
    val totalProfit by viewModel.totalProfit.collectAsState()
    val totalPending by viewModel.totalPendingAmount.collectAsState()
    val overdueCount by viewModel.overdueCount.collectAsState()
    
    val customerDues by viewModel.customerDues.collectAsState()
    val highProfitLowSales by viewModel.highProfitLowSales.collectAsState()
    val inactiveCustomers by viewModel.inactiveCustomers.collectAsState()
    val largeInvoices by viewModel.largeInvoices.collectAsState()

    val topProfitableProducts by viewModel.topProfitableProducts.collectAsState()
    val topCustomers by viewModel.topCustomers.collectAsState()
    val monthlyProfit by viewModel.monthlyProfit.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Business Dashboard") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Main Profit Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Profit", style = MaterialTheme.typography.titleSmall)
                            Text("₹${String.format("%.2f", totalProfit ?: 0.0)}", 
                                style = MaterialTheme.typography.headlineMedium, 
                                fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Payment Summary
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Pending", "₹${String.format("%.0f", totalPending ?: 0.0)}", Icons.Default.PendingActions, Color(0xFFF44336), Modifier.weight(1f))
                    StatCard("Overdue", "$overdueCount Invoices", Icons.Default.Warning, Color(0xFFFF9800), Modifier.weight(1f))
                }
            }

            // Revenue & GST
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Revenue", "₹${String.format("%.0f", totalRevenue ?: 0.0)}", Icons.Default.Payments, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    StatCard("GST Coll.", "₹${String.format("%.0f", totalGst ?: 0.0)}", Icons.Default.AccountBalanceWallet, Color(0xFF4CAF50), Modifier.weight(1f))
                }
            }

            // Smart Insights Section
            item {
                Text("Smart Business Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (customerDues.isEmpty() && highProfitLowSales.isEmpty() && inactiveCustomers.isEmpty() && largeInvoices.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text("No critical alerts today. Business is running smoothly!", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            items(customerDues) { due ->
                InsightCard("Payment Alert", "Customer ${due.name} has pending dues of ₹${String.format("%.0f", due.totalDue)}", Icons.Default.NotificationImportant, Color(0xFFF44336))
            }

            items(highProfitLowSales) { product ->
                val profitMargin = if (product.totalRevenue > 0) (product.profit / product.totalRevenue) * 100 else 0.0
                val message = remember(product.name) {
                    generateDynamicProductTip(product.name, profitMargin, product.totalSales)
                }
                InsightCard("Product Tip", message, Icons.Default.TipsAndUpdates, Color(0xFFFF9800))
            }

            items(inactiveCustomers) { customer ->
                InsightCard("Inactive Customer", "${customer.name} hasn't purchased in 30+ days. Follow up?", Icons.Default.PersonSearch, Color(0xFF2196F3))
            }

            items(largeInvoices) { inv ->
                InsightCard("Large Sale", "Invoice ${inv.invoiceNumber} for ${inv.customerName} is over ₹50,000", Icons.Default.Star, Color(0xFF9C27B0))
            }

            // Profit Chart
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Monthly Profit Trends", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        if (monthlyProfit.isNotEmpty()) {
                            AndroidView(
                                factory = { context ->
                                    BarChart(context).apply {
                                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600)
                                        description.isEnabled = false
                                        setDrawGridBackground(false)
                                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                                        xAxis.granularity = 1f
                                        axisRight.isEnabled = false
                                        legend.isEnabled = false
                                    }
                                },
                                update = { chart ->
                                    val entries = monthlyProfit.mapIndexed { index, data: MonthlyProfit -> 
                                        BarEntry(index.toFloat(), data.total.toFloat()) 
                                    }
                                    val dataSet = BarDataSet(entries, "Profit").apply {
                                        color = Color(0xFF4CAF50).toArgb()
                                        valueTextSize = 10f
                                    }
                                    chart.data = BarData(dataSet)
                                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(monthlyProfit.map { it.month })
                                    chart.invalidate()
                                },
                                modifier = Modifier.fillMaxWidth().height(250.dp)
                            )
                        } else {
                            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("Insufficient data for chart", color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Customer Intelligence
            item {
                Text("Top Performing Customers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (topCustomers.isEmpty()) {
                item { Text("No customer data yet", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp)) }
            } else {
                items(topCustomers) { customer ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customer.name, fontWeight = FontWeight.Bold)
                                Text("${customer.invoiceCount} Invoices • Last purchase: ${SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(customer.lastPurchaseDate))}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("₹${String.format("%.0f", customer.totalSpending)}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

fun generateDynamicProductTip(name: String, margin: Double, sales: Int): String {
    if (sales == 0) return "Product '$name' is not selling at all. Immediate action needed."
    if (margin > 40) return "Very high margin product '$name' (${String.format("%.0f", margin)}%). Focus on selling more."

    val templates = listOf(
        "Product '$name' has high profit but low sales ($sales). Consider promoting it.",
        "'$name' is highly profitable (${String.format("%.0f", margin)}% margin) but not selling much. Push marketing.",
        "Low sales ($sales) detected for '$name' despite good margins.",
        "'$name' can generate more revenue if promoted better. Current sales: $sales.",
        "High margin product '$name' is underperforming with only $sales sales."
    )
    
    return templates.random()
}

@Composable
fun InsightCard(title: String, message: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.labelLarge)
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color)
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = Color.DarkGray)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
