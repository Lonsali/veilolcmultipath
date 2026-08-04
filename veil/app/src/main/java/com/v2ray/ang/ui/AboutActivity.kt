@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.v2ray.ang.ui

import android.webkit.WebView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toPath
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.graphics.shapes.Morph
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.extension.toast
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ENTRANCE_STAGGER_MS = 70L
private const val HERO_SIZE_DP = 148
private const val HERO_LOGO_DP = 112
private const val HERO_PRESS_SCALE = 0.88f
private const val HERO_ROTATION_STEP_DEG = 45f

class AboutActivity : BaseComponentActivity() {

    @Composable
    override fun ScreenContent() {
        AboutScreen(onBackClick = { finish() })
    }
}

@Composable
fun AboutScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var showOssDialog by remember { mutableStateOf(false) }

    val versionText = "v${BuildConfig.VERSION_NAME}"
    val coreVersion = remember { CoreNativeManager.getLibVersion() }
    val appId = BuildConfig.APPLICATION_ID
    val versionDetails = "Veil $versionText • $coreVersion • $appId"

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_about),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AboutHero()

            VersionChip(
                versionText = versionText,
                versionDetails = versionDetails
            )

            val connectItems = listOf(
                AboutLinkItem(
                    icon = painterResource(R.drawable.ic_source_code_24dp),
                    title = stringResource(R.string.title_source_code),
                    subtitle = AppConfig.APP_URL.removePrefix("https://"),
                    external = true,
                    onClick = { Utils.openUri(context, AppConfig.APP_URL) }
                ),
                AboutLinkItem(
                    icon = painterResource(R.drawable.ic_feedback_24dp),
                    title = stringResource(R.string.title_pref_feedback),
                    subtitle = stringResource(R.string.summary_pref_feedback),
                    external = true,
                    onClick = { Utils.openUri(context, AppConfig.APP_ISSUES_URL) }
                ),
                AboutLinkItem(
                    icon = painterResource(R.drawable.ic_telegram_24dp),
                    title = stringResource(R.string.title_tg_channel),
                    subtitle = AppConfig.TG_CHANNEL_URL.removePrefix("https://"),
                    external = true,
                    onClick = { Utils.openUri(context, AppConfig.TG_CHANNEL_URL) }
                ),
                AboutLinkItem(
                    icon = painterResource(R.drawable.ic_source_code_24dp),
                    title = stringResource(R.string.title_original_repo),
                    subtitle = AppConfig.APP_ORIGINAL_URL.removePrefix("https://"),
                    external = true,
                    onClick = { Utils.openUri(context, AppConfig.APP_ORIGINAL_URL) }
                ),
            )
            ExpressiveEntrance(index = 1) {
                AboutLinkGroup(
                    title = stringResource(R.string.about_section_connect),
                    items = connectItems
                )
            }

            val legalItems = listOf(
                AboutLinkItem(
                    icon = painterResource(R.drawable.license_24px),
                    title = stringResource(R.string.title_oss_license),
                    subtitle = null,
                    external = false,
                    onClick = { showOssDialog = true }
                ),
                AboutLinkItem(
                    icon = painterResource(R.drawable.ic_privacy_24dp),
                    title = stringResource(R.string.title_privacy_policy),
                    subtitle = null,
                    external = true,
                    onClick = { Utils.openUri(context, AppConfig.APP_PRIVACY_POLICY) }
                ),
            )
            ExpressiveEntrance(index = 2) {
                AboutLinkGroup(
                    title = stringResource(R.string.about_section_legal),
                    items = legalItems
                )
            }

            ExpressiveEntrance(index = 3) {
                AboutFooter(coreVersion = coreVersion, appId = appId)
            }
        }
    }

    if (showOssDialog) {
        AlertDialog(
            onDismissRequest = { showOssDialog = false },
            title = { Text(stringResource(R.string.title_oss_license)) },
            text = {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            loadUrl("file:///android_asset/open_source_licenses.html")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showOssDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(bottom = 60.dp)
        )
    }
}

