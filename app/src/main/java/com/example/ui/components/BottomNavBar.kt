package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrandPrimary
import com.example.util.LocalAppStrings

@Composable
fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onTabSelected(0) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_home")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = strings.navHome,
                    tint = if (selectedTab == 0) BrandPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = { onTabSelected(1) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_waves")
            ) {
                Icon(
                    imageVector = Icons.Rounded.VideoLibrary,
                    contentDescription = strings.navWaves,
                    tint = if (selectedTab == 1) BrandPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.size(26.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape, spotColor = BrandPrimary)
                    .clip(CircleShape)
                    .background(BrandPrimary)
                    .clickable { onTabSelected(2) }
                    .testTag("nav_create_post"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = strings.navCreatePost,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = { onTabSelected(3) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_search")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = strings.navSearch,
                    tint = if (selectedTab == 3) BrandPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = { onTabSelected(4) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("nav_profile")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = strings.navProfile,
                    tint = if (selectedTab == 4) BrandPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
