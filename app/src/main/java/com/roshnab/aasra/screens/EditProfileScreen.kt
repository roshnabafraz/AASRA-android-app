package com.roshnab.aasra.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roshnab.aasra.data.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel
) {
    val context = LocalContext.current
    val state = viewModel.uiState

    var newName by remember { mutableStateOf(state.name) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Personal Information", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        isSaving = true
                        viewModel.updateUserName(newName) { success ->
                            isSaving = false
                            if (success) Toast.makeText(context, "Name Updated!", Toast.LENGTH_SHORT).show()
                            else Toast.makeText(context, "Update Failed", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Saving..." else "Update Name")
            }

            Divider()

            Text("Security", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm New Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Button(
                onClick = {
                    if (newPassword.length >= 6 && newPassword == confirmPassword) {
                        isSaving = true
                        viewModel.updateUserPassword(newPassword) { success, error ->
                            isSaving = false
                            if (success) {
                                Toast.makeText(context, "Password Changed!", Toast.LENGTH_SHORT).show()
                                newPassword = ""
                                confirmPassword = ""
                            } else {
                                Toast.makeText(context, error ?: "Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Passwords must match & be 6+ chars", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = !isSaving
            ) {
                Text("Change Password")
            }
        }
    }
}