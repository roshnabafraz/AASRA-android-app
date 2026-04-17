package com.roshnab.aasra.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop // Using a drop icon as a logo placeholder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roshnab.aasra.R
import androidx.compose.ui.res.stringResource

@Composable
fun AasraTopBar(
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    photoUrl: String? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- 1. LEFT SIDE: BRANDING (Logo + App Name) ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Small Logo Icon
//                Icon(
//                    imageVector = Icons.Filled.WaterDrop, // Placeholder for AASRA Logo
//                    contentDescription = "Logo",
//                    tint = MaterialTheme.colorScheme.primary,
//                    modifier = Modifier.size(28.dp)
//                )
//
//                Spacer(modifier = Modifier.width(8.dp))

                // APP NAME
                Text(text = stringResource(R.string.aasra),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp // Makes it look cleaner
                )
            }

            // --- 2. RIGHT SIDE: ACTIONS (Notification + Profile) ---
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Notification Icon removed: Replaced by Bottom Navigation item.

                // Profile Picture (Clickable)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.dp, Color.Gray.copy(alpha = 0.2f), CircleShape)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(photoUrl) {
                        if (!photoUrl.isNullOrEmpty() && photoUrl.startsWith("data:image")) {
                            try {
                                val base64Data = photoUrl.substringAfter(",")
                                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) { null }
                        } else null
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}