@Composable
private fun AboutHero() {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val motionScheme = MaterialTheme.motionScheme

    val heroShapes = remember {
        listOf(
            MaterialShapes.Cookie9Sided,
            MaterialShapes.Clover4Leaf,
            MaterialShapes.Sunny,
            MaterialShapes.SoftBurst,
            MaterialShapes.Gem,
            MaterialShapes.Flower,
        )
    }
    var shapeIndex by remember { mutableIntStateOf(0) }
    var startShape by remember { mutableStateOf(heroShapes.first()) }
    var endShape by remember { mutableStateOf(heroShapes.first()) }
    val morph = remember(startShape, endShape) { Morph(startShape, endShape) }
    val morphProgress = remember { Animatable(1f) }
    val blobRotation = remember { Animatable(0f) }

    val entranceScale = remember { Animatable(0.4f) }
    val entranceAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { entranceScale.animateTo(1f, motionScheme.slowSpatialSpec()) }
        launch { entranceAlpha.animateTo(1f, motionScheme.defaultEffectsSpec()) }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress = remember { Animatable(0f) }
    LaunchedEffect(isPressed) {
        pressProgress.animateTo(
            targetValue = if (isPressed) 1f else 0f,
            animationSpec = if (isPressed) motionScheme.fastSpatialSpec() else motionScheme.defaultSpatialSpec()
        )
    }

    val blobColor = MaterialTheme.colorScheme.primaryContainer
    val iconTint = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
            .graphicsLayer {
                scaleX = entranceScale.value
                scaleY = entranceScale.value
                alpha = entranceAlpha.value
            }
    ) {
        Box(
            modifier = Modifier
                .size(HERO_SIZE_DP.dp)
                .graphicsLayer {
                    val pressScale = lerp(1f, HERO_PRESS_SCALE, pressProgress.value)
                    scaleX = pressScale
                    scaleY = pressScale
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = blobRotation.value }
                    .background(blobColor, MorphShape(morph, morphProgress.value))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.about_logo_description)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        shapeIndex = (shapeIndex + 1) % heroShapes.size
                        startShape = endShape
                        endShape = heroShapes[shapeIndex]
                        scope.launch {
                            morphProgress.snapTo(0f)
                            morphProgress.animateTo(1f, motionScheme.slowSpatialSpec())
                        }
                        scope.launch {
                            blobRotation.animateTo(
                                blobRotation.value + HERO_ROTATION_STEP_DEG,
                                motionScheme.slowSpatialSpec()
                            )
                        }
                    }
            )
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                colorFilter = ColorFilter.tint(iconTint),
                modifier = Modifier.size(HERO_LOGO_DP.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.summary_app_fork),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun VersionChip(
    versionText: String,
    versionDetails: String
) {
    val context = LocalContext.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraExtraLarge)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(
                    onClick = {
                        Utils.setClipboard(context, versionDetails)
                        context.toast(R.string.about_copied_to_clipboard)
                    }
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = versionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

private data class AboutLinkItem(
    val icon: Painter,
    val title: String,
    val subtitle: String?,
    val external: Boolean,
    val onClick: () -> Unit
)

private val iconContainerColors: List<Pair<Color, Color>>
    @Composable
    get() = with(MaterialTheme.colorScheme) {
        listOf(
            primaryContainer to onPrimaryContainer,
            secondaryContainer to onSecondaryContainer,
            tertiaryContainer to onTertiaryContainer,
        )
    }

@Composable
private fun AboutSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun AboutLinkGroup(
    title: String,
    items: List<AboutLinkItem>,
    modifier: Modifier = Modifier
) {
    val containerColors = iconContainerColors
    Column(modifier = modifier.fillMaxWidth()) {
        AboutSectionHeader(title = title)
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            items.forEachIndexed { index, item ->
                val (containerColor, contentColor) = containerColors[index % containerColors.size]
                SegmentedListItem(
                    onClick = item.onClick,
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = items.size),
                    colors = ListItemDefaults.segmentedColors(),
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(containerColor, MaterialTheme.shapes.largeIncreased),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = item.icon,
                                contentDescription = null,
                                tint = contentColor
                            )
                        }
                    },
                    supportingContent = item.subtitle?.let { subtitle ->
                        { Text(subtitle) }
                    },
                    trailingContent = if (item.external) {
                        {
                            Icon(
                                painter = painterResource(R.drawable.ic_open_in_browser_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        null
                    },
                    content = { Text(item.title) }
                )
            }
        }
    }
}

@Composable
private fun AboutFooter(
    coreVersion: String,
    appId: String
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Text(
            text = coreVersion,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = appId,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable {
                    Utils.setClipboard(context, appId)
                    context.toast(R.string.about_copied_to_clipboard)
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ExpressiveEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ENTRANCE_STAGGER_MS * index)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "entranceAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 32f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "entranceOffsetY"
    )
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY.dp.toPx()
        }
    ) {
        content()
    }
}

private class MorphShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = morph.toPath(progress.coerceIn(0f, 1f))
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}
