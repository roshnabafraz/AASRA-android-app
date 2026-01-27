package com.roshnab.aasra.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roshnab.aasra.data.EmergencyContact
import com.roshnab.aasra.data.ProfileViewModel
import com.roshnab.aasra.data.SafeLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAddLocationClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onSupportClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = viewModel.uiState

    // --- DIALOG STATES ---
    var contactToDelete by remember { mutableStateOf<EmergencyContact?>(null) }
    var locationToDelete by remember { mutableStateOf<SafeLocation?>(null) }

    val contactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                viewModel.addContactFromUri(uri)
                Toast.makeText(context, "Contact Added!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactLauncher.launch(intent)
        } else {
            Toast.makeText(context, "Permission needed to add contacts", Toast.LENGTH_SHORT).show()
        }
    }

    var currentLanguage by remember { mutableStateOf("English") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Profile & Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProfileHeaderSection(
                name = if (state.name.isEmpty()) "Loading..." else state.name,
                email = if (state.email.isEmpty()) "..." else state.email,
                totalDonated = state.totalDonated
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Safety & Emergency")

                SettingsItem(
                    icon = Icons.Filled.PersonAdd,
                    title = "Add Emergency Contact",
                    subtitle = "Select from phone book"
                ) {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }

                if (state.emergencyContacts.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            state.emergencyContacts.forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(contact.name, fontWeight = FontWeight.Bold)
                                        Text(contact.number, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    IconButton(onClick = { contactToDelete = contact }) {
                                        Icon(Icons.Outlined.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (contact != state.emergencyContacts.last()) {
                                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Safe Locations")
                    TextButton(onClick = onAddLocationClick) {
                        Text("+ Add New", color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (state.safeLocations.isEmpty()) {
                    val msg = if (state.isLoading) "Loading locations..." else "No locations saved yet."
                    Text(msg, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 16.dp))
                } else {
                    state.safeLocations.forEach { loc ->
                        SettingsItem(
                            icon = Icons.Filled.LocationOn,
                            title = loc.name,
                            subtitle = "${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}"
                        ) {
                            locationToDelete = loc
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("App Preferences")

                // 1. Flood Alerts (Using Real ViewModel Data)
                ToggleItem(
                    icon = Icons.Filled.Notifications,
                    title = "Flood Alerts",
                    isChecked = state.areNotificationsEnabled
                ) { isChecked ->
                    viewModel.updateNotificationPreference(isChecked)
                }

                // 2. Dark Mode (Using Real App Theme Data)
                ToggleItem(
                    icon = Icons.Filled.DarkMode,
                    title = "Dark Mode",
                    isChecked = isDarkTheme // <--- USES PARAMETER
                ) { isChecked ->
                    onThemeChanged(isChecked) // <--- TRIGGERS MAIN ACTIVITY
                }

                SettingsItem(Icons.Filled.Language, "Language", currentLanguage) {
                    currentLanguage = if (currentLanguage == "English") "Urdu" else "English"
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Account & Support")

                SettingsItem(Icons.Filled.Edit, "Edit Profile", "Change name or password") {
                    onEditProfileClick()
                }

                SettingsItem(Icons.Filled.SupportAgent, "Help & Support", "Contact our team") {
                    onSupportClick()
                }

                SettingsItem(Icons.Outlined.Feedback, "Send Feedback", "Help us improve AASRA") {
                    sendFeedbackEmail(context)
                }
                LogoutItem(onLogoutClick)
            }

            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text("AASRA v1.0.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // --- WARNING DIALOGS ---

    if (contactToDelete != null) {
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Remove Contact?") },
            text = { Text("Are you sure you want to remove ${contactToDelete?.name} from your emergency contacts?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        contactToDelete?.let { viewModel.removeContact(it) }
                        contactToDelete = null
                        Toast.makeText(context, "Contact Removed", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (locationToDelete != null) {
        AlertDialog(
            onDismissRequest = { locationToDelete = null },
            title = { Text("Delete Location?") },
            text = { Text("Are you sure you want to delete '${locationToDelete?.name}' from your safe locations?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        locationToDelete?.let { viewModel.removeSafeLocation(it) }
                        locationToDelete = null
                        Toast.makeText(context, "Location Deleted", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { locationToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// --- HELPER FUNCTIONS ---

@Composable
fun ProfileHeaderSection(name: String, email: String, totalDonated: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ImpactStat("Tier", "Guardian ⭐")
                ImpactStat("Donated", "Rs. $totalDonated")
            }
        }
    }
}

@Composable
fun ImpactStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ToggleItem(icon: ImageVector, title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun LogoutItem(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Logout, null, tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.width(16.dp))
        Text("Log Out", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
    }
}

fun sendFeedbackEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@aasra.com"))
        putExtra(Intent.EXTRA_SUBJECT, "AASRA App Feedback")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
    }
}