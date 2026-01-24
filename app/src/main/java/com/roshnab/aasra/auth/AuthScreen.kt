package com.roshnab.aasra.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.roshnab.aasra.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    var isLoginMode by remember { mutableStateOf(true) }

    // 1. Initialize Credential Manager & Scope
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // 2. Define the Google Sign-In Logic (Reusable)
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

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                when (val credential = result.credential) {
                    is CustomCredential -> {
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val idToken = googleIdTokenCredential.idToken
                            viewModel.signInWithGoogle(idToken)
                        }
                    }
                    else -> {
                        Log.e("Auth", "Unknown credential type")
                    }
                }
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign In Failed", e)
                Toast.makeText(context, "Google Sign In Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // State Listener
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Welcome to AASRA", Toast.LENGTH_SHORT).show()
                onAuthSuccess()
            }
            is AuthState.Error -> {
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Main Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo / Title
            Image(
                painter = painterResource(id = R.drawable.aasra_logo),
                contentDescription = "AASRA Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Text(
                text = "AASRA",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )

//            Text(
//                text = "AI powered Aid System For Rapid Assistance",
//                style = MaterialTheme.typography.labelMedium,
//                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
//                modifier = Modifier.padding(bottom = 32.dp)
//            )

            // Smooth Toggle between Login and Sign Up Screens
            AnimatedContent(targetState = isLoginMode, label = "AuthToggle") { isLogin ->
                if (isLogin) {
                    LoginContent(
                        viewModel = viewModel,
                        isLoading = authState is AuthState.Loading,
                        onGoogleSignIn = onGoogleSignInClick,
                        onToggleMode = { isLoginMode = false }
                    )
                } else {
                    SignUpContent(
                        viewModel = viewModel,
                        isLoading = authState is AuthState.Loading,
                        onGoogleSignIn = onGoogleSignInClick,
                        onToggleMode = { isLoginMode = true }
                    )
                }
            }
        }
    }
}

// --- LOGIN CONTENT ---
@Composable
fun LoginContent(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    onGoogleSignIn: () -> Unit,
    onToggleMode: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        AasraTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address"
        )

        Spacer(modifier = Modifier.height(16.dp))

        AasraTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true,
            isVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AasraButton(
            text = "Log In",
            isLoading = isLoading,
            onClick = { viewModel.login(email, password) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        GoogleButton(onClick = onGoogleSignIn)

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text("Don't have an account? ", color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "Sign Up",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onToggleMode() }
            )
        }
    }
}

// --- SIGN UP CONTENT ---
@Composable
fun SignUpContent(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    onGoogleSignIn: () -> Unit,
    onToggleMode: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        AasraTextField(value = email, onValueChange = { email = it }, label = "Email Address")

        Spacer(modifier = Modifier.height(16.dp))

        AasraTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true,
            isVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AasraTextField(
            value = confirmPass,
            onValueChange = { confirmPass = it },
            label = "Confirm Password",
            isPassword = true,
            isVisible = passwordVisible, // Sync visibility
            onToggleVisibility = { passwordVisible = !passwordVisible }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AasraButton(
            text = "Sign Up",
            isLoading = isLoading,
            onClick = {
                if (password == confirmPass) {
                    viewModel.signUp(email, password)
                } else {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        GoogleButton(onClick = onGoogleSignIn)

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "Log In",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onToggleMode() }
            )
        }
    }
}

// --- CUSTOM COMPONENTS (Reusable) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AasraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    isVisible: Boolean = true,
    onToggleVisibility: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword && !isVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
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
fun AasraButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
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
            modifier = Modifier.size(24.dp) // Explicitly set the size to 24dp
        )

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Continue with Google",
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}