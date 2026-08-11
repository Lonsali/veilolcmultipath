package com.v2ray.ang.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R

data class ConnectionInfoSheetState(
    val visible: Boolean = false,
    val ip: String? = null,
    val location: String? = null,
    val isp: String? = null,
    val downloadSpeed: String = "0 B/s",
    val uploadSpeed: String = "0 B/s",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionInfoSheet(
    state: ConnectionInfoSheetState,
    onDismiss: () -> Unit,
    onCopyIp: () -> Unit,
    onPing: () -> Unit,
    onShare: () -> Unit,
) {
    if (!state.visible) return

    val leftPillShape = RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp, topEnd = 2.dp, bottomEnd = 2.dp)
    val rightPillShape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 999.dp, bottomEnd = 999.dp)

    val topShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    val middleShape = RoundedCornerShape(4.dp)
    val bottomShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 420.dp)
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.connection_info_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ConnectionInfoRow(
                    icon = Icons.Filled.Public,
                    label = stringResource(R.string.connection_info_ip),
                    value = state.ip ?: stringResource(R.string.connection_info_loading),
                    shape = topShape,
                )
                ConnectionInfoRow(
                    icon = Icons.Filled.LocationOn,
                    label = stringResource(R.string.connection_info_location),
                    value = state.location ?: stringResource(R.string.connection_info_loading),
                    shape = middleShape,
                )
                ConnectionInfoRow(
                    icon = Icons.Filled.Business,
                    label = stringResource(R.string.connection_info_isp),
                    value = state.isp ?: stringResource(R.string.connection_info_loading),
                    shape = bottomShape,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SpeedPill(
                    icon = Icons.Filled.ArrowDownward,
                    label = stringResource(R.string.connection_info_download),
                    value = state.downloadSpeed,
                    shape = leftPillShape,
                    modifier = Modifier.weight(1f),
                )
                SpeedPill(
                    icon = Icons.Filled.ArrowUpward,
                    label = stringResource(R.string.connection_info_upload),
                    value = state.uploadSpeed,
                    shape = rightPillShape,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ConnectionInfoAction(
                    icon = Icons.Filled.ContentCopy,
                    label = stringResource(R.string.connection_info_action_copy_ip),
                    enabled = !state.ip.isNullOrBlank() && state.ip != "-",
                    onClick = onCopyIp,
                    modifier = Modifier.weight(1f),
                )
                ConnectionInfoAction(
                    icon = Icons.Filled.NetworkPing,
                    label = stringResource(R.string.connection_info_action_ping),
                    enabled = true,
                    onClick = onPing,
                    modifier = Modifier.weight(1f),
                )
                ConnectionInfoAction(
                    icon = Icons.Filled.Share,
                    label = stringResource(R.string.connection_info_action_share),
                    enabled = true,
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ConnectionInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    shape: Shape,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SpeedPill(
    icon: ImageVector,
    label: String,
    value: String,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 64.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ConnectionInfoAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
