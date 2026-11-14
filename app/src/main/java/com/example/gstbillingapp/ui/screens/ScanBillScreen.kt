package com.example.gstbillingapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.gstbillingapp.ui.viewmodel.InvoiceViewModel
import com.example.gstbillingapp.utils.BillOcr
import com.example.gstbillingapp.utils.BillTextParser
import com.example.gstbillingapp.utils.GstRates
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/** Editable row on the review step. Everything is a String so the user can fix any OCR mistake. */
private class ReviewRow(
    name: String,
    qty: String,
    price: String,
    gst: String,
    suggested: Boolean,
) {
    var name by mutableStateOf(name)
    var qty by mutableStateOf(qty)
    var price by mutableStateOf(price)
    var gst by mutableStateOf(gst)
    var suggested by mutableStateOf(suggested)
    var gstEditedByUser = false
}

/**
 * Scan Bill: photo -> ML Kit OCR -> editable text -> editable items with suggested GST % -> invoice form.
 * Nothing is saved to the database here; items are only handed to the normal invoice form.
 */
@Composable
fun ScanBillScreen(viewModel: InvoiceViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) } // 1 = scan/text, 2 = review
    var ocrText by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val rows = remember { mutableStateListOf<ReviewRow>() }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun runOcr(uri: Uri) {
        busy = true
        status = "Reading the bill..."
        scope.launch {
            try {
                val text = BillOcr.readText(context, uri)
                ocrText = text
                status = if (text.isBlank()) "No text found. Type the items below instead."
                else "Check the text below. Handwriting can be misread."
            } catch (e: Exception) {
                status = "Could not read the photo (${e.message ?: "error"}). Type the items below."
            } finally {
                busy = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) runOcr(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok -> if (ok) cameraUri?.let { runOcr(it) } }

    fun newRow(name: String, qty: Int, price: Double): ReviewRow {
        val s = GstRates.suggest(name)
        return ReviewRow(
            name = name,
            qty = qty.toString(),
            price = if (price > 0) trimNumber(price) else "",
            gst = s?.let { trimNumber(it.rate) } ?: "",
            suggested = s != null,
        )
    }

    fun buildRows() {
        rows.clear()
        BillTextParser.parse(ocrText).forEach { rows.add(newRow(it.name, it.quantity, it.price)) }
        if (rows.isEmpty()) rows.add(newRow("", 1, 0.0))
        step = 2
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        if (step == 1) {
            item {
                Text("Scan a bill", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Take a photo or pick one from the gallery. Printed bills work best; handwriting is often misread.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraUri = uri
                            cameraLauncher.launch(uri)
                        }
                    ) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Camera")
                    }
                    OutlinedButton(
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            status?.let { item { Text(it, style = MaterialTheme.typography.bodySmall) } }
            item {
                OutlinedTextField(
                    value = ocrText,
                    onValueChange = { ocrText = it },
                    label = { Text("Bill text (edit freely, one item per line)") },
                    placeholder = { Text("Parle-G 5 x 10\nChawal 2 kg 60") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)
                )
            }
            item {
                Button(
                    onClick = { buildRows() },
                    enabled = !busy && ocrText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Make items") }
            }
        } else {
            item {
                Text("Review items", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = customer,
                    onValueChange = { customer = it },
                    label = { Text("Customer name (optional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        "Unverified: " + GstRates.UNVERIFIED_NOTE,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            itemsIndexed(rows) { index, row ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = row.name,
                                onValueChange = {
                                    row.name = it
                                    // Re-suggest only until the user has picked a rate themselves
                                    if (!row.gstEditedByUser) {
                                        val s = GstRates.suggest(it)
                                        row.gst = s?.let { x -> trimNumber(x.rate) } ?: ""
                                        row.suggested = s != null
                                    }
                                },
                                label = { Text("Item") },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { rows.removeAt(index) }) {
                                Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = row.qty, onValueChange = { row.qty = it },
                                label = { Text("Qty") }, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = row.price, onValueChange = { row.price = it },
                                label = { Text("Price (excl. GST)") }, modifier = Modifier.weight(1.4f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = row.gst,
                                onValueChange = {
                                    row.gst = it
                                    row.gstEditedByUser = true
                                    row.suggested = false
                                },
                                label = { Text("GST %") }, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = row.gst.toDoubleOrNull() == null
                            )
                        }
                        Text(
                            when {
                                row.suggested -> "GST suggested, not verified. Please confirm."
                                row.gst.toDoubleOrNull() == null -> "No suggestion for this item. Enter the GST % yourself."
                                else -> "GST set by you."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                TextButton(onClick = { rows.add(newRow("", 1, 0.0)) }) {
                    Icon(Icons.Default.Add, null)
                    Text("Add item")
                }
            }
            item {
                val ready = rows.isNotEmpty() && rows.all {
                    it.name.isNotBlank() &&
                        (it.qty.toIntOrNull() ?: 0) > 0 &&
                        (it.price.toDoubleOrNull() ?: -1.0) >= 0 &&
                        it.gst.toDoubleOrNull() != null
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = ready,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            rows.forEach {
                                viewModel.addItem(
                                    it.name.trim(),
                                    it.price.toDouble(),
                                    0.0,
                                    it.qty.toInt(),
                                    it.gst.toDouble()
                                )
                            }
                            if (customer.isNotBlank()) viewModel.setScannedCustomerName(customer.trim())
                            onDone()
                        }
                    ) { Text("Add to invoice") }
                    if (!ready) {
                        Text(
                            "Every item needs a name, quantity, price and a GST %.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedButton(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Back to text") }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

private fun trimNumber(v: Double): String =
    if (v == Math.floor(v)) v.toLong().toString() else String.format(Locale.US, "%.2f", v)
