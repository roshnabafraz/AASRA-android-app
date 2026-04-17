package com.roshnab.aasra.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
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
import com.roshnab.aasra.data.ProfileViewModel
import com.roshnab.aasra.R
import androidx.compose.ui.res.stringResource

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

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isSaving = true
            
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    viewModel.uploadProfileImage(bytes) { success, message ->
                        isSaving = false
                        if (success) {
                            Toast.makeText(context, message ?: "Profile Picture Updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, message ?: "Failed to upload image", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    isSaving = false
                    Toast.makeText(context, "Failed to read image data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isSaving = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_profile)) },
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
            Text(stringResource(R.string.personal_information), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(selectedImageUri, state.photoUrl) {
                        val bytes: ByteArray? = when {
                            selectedImageUri != null -> {
                                try {
                                    context.contentResolver.openInputStream(selectedImageUri!!)?.readBytes()
                                } catch (e: Exception) { null }
                            }
                            state.photoUrl.startsWith("data:image") -> {
                                try {
                                    Base64.decode(state.photoUrl.substringAfter(","), Base64.DEFAULT)
                                } catch (e: Exception) { null }
                            }
                            else -> null
                        }
                        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Filled.Person, null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(stringResource(R.string.full_name)) },
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

            Text(stringResource(R.string.security), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text(stringResource(R.string.new_password)) },
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(R.string.confirm_new_password)) },
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
                Text(stringResource(R.string.change_password))
            }
        }
    }
}