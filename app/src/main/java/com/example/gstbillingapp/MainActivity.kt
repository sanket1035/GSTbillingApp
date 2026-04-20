package com.example.gstbillingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gstbillingapp.data.local.AppDatabase
import com.example.gstbillingapp.data.repository.InvoiceRepository
import com.example.gstbillingapp.ui.screens.*
import com.example.gstbillingapp.ui.theme.GSTBillingAppTheme
import com.example.gstbillingapp.ui.viewmodel.AuthViewModel
import com.example.gstbillingapp.ui.viewmodel.InvoiceViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = AppDatabase.getDatabase(this)
        val repository = InvoiceRepository(database.invoiceDao(), database.settingsDao())
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InvoiceViewModel(repository) as T
            }
        }

        setContent {
            GSTBillingAppTheme {
                val authViewModel: AuthViewModel = viewModel()
                val currentUser by authViewModel.currentUser.collectAsState()

                if (currentUser == null) {
                    LoginScreen(authViewModel)
                } else {
                    val viewModel: InvoiceViewModel = viewModel(factory = viewModelFactory)
                    viewModel.setUserId(currentUser?.uid ?: "")
                    MainAppContent(viewModel, authViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: InvoiceViewModel, authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "GST Billing Pro",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Dashboard, null) },
                    label = { Text("Dashboard") },
                    selected = currentRoute == "dashboard",
                    onClick = {
                        navController.navigate("dashboard")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Add, null) },
                    label = { Text("Create Invoice") },
                    selected = currentRoute == "create_invoice",
                    onClick = {
                        navController.navigate("create_invoice")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.History, null) },
                    label = { Text("Invoice History") },
                    selected = currentRoute == "history",
                    onClick = {
                        navController.navigate("history")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Settings") },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            googleSignInClient.signOut().addOnCompleteListener {
                                authViewModel.logout()
                            }
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                        Text(when(currentRoute) {
                            "dashboard" -> "Dashboard"
                            "create_invoice" -> "Create Invoice"
                            "history" -> "Invoice History"
                            "settings" -> "Settings"
                            else -> "GST Billing App"
                        })
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                if (currentRoute == "dashboard" || currentRoute == "history") {
                    FloatingActionButton(onClick = { navController.navigate("create_invoice") }) {
                        Icon(Icons.Default.Add, "Create Invoice")
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController, 
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("dashboard") { DashboardScreen(viewModel) }
                composable("create_invoice") {
                    CreateInvoiceScreen(viewModel, onViewHistory = {
                        navController.navigate("history")
                    })
                }
                composable("history") {
                    InvoiceHistoryScreen(
                        viewModel = viewModel, 
                        onBack = { navController.popBackStack() },
                        onViewInvoice = { id -> navController.navigate("invoice_detail/$id") }
                    )
                }
                composable("settings") { SettingsScreen(viewModel) }
                composable(
                    route = "invoice_detail/{invoiceId}",
                    arguments = listOf(navArgument("invoiceId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val invoiceId = backStackEntry.arguments?.getLong("invoiceId") ?: 0L
                    InvoiceDetailScreen(
                        invoiceId = invoiceId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
