package com.example.gstbillingapp.utils

import android.content.Context
import android.net.Uri
import com.example.gstbillingapp.data.local.entities.InvoiceEntity
import com.example.gstbillingapp.data.local.entities.InvoiceItemEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.util.Locale

object BackupUtils {

    fun exportToCsv(context: Context, uri: Uri, invoices: List<InvoiceEntity>, items: List<InvoiceItemEntity>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write("Invoice Number,Customer Name,Date,Total Amount,CGST,SGST\n")
                    invoices.forEach { inv ->
                        writer.write("${inv.invoiceNumber},${inv.customerName},${inv.date},${inv.grandTotal},${inv.cgst},${inv.sgst}\n")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun backupToJson(context: Context, uri: Uri, invoices: List<InvoiceEntity>, items: List<InvoiceItemEntity>): Boolean {
        return try {
            val root = JSONObject()
            val invArray = JSONArray()
            invoices.forEach { inv ->
                val obj = JSONObject().apply {
                    put("id", inv.id)
                    put("invoiceNumber", inv.invoiceNumber)
                    put("customerName", inv.customerName)
                    put("customerGstin", inv.customerGstin)
                    put("date", inv.date)
                    put("subTotal", inv.subTotal)
                    put("cgst", inv.cgst)
                    put("sgst", inv.sgst)
                    put("grandTotal", inv.grandTotal)
                }
                invArray.put(obj)
            }
            
            val itemsArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("invoiceId", item.invoiceId)
                    put("itemName", item.itemName)
                    put("quantity", item.quantity)
                    put("price", item.price)
                    put("gstRate", item.gstRate)
                }
                itemsArray.put(obj)
            }
            
            root.put("invoices", invArray)
            root.put("items", itemsArray)

            context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString().toByteArray()) }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun restoreFromJson(context: Context, uri: Uri): Pair<List<InvoiceEntity>, List<InvoiceItemEntity>>? {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() } ?: return null
            val root = JSONObject(jsonString)
            
            val invoices = mutableListOf<InvoiceEntity>()
            val invArray = root.getJSONArray("invoices")
            for (i in 0 until invArray.length()) {
                val obj = invArray.getJSONObject(i)
                invoices.add(InvoiceEntity(
                    invoiceNumber = obj.getString("invoiceNumber"),
                    customerName = obj.getString("customerName"),
                    customerGstin = obj.getString("customerGstin"),
                    date = obj.getLong("date"),
                    subTotal = obj.getDouble("subTotal"),
                    cgst = obj.getDouble("cgst"),
                    sgst = obj.getDouble("sgst"),
                    grandTotal = obj.getDouble("grandTotal")
                ))
            }

            val items = mutableListOf<InvoiceItemEntity>()
            val itemsArray = root.getJSONArray("items")
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.getJSONObject(i)
                items.add(InvoiceItemEntity(
                    invoiceId = obj.getLong("invoiceId"),
                    itemName = obj.getString("itemName"),
                    quantity = obj.getInt("quantity"),
                    price = obj.getDouble("price"),
                    gstRate = obj.getDouble("gstRate")
                ))
            }
            Pair(invoices, items)
        } catch (e: Exception) {
            null
        }
    }
}
