package ir.heydar.monetra

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ir.heydar.monetra.core.datastore.PreferencesManager
import ir.heydar.monetra.core.permissions.PermissionManager
import ir.heydar.monetra.core.designsystem.components.LoadingIndicator
import ir.heydar.monetra.core.designsystem.theme.AppTheme
import ir.heydar.monetra.core.designsystem.theme.ThemeMode
import ir.heydar.monetra.core.localization.LanguageManager
import ir.heydar.monetra.core.localization.LanguageRepository
import ir.heydar.monetra.core.localization.ProvideAppLocale
import ir.heydar.monetra.core.navigation.AppNavigation
import ir.heydar.monetra.core.security.BiometricManager
import ir.heydar.monetra.feature.smsimport.SharedTextStore
import javax.inject.Inject

/**
 * Biometric-gated entry activity. When the app-lock setting is on, the UI stays
 * behind a lock screen and a system biometric prompt is raised on every resume.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var languageManager: LanguageManager

    @Inject
    lateinit var languageRepository: LanguageRepository

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var sharedTextStore: SharedTextStore

    @Inject
    lateinit var biometricManager: BiometricManager

    @Inject
    lateinit var permissionManager: PermissionManager

    private val pendingTransactionIdFlow = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedTextStore.offer(intent?.getStringExtra(Intent.EXTRA_TEXT))
        extractPendingTransactionId(intent)
        enableEdgeToEdge()
        setContent {
            val isFirstLaunch by languageRepository.isFirstLaunch.collectAsStateWithLifecycle(initialValue = null)
            val selectedLanguage by languageRepository.selectedLanguageCode.collectAsStateWithLifecycle(initialValue = null)
            val themeMode by preferencesManager.themeMode.collectAsStateWithLifecycle(initialValue = null)
            val sharedText by sharedTextStore.text.collectAsStateWithLifecycle()
            val pendingTxId by pendingTransactionIdFlow.collectAsStateWithLifecycle()
            val biometricEnabled by preferencesManager.isBiometricEnabled.collectAsStateWithLifecycle(initialValue = null)
            val permissionStatus by permissionManager.status.collectAsStateWithLifecycle()
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionManager.refresh() }

            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (ThemeMode.fromKey(themeMode)) {
                ThemeMode.SYSTEM -> systemInDarkTheme
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // enableEdgeToEdge() decides the status/navigation bar icon color from the *system*
            // dark mode. When the app forces light while the system is dark (or vice versa), the
            // icons keep the wrong tint and disappear against the background, so re-apply them
            // from the theme actually in use.
            val view = LocalView.current
            val activity = view.context.findActivity()
            androidx.compose.runtime.SideEffect {
                if (activity != null) {
                    WindowCompat.getInsetsController(activity.window, view).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
            }

            // ---- App lock (biometric) ----
            var unlocked by remember { mutableStateOf(false) }
            var promptTick by remember { mutableIntStateOf(0) }

            // Re-lock whenever the app goes to the background.
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) unlocked = false
                    // Grants can change while we are away (system dialog, app settings).
                    if (event == Lifecycle.Event.ON_RESUME) permissionManager.refresh()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val showLock = biometricEnabled == true && !unlocked

            // Ask once, as soon as onboarding is done: the dashboard then reports the real state
            // instead of the app nagging on every launch.
            LaunchedEffect(isFirstLaunch, showLock, permissionStatus.promptShown) {
                if (isFirstLaunch == false && !showLock && !permissionStatus.promptShown) {
                    val missing = permissionManager.missingPermissions()
                    permissionManager.markPromptShown()
                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing)
                    }
                }
            }
            val lockTitle = stringResource(id = ir.heydar.monetra.R.string.app_name)
            val lockSubtitle = stringResource(id = ir.heydar.monetra.R.string.settings_lock_message)
            val lockNegative = stringResource(id = ir.heydar.monetra.R.string.action_cancel)

            LaunchedEffect(showLock, promptTick) {
                if (showLock) {
                    biometricManager.authenticate(
                        activity = this@MainActivity,
                        title = lockTitle,
                        subtitle = lockSubtitle,
                        negativeButtonText = lockNegative,
                        onSuccess = { unlocked = true },
                        onError = { /* the lock screen stays with a manual retry */ }
                    )
                }
            }

            // Wraps the theme as well so typography is resolved against the selected locale.
            ProvideAppLocale(languageCode = selectedLanguage.orEmpty()) {
                AppTheme(darkTheme = darkTheme) {
                    when {
                        isFirstLaunch == null || selectedLanguage == null || biometricEnabled == null ->
                            LoadingIndicator()
                        showLock -> LockScreen(
                            onRetry = { promptTick++ }
                        )
                        else -> AppNavigation(
                            isFirstLaunch = isFirstLaunch!!,
                            sharedText = sharedText,
                            pendingTransactionId = pendingTxId
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedTextStore.offer(intent.getStringExtra(Intent.EXTRA_TEXT))
        extractPendingTransactionId(intent)
    }

    private fun extractPendingTransactionId(intent: Intent?) {
        val id = intent?.getLongExtra(ir.heydar.monetra.core.notification.BankTransactionNotifier.EXTRA_PENDING_TRANSACTION_ID, -1L) ?: -1L
        if (id > 0L) {
            pendingTransactionIdFlow.value = id
        }
    }
}

/** Full-screen gate shown while the app is locked. */
@Composable
private fun LockScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
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
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(id = ir.heydar.monetra.R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = ir.heydar.monetra.R.string.settings_lock_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = stringResource(id = ir.heydar.monetra.R.string.retry))
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context: Context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
