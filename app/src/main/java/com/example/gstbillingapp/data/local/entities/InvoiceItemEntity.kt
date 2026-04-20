package com.example.gstbillingapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_items")
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "",
    val invoiceId: Long,
    val itemName: String,
    val quantity: Int,
    val price: Double,
    val costPrice: Double = 0.0,
    val gstRate: Double
)
