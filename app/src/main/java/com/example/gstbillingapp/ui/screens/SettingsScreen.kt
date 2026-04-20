package com.example.gstbillingapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gstbillingapp.data.local.entities.BusinessSettings
import com.example.gstbillingapp.ui.viewmodel.InvoiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: InvoiceViewModel) {
    val settings by viewModel.businessSettings.collectAsState()
    val context = LocalContext.current
    
    var companyName by remember(settings) { mutableStateOf(settings?.companyName ?: "") }
    var gstNumber by remember(settings) { mutableStateOf(settings?.gstNumber ?: "") }
    var address by remember(settings) { mutableStateOf(settings?.address ?: "") }
    var phoneNumber by remember(settings) { mutableStateOf(settings?.phoneNumber ?: "") }
    var email by remember(settings) { mutableStateOf(settings?.email ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Business Settings") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.saveSettings(
                        BusinessSettings(
                            userId = "", // Will be filled by ViewModel
                            companyName = companyName,
                            gstNumber = gstNumber,
                            address = address,
                            phoneNumber = phoneNumber,
                            email = email
                        )
                    )
                    Toast.makeText(context, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                },
                icon = { Icon(Icons.Default.Save, null) },
                text = { Text("Save Settings") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Business, null) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = gstNumber,
                onValueChange = { gstNumber = it },
                label = { Text("GST Number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                minLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, null) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Email, null) }
            )
            
            Spacer(modifier = Modifier.height(100.dp)) // Space for FAB
        }
    }
}
