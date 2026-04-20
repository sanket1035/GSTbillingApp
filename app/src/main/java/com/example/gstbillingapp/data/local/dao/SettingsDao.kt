package com.example.gstbillingapp.data.local.dao

import androidx.room.*
import com.example.gstbillingapp.data.local.entities.BusinessSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM business_settings WHERE userId = :userId")
    fun getSettings(userId: String): Flow<BusinessSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: BusinessSettings)
}
