package ir.heydar.monetra.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.heydar.monetra.R
import ir.heydar.monetra.core.calendar.CalendarSystem
import ir.heydar.monetra.core.currency.AppCurrency
import ir.heydar.monetra.core.designsystem.components.GlassFilterChip
import ir.heydar.monetra.core.designsystem.components.GlassFilterChip
import ir.heydar.monetra.core.designsystem.components.GlassSwitch
import ir.heydar.monetra.core.designsystem.glass.GlassSurface
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.ThemeMode

@Composable
fun SettingsRoute(
    onNavigateToCategories: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val draftsClearedMessage = stringResource(id = R.string.settings_drafts_cleared)

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SettingsUiEffect.DraftsCleared -> snackbarHostState.showSnackbar(draftsClearedMessage)
                is SettingsUiEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    val promptTitle = stringResource(id = R.string.app_name)
    val promptSubtitle = stringResource(id = R.string.settings_lock_message)
    val promptNegative = stringResource(id = R.string.action_cancel)

    SettingsScreen(
        uiState = uiState,
        isBiometricAvailable = viewModel.isBiometricAvailable,
        snackbarHostState = snackbarHostState,
        onNavigateToCategories = onNavigateToCategories,
        onEvent = viewModel::onEvent,
        onAuthenticate = { activity, onSuccess, onError ->
            viewModel.authenticate(activity, promptTitle, promptSubtitle, promptNegative, onSuccess, onError)
        }
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    isBiometricAvailable: Boolean,
    snackbarHostState: SnackbarHostState,
    onNavigateToCategories: () -> Unit = {},
    onEvent: (SettingsUiEvent) -> Unit,
    onAuthenticate: (FragmentActivity, () -> Unit, (String) -> Unit) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val snackbarUnavailable = stringResource(id = R.string.settings_biometric_unavailable)

    // One inline selector open at a time — no dialogs needed.
    var expandedSection by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spacingMd)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
        ) {
            AppBrandHeaderCard(
                appName = stringResource(id = R.string.app_name),
                tagline = stringResource(id = R.string.onboarding_welcome_tagline),
                appVersion = uiState.appVersion
            )

            // Section 1: Preferences — inline expandable selectors, zero dialogs.
            SettingsGroup(title = stringResource(id = R.string.settings_section_general)) {
                InlineOptionRow(
                    key = "theme",
                    icon = Icons.Outlined.Palette,
                    iconColor = MaterialTheme.colorScheme.error,
                    title = stringResource(id = R.string.theme_mode),
                    currentLabel = when (ThemeMode.fromKey(uiState.themeMode)) {
                        ThemeMode.LIGHT -> stringResource(id = R.string.theme_light)
                        ThemeMode.DARK -> stringResource(id = R.string.theme_dark)
                        else -> stringResource(id = R.string.theme_system)
                    },
                    expanded = expandedSection == "theme",
                    onToggle = { expandedSection = if (expandedSection == "theme") null else "theme" }
                ) {
                    OptionChipsRow(
                        options = listOf(
                            ThemeMode.SYSTEM.key to stringResource(id = R.string.theme_system),
                            ThemeMode.LIGHT.key to stringResource(id = R.string.theme_light),
                            ThemeMode.DARK.key to stringResource(id = R.string.theme_dark)
                        ),
                        selectedKey = uiState.themeMode,
                        onSelect = { key ->
                            onEvent(SettingsUiEvent.ChangeThemeMode(key))
                            expandedSection = null
                        }
                    )
                }

                SettingsDivider()

                InlineOptionRow(
                    key = "language",
                    icon = Icons.Outlined.Language,
                    iconColor = Color(0xFF7A5642),
                    title = stringResource(id = R.string.language),
                    currentLabel = when (uiState.language) {
                        "fa" -> stringResource(id = R.string.language_persian)
                        "en" -> stringResource(id = R.string.language_english)
                        else -> stringResource(id = R.string.language_system_default)
                    },
                    expanded = expandedSection == "language",
                    onToggle = { expandedSection = if (expandedSection == "language") null else "language" }
                ) {
                    OptionChipsRow(
                        options = listOf(
                            "" to stringResource(id = R.string.language_system_default),
                            "fa" to stringResource(id = R.string.language_persian),
                            "en" to stringResource(id = R.string.language_english)
                        ),
                        selectedKey = uiState.language,
                        onSelect = { key ->
                            onEvent(SettingsUiEvent.ChangeLanguage(key))
                            expandedSection = null
                        }
                    )
                }

                SettingsDivider()

                InlineOptionRow(
                    key = "calendar",
                    icon = Icons.Outlined.CalendarMonth,
                    iconColor = Color(0xFF5FA873),
                    title = stringResource(id = R.string.calendar_title),
                    currentLabel = if (uiState.calendar == CalendarSystem.JALALI.key) {
                        stringResource(id = R.string.calendar_jalali)
                    } else {
                        stringResource(id = R.string.calendar_gregorian)
                    },
                    expanded = expandedSection == "calendar",
                    onToggle = { expandedSection = if (expandedSection == "calendar") null else "calendar" }
                ) {
                    OptionChipsRow(
                        options = CalendarSystem.entries.map { system ->
                            system.key to stringResource(
                                id = if (system == CalendarSystem.JALALI) {
                                    R.string.calendar_jalali
                                } else {
                                    R.string.calendar_gregorian
                                }
                            )
                        },
                        selectedKey = uiState.calendar,
                        onSelect = { key ->
                            onEvent(SettingsUiEvent.ChangeCalendar(key))
                            expandedSection = null
                        }
                    )
                }

                SettingsDivider()

                InlineOptionRow(
                    key = "currency",
                    icon = Icons.Outlined.Payments,
                    iconColor = Color(0xFFA2922F),
                    title = stringResource(id = R.string.currency_title),
                    currentLabel = currencyLabel(uiState.currency),
                    expanded = expandedSection == "currency",
                    onToggle = { expandedSection = if (expandedSection == "currency") null else "currency" }
                ) {
                    OptionChipsRow(
                        options = AppCurrency.entries.map { option ->
                            option.code to currencyLabel(option.code)
                        },
                        selectedKey = uiState.currency,
                        onSelect = { key ->
                            onEvent(SettingsUiEvent.ChangeCurrency(key))
                            expandedSection = null
                        }
                    )
                }

                SettingsDivider()

                SettingsActionRow(
                    icon = Icons.Outlined.Category,
                    iconColor = Color(0xFF2E8A67),
                    title = stringResource(id = R.string.categories_title),
                    subtitle = stringResource(id = R.string.categories_title),
                    onClick = onNavigateToCategories
                )
            }

            // Section 2: Security
            SettingsGroup(title = stringResource(id = R.string.settings_section_security)) {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Fingerprint,
                    iconColor = Color(0xFF2E8A67),
                    title = stringResource(id = R.string.biometric_auth),
                    subtitle = stringResource(id = R.string.settings_biometric_subtitle),
                    checked = uiState.isBiometricEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            onEvent(SettingsUiEvent.ToggleBiometric(false))
                        } else if (!isBiometricAvailable) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(SettingsUiEvent.NotifyUnavailable(snackbarUnavailable))
                        } else {
                            val activity = context as? FragmentActivity
                            if (activity == null) {
                                onEvent(SettingsUiEvent.NotifyUnavailable(snackbarUnavailable))
                            } else {
                                // Confirm with a live prompt before switching on.
                                onAuthenticate(
                                    activity,
                                    {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onEvent(SettingsUiEvent.ToggleBiometric(true))
                                    },
                                    { message ->
                                        onEvent(SettingsUiEvent.NotifyUnavailable(message))
                                    }
                                )
                            }
                        }
                    }
                )

                SettingsDivider()

                SettingsInfoRow(
                    icon = Icons.Outlined.Security,
                    iconColor = Color(0xFF5FA873),
                    title = stringResource(id = R.string.settings_privacy),
                    description = stringResource(id = R.string.settings_privacy_body)
                )
            }

            // Section 3: Smart bank SMS
            SettingsGroup(title = stringResource(id = R.string.settings_section_data)) {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Notifications,
                    iconColor = MaterialTheme.colorScheme.error,
                    title = stringResource(id = R.string.settings_bank_notif_enable),
                    checked = uiState.bankNotificationsEnabled,
                    onCheckedChange = { onEvent(SettingsUiEvent.ToggleBankNotifications(it)) }
                )

                AnimatedVisibility(
                    visible = uiState.bankNotificationsEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        SettingsSwitchRow(
                            icon = null,
                            title = stringResource(id = R.string.settings_bank_notif_sound),
                            checked = uiState.bankNotificationsSound,
                            onCheckedChange = { onEvent(SettingsUiEvent.ToggleBankNotificationsSound(it)) },
                            indented = true
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            icon = null,
                            title = stringResource(id = R.string.settings_bank_notif_show_amount),
                            checked = uiState.bankNotificationsShowAmount,
                            onCheckedChange = { onEvent(SettingsUiEvent.ToggleBankNotificationsShowAmount(it)) },
                            indented = true
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            icon = null,
                            title = stringResource(id = R.string.settings_bank_notif_hide_lockscreen),
                            checked = uiState.bankNotificationsHideLockscreen,
                            onCheckedChange = { onEvent(SettingsUiEvent.ToggleBankNotificationsHideLockscreen(it)) },
                            indented = true
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            icon = null,
                            title = stringResource(id = R.string.settings_bank_notif_auto_suggest_category),
                            checked = uiState.bankNotificationsAutoSuggestCategory,
                            onCheckedChange = { onEvent(SettingsUiEvent.ToggleBankNotificationsAutoSuggestCategory(it)) },
                            indented = true
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            icon = null,
                            title = stringResource(id = R.string.settings_bank_notif_auto_suggest_account),
                            checked = uiState.bankNotificationsAutoSuggestAccount,
                            onCheckedChange = { onEvent(SettingsUiEvent.ToggleBankNotificationsAutoSuggestAccount(it)) },
                            indented = true
                        )
                    }
                }

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Outlined.AccountBalance,
                    iconColor = Color(0xFF7A5642),
                    title = stringResource(id = R.string.settings_auto_save),
                    checked = uiState.autoSaveBankMessages,
                    onCheckedChange = { onEvent(SettingsUiEvent.ToggleAutoSave(it)) }
                )

                SettingsDivider()

                SettingsActionRow(
                    icon = Icons.Outlined.DeleteSweep,
                    iconColor = MaterialTheme.colorScheme.error,
                    title = stringResource(id = R.string.settings_clear_drafts),
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { onEvent(SettingsUiEvent.ClearPendingDrafts) }
                )
            }

            // Section 4: About
            SettingsGroup(title = stringResource(id = R.string.settings_about)) {
                SettingsInfoRow(
                    icon = Icons.Outlined.Info,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = stringResource(id = R.string.app_name),
                    description = stringResource(id = R.string.settings_version, uiState.appVersion)
                )
            }

            Spacer(modifier = Modifier.height(136.dp))
        }
    }
}

