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
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.roshnab.aasra.R
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onAuthSuccess: () -> Unit
) {

    val context = LocalContext.current
    var isLoginMode by remember { mutableStateOf(true) }

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

            AnimatedContent(targetState = isLoginMode, label = "AuthToggle") { isLogin ->
                if (isLogin) {
                    LoginContent(
                        viewModel = viewModel,
                        onGoogleSignIn = onGoogleSignInClick,
                        onToggleMode = { isLoginMode = false },
                        onSuccess = onAuthSuccess
                    )
                } else {
                    SignUpContent(
                        viewModel = viewModel,
                        onGoogleSignIn = onGoogleSignInClick,
                        onToggleMode = { isLoginMode = true },
                        onSuccess = onAuthSuccess
                    )
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
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("victim") } // Default Role
    var skills by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = stringResource(R.string.create_account),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
        ) {
            RoleOption(
                text = "I Need Help",
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
        visualTransformation = if (isPassword && !isVisible) PasswordVisualTransformation() else VisualTransformation.None,
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