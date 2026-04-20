package com.example.gstbillingapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_settings")
data class BusinessSettings(
    @PrimaryKey val userId: String,
    val companyName: String = "",
    val gstNumber: String = "",
    val address: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val logoPath: String? = null,
    val signaturePath: String? = null
)
