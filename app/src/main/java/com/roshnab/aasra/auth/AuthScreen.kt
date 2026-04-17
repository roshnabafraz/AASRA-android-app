package com.roshnab.aasra.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.roshnab.aasra.R
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

enum class AuthMode { LOGIN, SIGN_UP, COMPLETE_PROFILE }

data class GoogleProfileData(val name: String, val email: String, val photoUrl: String)

class CnicVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        var out = ""
        for (i in text.indices) {
            out += text[i]
            if (i == 4 || i == 11) out += "-"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 11) return offset + 1
                return offset + 2
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 5) return offset
                if (offset <= 13) return offset - 1
                return offset - 2
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onAuthSuccess: () -> Unit
) {

    val context = LocalContext.current
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var googleProfileData by remember { mutableStateOf<GoogleProfileData?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val onGoogleSignInClick: () -> Unit = {
        coroutineScope.launch {
            try {
                val webClientId = "1048876079888-p249t5h202c6ul2574i4r2k178vv9t8l.apps.googleusercontent.com"

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(request = request, context = context)

                when (val credential = result.credential) {
                    is CustomCredential -> {
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                            viewModel.signInWithGoogle(
                                idToken = googleIdTokenCredential.idToken,
                                onSuccess = onAuthSuccess,
                                onNeedsProfile = { name, email, photoUrl ->
                                    googleProfileData = GoogleProfileData(name, email, photoUrl)
                                    authMode = AuthMode.COMPLETE_PROFILE
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                    else -> Log.e("Auth", "Unknown credential type")
                }
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign In Failed", e)
                Toast.makeText(context, "Google Sign In Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState()), // Allow scrolling for the longer signup form
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.aasra_logo),
                contentDescription = "AASRA Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp) // Slightly smaller to fit form
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Text(text = stringResource(R.string.aasra),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(targetState = authMode, label = "AuthToggle") { mode ->
                when (mode) {
                    AuthMode.LOGIN -> {
                        LoginContent(
                            viewModel = viewModel,
                            onGoogleSignIn = onGoogleSignInClick,
                            onToggleMode = { authMode = AuthMode.SIGN_UP },
                            onSuccess = onAuthSuccess
                        )
                    }
                    AuthMode.SIGN_UP -> {
                        SignUpContent(
                            viewModel = viewModel,
                            onGoogleSignIn = onGoogleSignInClick,
                            onToggleMode = { authMode = AuthMode.LOGIN },
                            onSuccess = onAuthSuccess
                        )
                    }
                    AuthMode.COMPLETE_PROFILE -> {
                        CompleteProfileContent(
                            viewModel = viewModel,
                            profileData = googleProfileData,
                            onCancel = { authMode = AuthMode.LOGIN },
                            onSuccess = onAuthSuccess
                        )
                    }
                }
            }
        }
    }
}

// LOGIN
@Composable
fun LoginContent(
    viewModel: AuthViewModel,
    onGoogleSignIn: () -> Unit,
    onToggleMode: () -> Unit,
    onSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = stringResource(R.string.welcome_back),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        AasraTextField(value = email, onValueChange = { email = it }, label = "Email Address", icon = Icons.Filled.Email)
        Spacer(modifier = Modifier.height(16.dp))
        AasraTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            icon = Icons.Filled.Lock,
            isPassword = true,
            isVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AasraButton(text = "Log In", isLoading = isLoading) {
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@AasraButton
            }

            isLoading = true
            viewModel.login(
                email = email,
                pass = password,
                onSuccess = {
                    isLoading = false
                    onSuccess() // Navigate to Home
                },
                onError = { error ->
                    isLoading = false
                    Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        GoogleButton(onClick = onGoogleSignIn)
        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text(stringResource(R.string.don_t_have_an_account), color = MaterialTheme.colorScheme.onSurface)
            Text(text = stringResource(R.string.sign_up),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onToggleMode() }
            )
        }
    }
}

// SIGN UP
@Composable
fun SignUpContent(
    viewModel: AuthViewModel,
    onGoogleSignIn: () -> Unit,
    onToggleMode: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var cnic by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("victim") } // Default Role
    var skills by remember { mutableStateOf("") }
    
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBase64 by remember { mutableStateOf<String?>(null) }

    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            photoUri = uri
            if (uri != null) {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    photoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            }
        }
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = stringResource(R.string.create_account),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // PROFILE PICTURE PICKER
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { photoPickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                ) },
            contentAlignment = Alignment.Center
        ) {
            if (photoBase64 != null) {
                val imageBytes = Base64.decode(photoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Add Photo",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
        ) {
            RoleOption(
                text = "Victim",
                selected = selectedRole == "victim",
                modifier = Modifier.weight(1f)
            ) { selectedRole = "victim" }

            RoleOption(
                text = "Volunteer",
                selected = selectedRole == "volunteer",
                modifier = Modifier.weight(1f)
            ) { selectedRole = "volunteer" }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AasraTextField(value = name, onValueChange = { name = it }, label = "Full Name", icon = Icons.Filled.Person)
        Spacer(modifier = Modifier.height(12.dp))

        AasraTextField(value = email, onValueChange = { email = it }, label = "Email Address", icon = Icons.Filled.Email)
        Spacer(modifier = Modifier.height(12.dp))

        AasraTextField(
            value = cnic, 
            onValueChange = { if (it.length <= 13 && it.all { char -> char.isDigit() }) cnic = it }, 
            label = "CNIC Number", 
            icon = Icons.Filled.Person,
            keyboardType = KeyboardType.Number,
            visualTransformation = CnicVisualTransformation()
        )
        if (cnic.isNotEmpty() && cnic.length < 13) {
            Text("CNIC must be 13 digits", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(start = 16.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))

        AasraTextField(
            value = age, 
            onValueChange = { if (it.all { char -> char.isDigit() }) age = it }, 
            label = "Age", 
            icon = Icons.Filled.Person,
            keyboardType = KeyboardType.Number
        )
        Spacer(modifier = Modifier.height(12.dp))

        AasraTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone Number",
            icon = Icons.Filled.Phone,
            keyboardType = KeyboardType.Phone
        )
        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(visible = selectedRole == "volunteer") {
            Column {
                AasraTextField(
                    value = skills,
                    onValueChange = { skills = it },
                    label = "Skills (e.g. First Aid)",
                    icon = Icons.Filled.MedicalServices
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        AasraTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            icon = Icons.Filled.Lock,
            isPassword = true,
            isVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AasraButton(text = "Sign Up", isLoading = isLoading) {
            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@AasraButton
            }

            isLoading = true
            viewModel.signUp(
                email = email,
                pass = password,
                name = name,
                phone = phone,
                role = selectedRole,
                skills = if (selectedRole == "volunteer") skills else "",
                cnic = cnic,
                age = age,
                photoBase64 = photoBase64,
                onSuccess = {
                    isLoading = false
                    onSuccess()
                },
                onError = { error ->
                    isLoading = false
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        GoogleButton(onClick = onGoogleSignIn)
        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text(stringResource(R.string.already_have_an_account), color = MaterialTheme.colorScheme.onSurface)
            Text(text = stringResource(R.string.log_in),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onToggleMode() }
            )
        }
    }
}

@Composable
fun CompleteProfileContent(
    viewModel: AuthViewModel,
    profileData: GoogleProfileData?,
    onCancel: () -> Unit,
    onSuccess: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var cnic by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("victim") }
    var skills by remember { mutableStateOf("") }
    
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            photoUri = uri
            if (uri != null) {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    photoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            }
        }
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Complete Profile",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(text = "Please complete your details to finish signing up.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // PROFILE PICTURE PICKER
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { photoPickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                ) },
            contentAlignment = Alignment.Center
        ) {
            if (photoBase64 != null) {
                val imageBytes = Base64.decode(photoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (profileData?.photoUrl != null && profileData.photoUrl.isNotBlank()) {
                // Show a placeholder or use Coil to load if available, here we just show Icon for simplicity
                // In a real app we'd use AsyncImage, but currently this app loads Base64.
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Add Photo",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Add Photo",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
        ) {
            RoleOption(
                text = "Victim",
                selected = selectedRole == "victim",
                modifier = Modifier.weight(1f)
            ) { selectedRole = "victim" }

            RoleOption(
                text = "Volunteer",
                selected = selectedRole == "volunteer",
                modifier = Modifier.weight(1f)
            ) { selectedRole = "volunteer" }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Read-only fields
        OutlinedTextField(
            value = profileData?.name ?: "",
            onValueChange = { },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = profileData?.email ?: "",
            onValueChange = { },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        AasraTextField(
            value = cnic, 
            onValueChange = { if (it.length <= 13 && it.all { char -> char.isDigit() }) cnic = it }, 
            label = "CNIC Number", 
            icon = Icons.Filled.Person,
            keyboardType = KeyboardType.Number,
            visualTransformation = CnicVisualTransformation()
        )
        if (cnic.isNotEmpty() && cnic.length < 13) {
            Text("CNIC must be 13 digits", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(start = 16.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))

        AasraTextField(
            value = age, 
            onValueChange = { if (it.all { char -> char.isDigit() }) age = it }, 
            label = "Age", 
            icon = Icons.Filled.Person,
            keyboardType = KeyboardType.Number
        )
        Spacer(modifier = Modifier.height(12.dp))

        AasraTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone Number",
            icon = Icons.Filled.Phone,
            keyboardType = KeyboardType.Phone
        )
        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(visible = selectedRole == "volunteer") {
            Column {
                AasraTextField(
                    value = skills,
                    onValueChange = { skills = it },
                    label = "Skills (e.g. First Aid)",
                    icon = Icons.Filled.MedicalServices
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AasraButton(text = "Complete Profile", isLoading = isLoading) {
            if (cnic.length != 13 || age.isBlank() || phone.isBlank()) {
                Toast.makeText(context, "Please fill required details correctly", Toast.LENGTH_SHORT).show()
                return@AasraButton
            }

            isLoading = true
            viewModel.completeGoogleProfile(
                name = profileData?.name ?: "",
                email = profileData?.email ?: "",
                phone = phone,
                role = selectedRole,
                skills = if (selectedRole == "volunteer") skills else "",
                cnic = cnic,
                age = age,
                photoBase64 = photoBase64,
                photoUrlFromGoogle = profileData?.photoUrl,
                onSuccess = {
                    isLoading = false
                    onSuccess()
                },
                onError = { error ->
                    isLoading = false
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onCancel) {
            Text("Cancel", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun RoleOption(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AasraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    isPassword: Boolean = false,
    isVisible: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation? = null,
    onToggleVisibility: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = if (icon != null) { { Icon(icon, contentDescription = null) } } else null,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = visualTransformation ?: if (isPassword && !isVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle Password"
                    )
                }
            }
        }
    )
}

@Composable
fun AasraButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun GoogleButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_google),
            contentDescription = "Google Sign In",
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.continue_with_google), color = MaterialTheme.colorScheme.onBackground)
    }
}