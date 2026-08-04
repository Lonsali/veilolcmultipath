package com.v2ray.ang.ui.compose

import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.v2ray.ang.R

private const val BUTTON_SIZE_SCALE = 0.90f

@Composable
fun ExpressiveToolbarActions(
    onFilterClick: () -> Unit,
    onShowImportMenu: (View) -> Unit,
    onShowOverflowMenu: (View) -> Unit,
    showSearchButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val addIcon = Icons.Filled.Add
    val filterIcon = Icons.Default.Search
    val moreIcon = Icons.Default.MoreVert
    val addLabel = stringResource(R.string.menu_item_add_config)
    val searchLabel = stringResource(R.string.menu_item_search)

    ButtonGroup(
        overflowIndicator = {},
        modifier = modifier
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        toolbarIconItem(
            icon = addIcon,
            contentDescription = addLabel,
            onClick = { onShowImportMenu(view) },
            isFilled = true,
            widthMultiplier = 2f,
        )

        if (showSearchButton) {
            toolbarIconItem(
                icon = filterIcon,
                contentDescription = searchLabel,
                onClick = onFilterClick,
                isFilled = false,
            )
        }

        toolbarIconItem(
            icon = moreIcon,
            contentDescription = "More options",
            onClick = { onShowOverflowMenu(view) },
            isFilled = false,
        )
    }
}

private fun ButtonGroupScope.toolbarIconItem(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isFilled: Boolean,
    widthMultiplier: Float = 1f,
) {
    customItem(
        buttonGroupContent = {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val pressProgress = remember { Animatable(0f) }
            val hapticFeedback = LocalHapticFeedback.current
            val pressSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
            val bounceSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

            LaunchedEffect(isPressed) {
                if (isPressed) {
                    pressProgress.animateTo(1f, pressSpec)
                } else {
                    pressProgress.animateTo(0f, bounceSpec)
                }
            }

            val shape = RoundedCornerShape(percent = 50)
            val containerColor =
                if (isFilled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondaryContainer
            val contentColor =
                if (isFilled) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSecondaryContainer

            val iconScale = lerp(1f, 0.85f, pressProgress.value)
            val width = ((42f + 10f * pressProgress.value) * widthMultiplier * BUTTON_SIZE_SCALE).dp

            Box(
                modifier = Modifier
                    .requiredWidth(width)
                    .requiredHeight(52.dp * BUTTON_SIZE_SCALE)
                    .clip(shape)
                    .background(containerColor, shape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        },
                    )
                    .padding(horizontal = 10.dp * BUTTON_SIZE_SCALE),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .size(24.dp * BUTTON_SIZE_SCALE)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                    )
                }
            }
        },
        menuContent = {},
    )
}
