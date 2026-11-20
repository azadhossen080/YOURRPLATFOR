package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.BrandPrimary
import com.example.util.LocalAppStrings

@Composable
fun QrCodeDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    val strings = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.qrCodeMenu,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // QR Display Box
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(140.dp)) {
                        val gridSize = 7
                        val cellSize = size.width / gridSize
                        val brandColor = BrandPrimary

                        fun drawFinderPattern(x: Float, y: Float) {
                            drawRect(
                                color = brandColor,
                                topLeft = Offset(x, y),
                                size = Size(cellSize * 2.5f, cellSize * 2.5f)
                            )
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(x + cellSize * 0.5f, y + cellSize * 0.5f),
                                size = Size(cellSize * 1.5f, cellSize * 1.5f)
                            )
                            drawRect(
                                color = brandColor,
                                topLeft = Offset(x + cellSize * 0.8f, y + cellSize * 0.8f),
                                size = Size(cellSize * 0.9f, cellSize * 0.9f)
                            )
                        }

                        drawFinderPattern(0f, 0f)
                        drawFinderPattern(size.width - cellSize * 2.5f, 0f)
                        drawFinderPattern(0f, size.height - cellSize * 2.5f)

                        val dots = listOf(
                            Pair(3, 0), Pair(4, 1), Pair(3, 2), Pair(4, 3), Pair(3, 4),
                            Pair(0, 3), Pair(1, 4), Pair(2, 4), Pair(5, 4), Pair(6, 4),
                            Pair(3, 5), Pair(4, 5), Pair(5, 5), Pair(5, 6), Pair(6, 6)
                        )
                        for ((col, row) in dots) {
                            drawCircle(
                                color = brandColor,
                                radius = cellSize * 0.35f,
                                center = Offset(col * cellSize + cellSize / 2, row * cellSize + cellSize / 2)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = userProfile.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${userProfile.handle.ifBlank { "@${userProfile.displayUserId}" }} • ${strings.identity.userIdPrefix} ${userProfile.displayUserId}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.qrScanSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                modifier = Modifier.testTag("share_qr_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = strings.shareLabel,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.shareBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}
