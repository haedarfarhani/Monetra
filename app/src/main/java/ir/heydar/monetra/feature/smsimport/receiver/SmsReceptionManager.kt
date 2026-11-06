package ir.heydar.monetra.feature.smsimport.receiver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.heydar.monetra.core.datastore.PreferencesManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsReceptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    val isAutoDetectionEnabled: Flow<Boolean>
        get() = preferencesManager.bankNotificationsEnabled

    suspend fun setAutoDetectionEnabled(enabled: Boolean) {
        preferencesManager.setBankNotificationsEnabled(enabled)
    }

    fun getGooglePlayPolicyNotice(): String {
        return "READ_SMS and RECEIVE_SMS permissions are restricted by Google Play policy. " +
            "If direct SMS permission is not granted, Monetra provides zero-permission alternatives: " +
            "sharing bank SMS messages via the Android Share sheet, or manually pasting transaction text."
    }
}
