package com.example.gstbillingapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "",
    val invoiceNumber: String,
    val customerName: String,
    val customerGstin: String,
    val date: Long,
    val subTotal: Double,
    val cgst: Double,
    val sgst: Double,
    val grandTotal: Double,
    val paymentStatus: String = "UNPAID", // PAID, UNPAID, PARTIAL
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val dueDate: Long = 0L
)
