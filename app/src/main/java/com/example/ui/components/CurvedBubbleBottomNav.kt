package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenTab
import com.example.ui.theme.DokanGreen
import com.example.ui.theme.DokanGreenDark
import com.example.ui.theme.DueAmber
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightBorder

@Composable
fun CurvedBubbleBottomNav(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    hasDueAlert: Boolean = false,
    hasLowStockAlert: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Floating Premium Capsule Bar with soft ambient shadow
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 14.dp,
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .border(
                    width = 1.2.dp,
                    color = LightBorder.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScreenTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    val hasBadge = when (tab) {
                        ScreenTab.CUSTOMERS -> hasDueAlert
                        ScreenTab.INVENTORY -> hasLowStockAlert
                        else -> false
                    }
                    val badgeColor = when (tab) {
                        ScreenTab.CUSTOMERS -> DueAmber
                        ScreenTab.INVENTORY -> ErrorRed
                        else -> DokanGreen
                    }

                    FloatingNavItem(
                        tab = tab,
                        isSelected = isSelected,
                        hasBadge = hasBadge,
                        badgeColor = badgeColor,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(if (isSelected) 1.25f else 0.95f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    tab: ScreenTab,
    isSelected: Boolean,
    hasBadge: Boolean,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ItemScale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        label = "ContentColor"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 24.dp)
            ) { onClick() }
            .testTag(tab.tag)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            // Animated Active Pill with Vibrant Dokan Green Gradient
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(DokanGreen, DokanGreenDark)
                        )
                    )
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = tab.selectedIcon,
                        contentDescription = tab.title,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    if (hasBadge) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .offset(x = 2.dp, y = (-2).dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(1.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = tab.banglaTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp
                    ),
                    color = contentColor,
                    maxLines = 1
                )
            }
        } else {
            // Unselected Clean Icon + Label Column with optional notification badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = tab.unselectedIcon,
                        contentDescription = tab.title,
                        tint = contentColor,
                        modifier = Modifier.size(21.dp)
                    )
                    if (hasBadge) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .offset(x = 2.dp, y = (-2).dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = tab.banglaTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    ),
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}
