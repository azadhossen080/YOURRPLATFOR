package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.UserProfile
import com.example.ui.theme.BrandPrimary
import com.example.util.LocalAppStrings
import kotlin.random.Random

@Composable
fun CreateIdentityDialog(
    currentProfile: UserProfile,
    isEditingExisting: Boolean,
    onDismiss: () -> Unit,
    onSaveIdentity: (userName: String, userId: String, email: String, phoneNumber: String) -> Unit,
    onUpdateVerificationAndVip: (isVerified: Boolean, isVip: Boolean) -> Unit = { _, _ -> }
) {
    val strings = LocalAppStrings.current
    var userName by remember(currentProfile.name, isEditingExisting) {
        mutableStateOf(if (isEditingExisting || currentProfile.isProfileCreated) currentProfile.name else "")
    }
    var userId by remember(currentProfile.userId, isEditingExisting) {
        mutableStateOf(
            if (isEditingExisting || currentProfile.isProfileCreated) currentProfile.userId else ""
        )
    }
    var email by remember(currentProfile.email, isEditingExisting) {
        mutableStateOf(currentProfile.email)
    }
    var phoneNumber by remember(currentProfile.phoneNumber, isEditingExisting) {
        mutableStateOf(currentProfile.phoneNumber)
    }
    var isVerifiedBadge by remember(currentProfile.isVerified) { mutableStateOf(currentProfile.isVerified) }
    var isVipBadge by remember(currentProfile.isVip) { mutableStateOf(currentProfile.isVip) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun generateSuggestedId(): String {
        val base = userName.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "user" }
        val suffix = Random.nextInt(1000, 9999)
        return "${base}_$suffix"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "YOURPLATFORM Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .align(Alignment.Center),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.TopEnd)
                            .testTag("identity_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isEditingExisting) strings.identity.editIdentityTitle else strings.identity.createIdentityTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.identity.createIdentitySubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandPrimary.copy(alpha = 0.1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.identity.initialFollowersBadge,
                            color = BrandPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = userName,
                    onValueChange = {
                        userName = it
                        validationError = null
                    },
                    label = { Text(strings.identity.userNameLabel) },
                    placeholder = { Text(strings.identity.userNamePlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("identity_name_input")
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
                        label = { Text(strings.identity.userIdLabel) },
                        placeholder = { Text(strings.identity.userIdPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = BrandPrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("identity_userid_input")
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
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("identity_email_input")
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text(strings.auth.phoneNumberLabel) },
                    placeholder = { Text(strings.auth.phonePlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("identity_phone_input")
                )

                // Verified User ID Logo (Blue Mark) & VIP Badge Toggles
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                VerifiedAndVipBadges(isVerified = true, isVip = false, compact = true)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.mediaStudio.verifiedIdToggleLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Switch(
                                checked = isVerifiedBadge,
                                onCheckedChange = { isVerifiedBadge = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = VerifiedBlueColor, checkedTrackColor = VerifiedBlueColor.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("toggle_verified_blue_mark")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                VerifiedAndVipBadges(isVerified = false, isVip = true, compact = true)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.mediaStudio.vipMemberToggleLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Switch(
                                checked = isVipBadge,
                                onCheckedChange = { isVipBadge = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = VipGoldStart, checkedTrackColor = VipGoldStart.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("toggle_vip_badge")
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = userName.trim()
                    val cleanId = userId.trim().removePrefix("@")
                    if (cleanName.isBlank() || cleanId.isBlank()) {
                        validationError = strings.identity.identityRequiredError
                    } else {
                        onUpdateVerificationAndVip(isVerifiedBadge, isVipBadge)
                        onSaveIdentity(cleanName, cleanId, email.trim(), phoneNumber.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("identity_save_button")
            ) {
                Text(strings.identity.createAccountBtn, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("identity_cancel_button")
            ) {
                Text(strings.cancel)
            }
        }
    )
}
