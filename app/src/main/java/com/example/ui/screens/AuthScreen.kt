package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.AccountAuthResult
import com.example.data.LocalAccount
import com.example.data.LocalAccountStore
import com.example.model.UserProfile
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import com.example.util.GoogleAuthHelper
import com.example.util.LocalAppStrings
import kotlin.random.Random

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onLocalAuthSuccess: (LocalAccount) -> Unit = {},
    onCancelAndUsePlatform: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val accountStore = remember(context) { LocalAccountStore(context) }

    var selectedMode by remember { mutableIntStateOf(0) } // 0 = Log In, 1 = Create Account
    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSilent by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Log In fields
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Create Account fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var signupContact by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var showSignupPassword by remember { mutableStateOf(false) }
    var generatedVerificationCode by remember { mutableStateOf<String?>(null) }
    var verificationCodeInput by remember { mutableStateOf("") }
    var captchaChallenge by remember { mutableStateOf(LocalAccountStore.generateCaptchaCode()) }
    var captchaSeed by remember { mutableIntStateOf(Random.nextInt(1, 100_000)) }
    var captchaInput by remember { mutableStateOf("") }

    fun regenerateCaptcha() {
        captchaChallenge = LocalAccountStore.generateCaptchaCode()
        captchaSeed = Random.nextInt(1, 100_000)
        captchaInput = ""
    }

    BackHandler {
        onCancelAndUsePlatform()
    }

    LaunchedEffect(Unit) {
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSilent = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSilent = false
            },
            scope = scope
        )
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .testTag("auth_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = onCancelAndUsePlatform,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(48.dp)
                    .testTag("auth_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.close,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(26.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(BrandPrimary.copy(alpha = 0.16f), BrandAccent.copy(alpha = 0.16f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "YOURPLATFORM Logo",
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Text(
                        text = strings.auth.welcomeTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = strings.auth.authSubtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = selectedMode == 0,
                            onClick = {
                                selectedMode = 0
                                errorMessage = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.testTag("auth_tab_login")
                        ) {
                            Text(strings.auth.loginTab, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                        SegmentedButton(
                            selected = selectedMode == 1,
                            onClick = {
                                selectedMode = 1
                                errorMessage = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.testTag("auth_tab_signup")
                        ) {
                            Text(strings.auth.signUpTab, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (selectedMode == 0) {
                                // LOG IN TAB: Email or Phone number + Password + Log In + Google
                                OutlinedTextField(
                                    value = loginIdentifier,
                                    onValueChange = {
                                        loginIdentifier = it
                                        errorMessage = null
                                    },
                                    label = { Text(strings.auth.emailOrPhoneLabel) },
                                    placeholder = { Text(strings.auth.emailOrPhonePlaceholder) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = BrandPrimary)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_identifier_input")
                                )

                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = {
                                        loginPassword = it
                                        errorMessage = null
                                    },
                                    label = { Text(strings.auth.passwordLabel) },
                                    placeholder = { Text(strings.auth.passwordPlaceholder) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = BrandPrimary)
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { showLoginPassword = !showLoginPassword },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("login_password_toggle")
                                        ) {
                                            Icon(
                                                imageVector = if (showLoginPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showLoginPassword) strings.auth.hidePasswordDesc else strings.auth.showPasswordDesc
                                            )
                                        }
                                    },
                                    visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_password_input")
                                )

                                if (errorMessage != null) {
                                    AuthErrorBanner(message = errorMessage!!)
                                }

                                Button(
                                    onClick = {
                                        errorMessage = null
                                        if (loginIdentifier.isBlank() || loginPassword.isEmpty()) {
                                            errorMessage = strings.auth.emptyLoginError
                                            return@Button
                                        }
                                        when (val result = accountStore.authenticate(loginIdentifier, loginPassword)) {
                                            is AccountAuthResult.Success -> onLocalAuthSuccess(result.account)
                                            else -> errorMessage = strings.auth.invalidCredentialsError
                                        }
                                    },
                                    enabled = !isLoading && !isCheckingSilent,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .testTag("login_submit_button")
                                ) {
                                    Text(
                                        text = strings.auth.loginBtn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                AuthOrDivider(text = strings.auth.orDividerText)

                                OutlinedButton(
                                    onClick = {
                                        errorMessage = null
                                        isLoading = true
                                        GoogleAuthHelper.onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onAuthSuccess = {
                                                isLoading = false
                                                onAuthSuccess()
                                            },
                                            onAuthError = { msg ->
                                                isLoading = false
                                                errorMessage = msg
                                            },
                                            scope = scope,
                                            onAuthCancelled = {
                                                isLoading = false
                                            }
                                        )
                                    },
                                    enabled = !isLoading && !isCheckingSilent,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .testTag("google_signin_button")
                                ) {
                                    if (isLoading || isCheckingSilent) {
                                        CircularProgressIndicator(
                                            color = BrandPrimary,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = strings.auth.signingInLabel,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = BrandPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = strings.auth.continueWithGoogle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            } else {
                                // CREATE ACCOUNT TAB: First name, Last name, Email or Phone, Password, Verification code, Captcha & Create
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = firstName,
                                        onValueChange = {
                                            firstName = it
                                            errorMessage = null
                                        },
                                        label = { Text(strings.auth.firstNameLabel) },
                                        placeholder = { Text(strings.auth.firstNamePlaceholder) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("signup_first_name_input")
                                    )

                                    OutlinedTextField(
                                        value = lastName,
                                        onValueChange = {
                                            lastName = it
                                            errorMessage = null
                                        },
                                        label = { Text(strings.auth.lastNameLabel) },
                                        placeholder = { Text(strings.auth.lastNamePlaceholder) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("signup_last_name_input")
                                    )
                                }

                                OutlinedTextField(
                                    value = signupContact,
                                    onValueChange = {
                                        signupContact = it
                                        errorMessage = null
                                    },
                                    label = { Text(strings.auth.emailOrPhoneLabel) },
                                    placeholder = { Text(strings.auth.emailOrPhonePlaceholder) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = BrandPrimary)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_contact_input")
                                )

                                OutlinedTextField(
                                    value = signupPassword,
                                    onValueChange = {
                                        signupPassword = it
                                        errorMessage = null
                                    },
                                    label = { Text(strings.auth.passwordLabel) },
                                    placeholder = { Text(strings.auth.passwordPlaceholder) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = BrandPrimary)
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { showSignupPassword = !showSignupPassword },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("signup_password_toggle")
                                        ) {
                                            Icon(
                                                imageVector = if (showSignupPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showSignupPassword) strings.auth.hidePasswordDesc else strings.auth.showPasswordDesc
                                            )
                                        }
                                    },
                                    visualTransformation = if (showSignupPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_password_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = verificationCodeInput,
                                        onValueChange = {
                                            verificationCodeInput = it.filter { ch -> ch.isDigit() }.take(6)
                                            errorMessage = null
                                        },
                                        label = { Text(strings.auth.verificationCodeLabel) },
                                        placeholder = { Text(strings.auth.verificationCodePlaceholder) },
                                        leadingIcon = {
                                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandPrimary)
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("signup_verification_code_input")
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            errorMessage = null
                                            if (!LocalAccountStore.isValidContact(signupContact)) {
                                                errorMessage = strings.auth.invalidContactError
                                            } else {
                                                generatedVerificationCode = LocalAccountStore.generateVerificationCode()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .heightIn(min = 52.dp)
                                            .testTag("signup_send_code_button")
                                    ) {
                                        Text(
                                            text = if (generatedVerificationCode == null) {
                                                strings.auth.sendCodeBtn
                                            } else {
                                                strings.auth.resendCodeBtn
                                            },
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                if (generatedVerificationCode != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = BrandPrimary.copy(alpha = 0.1f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_code_banner")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = BrandPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = strings.auth.verificationCodeNotice(generatedVerificationCode!!),
                                                color = BrandPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Visual CAPTCHA Challenge + Input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CaptchaChallengeBox(
                                        code = captchaChallenge,
                                        seed = captchaSeed,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .testTag("signup_captcha_canvas")
                                    )

                                    IconButton(
                                        onClick = {
                                            regenerateCaptcha()
                                            errorMessage = null
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(BrandPrimary.copy(alpha = 0.1f))
                                            .testTag("signup_refresh_captcha_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = strings.auth.refreshCaptchaDesc,
                                            tint = BrandPrimary
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = captchaInput,
                                    onValueChange = {
                                        captchaInput = it.uppercase()
                                        errorMessage = null
                                    },
                                    label = { Text(strings.auth.captchaLabel) },
                                    placeholder = { Text(strings.auth.captchaPlaceholder) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = BrandPrimary)
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_captcha_input")
                                )

                                if (errorMessage != null) {
                                    AuthErrorBanner(message = errorMessage!!)
                                }

                                // Final "Create" Option
                                Button(
                                    onClick = {
                                        errorMessage = null
                                        when {
                                            firstName.isBlank() || lastName.isBlank() || signupContact.isBlank() || signupPassword.isEmpty() -> {
                                                errorMessage = strings.auth.emptySignupFieldsError
                                            }
                                            !LocalAccountStore.isValidContact(signupContact) -> {
                                                errorMessage = strings.auth.invalidContactError
                                            }
                                            signupPassword.length < 6 -> {
                                                errorMessage = strings.auth.passwordTooShortError
                                            }
                                            generatedVerificationCode == null -> {
                                                errorMessage = strings.auth.verificationCodeRequiredError
                                            }
                                            verificationCodeInput.trim() != generatedVerificationCode -> {
                                                errorMessage = strings.auth.invalidVerificationCodeError
                                            }
                                            !captchaInput.trim().equals(captchaChallenge, ignoreCase = true) -> {
                                                errorMessage = strings.auth.invalidCaptchaError
                                                regenerateCaptcha()
                                            }
                                            else -> {
                                                when (val res = accountStore.registerAccount(firstName, lastName, signupContact, signupPassword)) {
                                                    is AccountAuthResult.Success -> onLocalAuthSuccess(res.account)
                                                    AccountAuthResult.DuplicateAccount -> {
                                                        errorMessage = strings.auth.accountAlreadyExistsError
                                                        regenerateCaptcha()
                                                    }
                                                    AccountAuthResult.WeakPassword -> {
                                                        errorMessage = strings.auth.passwordTooShortError
                                                    }
                                                    else -> {
                                                        errorMessage = strings.auth.invalidContactError
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading && !isCheckingSilent,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .testTag("signup_create_button")
                                ) {
                                    Text(
                                        text = strings.auth.createAccountSubmitBtn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                AuthOrDivider(text = strings.auth.orDividerText)

                                OutlinedButton(
                                    onClick = {
                                        errorMessage = null
                                        isLoading = true
                                        GoogleAuthHelper.onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onAuthSuccess = {
                                                isLoading = false
                                                onAuthSuccess()
                                            },
                                            onAuthError = { msg ->
                                                isLoading = false
                                                errorMessage = msg
                                            },
                                            scope = scope,
                                            onAuthCancelled = {
                                                isLoading = false
                                            }
                                        )
                                    },
                                    enabled = !isLoading && !isCheckingSilent,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("google_signin_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.auth.continueWithGoogle,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthErrorBanner(message: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_error_banner")
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 12.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun AuthOrDivider(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
        )
    }
}

@Composable
private fun CaptchaChallengeBox(
    code: String,
    seed: Int,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val borderColor = BrandPrimary.copy(alpha = 0.35f)
    val inkColor = MaterialTheme.colorScheme.onSurface
    val accentColor = BrandAccent

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rnd = Random(seed)
            // Noise lines
            repeat(5) {
                drawLine(
                    color = accentColor.copy(alpha = 0.28f),
                    start = Offset(rnd.nextFloat() * size.width * 0.25f, rnd.nextFloat() * size.height),
                    end = Offset(size.width * (0.75f + rnd.nextFloat() * 0.25f), rnd.nextFloat() * size.height),
                    strokeWidth = 2.2f
                )
            }
            // Noise dots
            repeat(28) {
                drawCircle(
                    color = borderColor.copy(alpha = 0.45f),
                    radius = 2f + rnd.nextFloat() * 2f,
                    center = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
                )
            }
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = size.height * 0.56f
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.MONOSPACE,
                    android.graphics.Typeface.BOLD
                )
                textAlign = android.graphics.Paint.Align.CENTER
                color = inkColor.toArgb()
            }
            val step = size.width / (code.length + 1).coerceAtLeast(2)
            drawIntoCanvas { canvas ->
                code.forEachIndexed { idx, ch ->
                    val cx = step * (idx + 1)
                    val cy = size.height * 0.66f + (rnd.nextFloat() - 0.5f) * (size.height * 0.14f)
                    val angle = (rnd.nextFloat() - 0.5f) * 26f
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.rotate(angle, cx, cy)
                    canvas.nativeCanvas.drawText(ch.toString(), cx, cy, paint)
                    canvas.nativeCanvas.restore()
                }
            }
        }
    }
}


@Composable
fun CompleteRegistrationScreen(
    currentProfile: UserProfile,
    googleEmail: String,
    googleName: String,
    onSaveAccount: (name: String, userId: String, email: String, phone: String) -> Unit,
    onLogOut: () -> Unit,
    onCancelAndUsePlatform: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    BackHandler {
        onCancelAndUsePlatform()
    }
    var name by remember(currentProfile.name, googleName) {
        mutableStateOf(
            when {
                currentProfile.name.isNotBlank() && currentProfile.name != "Guest User" -> currentProfile.name
                googleName.isNotBlank() -> googleName
                else -> ""
            }
        )
    }
    var userId by remember(currentProfile.userId) {
        mutableStateOf(currentProfile.userId)
    }
    var email by remember(currentProfile.email, googleEmail) {
        mutableStateOf(currentProfile.email.ifBlank { googleEmail })
    }
    var phone by remember(currentProfile.phoneNumber) {
        mutableStateOf(currentProfile.phoneNumber)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun generateSuggestedId(): String {
        val base = name.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "user" }
        return "${base}_${Random.nextInt(1000, 9999)}"
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .testTag("complete_registration_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = BrandPrimary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = strings.identity.initialFollowersBadge,
                            color = BrandPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onLogOut,
                            modifier = Modifier.testTag("registration_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = strings.auth.logOutBtn,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.auth.logOutBtn,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = onCancelAndUsePlatform,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("registration_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.close,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Text(
                    text = strings.auth.completeProfileTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = strings.auth.completeProfileSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text(strings.auth.nameLabel) },
                    placeholder = { Text(strings.identity.userNamePlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { raw ->
                            userId = raw.replace(" ", "_").removePrefix("@")
                            validationError = null
                        },
                        label = { Text(strings.auth.userIdLabel) },
                        placeholder = { Text(strings.identity.userIdPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = BrandPrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reg_userid_input")
                    )

                    OutlinedButton(
                        onClick = {
                            userId = generateSuggestedId()
                            validationError = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.identity.autoGenerateIdBtn, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(strings.auth.emailLabel) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_email_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.auth.phoneNumberLabel) },
                    placeholder = { Text(strings.auth.phonePlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BrandPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_phone_input")
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandPrimary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.auth.verifiedViaGoogleBadge,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandPrimary
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = {
                        val cleanName = name.trim()
                        val cleanId = userId.trim().removePrefix("@")
                        if (cleanName.isBlank() || cleanId.isBlank()) {
                            validationError = strings.auth.registrationErrorEmpty
                        } else {
                            onSaveAccount(cleanName, cleanId, email.trim(), phone.trim())
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("reg_save_button")
                ) {
                    Text(
                        text = strings.auth.saveAndContinueBtn,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
