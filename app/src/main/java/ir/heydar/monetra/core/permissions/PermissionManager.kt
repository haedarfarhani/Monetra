package ir.heydar.monetra.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.heydar.monetra.core.datastore.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** The runtime permissions Monetra needs, and whether they are currently usable. */
data class PermissionStatus(
    val smsGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    /** True once the system dialogs have been raised, so they are not repeated on every launch. */
    val promptShown: Boolean = false
) {
    val anyMissing: Boolean
        get() = !smsGranted || !notificationsGranted
}

/**
 * Single source of truth for SMS and notification access. The flags are re-read from the
 * system on [refresh] because the user can change them at any time in system settings.
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow(
        PermissionStatus(
            smsGranted = hasSmsPermission(),
            notificationsGranted = areNotificationsEnabled()
        )
    )
    val status: StateFlow<PermissionStatus> = _status.asStateFlow()

    init {
        scope.launch {
            preferencesManager.permissionPromptShown.collect { shown ->
                _status.value = readStatus().copy(promptShown = shown)
            }
        }
    }

    fun refresh() {
        _status.value = readStatus().copy(promptShown = _status.value.promptShown)
    }

    fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

    /** Also false when the user switched notifications off for the app in system settings. */
    fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Permissions still missing a grant. The notification toggle on Android 12 and older is a
     * system setting rather than a runtime permission, so it is not part of this list.
     */
    fun missingPermissions(): Array<String> = buildList {
        if (!hasSmsPermission()) add(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    suspend fun markPromptShown() = preferencesManager.setPermissionPromptShown()

    /** Opens Monetra's page in system settings, where a permanently denied grant can be restored. */
    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun readStatus() = PermissionStatus(
        smsGranted = hasSmsPermission(),
        notificationsGranted = areNotificationsEnabled()
    )
}