@Composable
private fun currencyLabel(code: String): String = when (AppCurrency.fromCode(code)) {
    AppCurrency.USD -> stringResource(id = R.string.currency_usd)
    AppCurrency.IRR -> stringResource(id = R.string.currency_irr)
    AppCurrency.TOMAN -> stringResource(id = R.string.currency_toman)
}

/**
 * A settings row that expands in place with a chip row — replaces the old
 * option dialogs so a single tap applies the value.
 */
@Composable
private fun InlineOptionRow(
    key: String,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    currentLabel: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "chevron"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(20.dp)
                    .rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(modifier = Modifier.padding(top = 12.dp)) {
                content()
            }
        }
    }
}

/** A horizontally scrollable row of option chips. */
@Composable
private fun OptionChipsRow(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            GlassFilterChip(
                selected = selectedKey == key,
                onClick = { onSelect(key) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun AppBrandHeaderCard(
    appName: String,
    tagline: String,
    appVersion: String
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appName.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (appVersion.isNotBlank()) {
                    Text(
                        text = "v$appVersion",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector?,
    iconColor: Color = Color.Transparent,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    indented: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(
                start = if (indented) 44.dp else 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = 10.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column {
                Text(
                    text = title,
                    style = if (indented) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    fontWeight = if (indented) FontWeight.Normal else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        GlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    )
}
