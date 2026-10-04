package dev.minios.ocremote.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.minios.ocremote.R
import dev.minios.ocremote.data.repository.*
import dev.minios.ocremote.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PresentationPreferenceKind { PROJECT_APPEARANCE, SESSION_APPEARANCE, SERVER_TIMELINE, SESSION_TIMELINE, APP_NOTIFICATIONS, SERVER_NOTIFICATIONS, SESSION_NOTIFICATIONS }

data class PresentationPreferenceRequest(
    val kind: PresentationPreferenceKind,
    val serverId: String = "",
    val sessionId: String = "",
    val projectId: String = "",
    val directory: String = "",
)

@HiltViewModel
class PresentationPreferencesViewModel @Inject constructor(
    settings: SettingsRepository,
    val appearance: AppearanceRepository,
    val timeline: TimelineRepository,
    val notifications: NotificationPreferencesRepository,
) : ViewModel() {
    private val _saveFailed = MutableStateFlow(false)
    val saveFailed = _saveFailed.asStateFlow()
    val appAppearance = combine(settings.appTheme, settings.appAccent, settings.dynamicColor, settings.amoledDark) {
        theme, accent, dynamic, amoled -> ThemeAppearance(theme, accent, dynamic, amoled)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeAppearance())
    val autoScrollEnabled: StateFlow<Boolean?> = settings.autoScrollEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val notificationDefaults: StateFlow<NotificationPreferences?> = combine(settings.notificationsEnabled, settings.silentNotifications, ::legacyNotificationPreferences)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun clearError() { _saveFailed.value = false }

    fun save(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { _saveFailed.value = true }
        }
    }
}

/** One host exposes presentation preferences without passing server credentials into dialogs. */
@Composable
fun PresentationPreferencesHost(
    request: PresentationPreferenceRequest?,
    onDismiss: () -> Unit,
    viewModel: PresentationPreferencesViewModel = hiltViewModel(),
) {
    val failed by viewModel.saveFailed.collectAsState()
    if (failed) AlertDialog(
        onDismissRequest = viewModel::clearError,
        text = { Text(stringResource(R.string.presentation_save_failed)) },
        confirmButton = { TextButton(onClick = viewModel::clearError) { Text(stringResource(R.string.appearance_cancel)) } },
    )
    if (request == null) return
    key(request) {
        val app by viewModel.appAppearance.collectAsState()
        when (request.kind) {
            PresentationPreferenceKind.PROJECT_APPEARANCE, PresentationPreferenceKind.SESSION_APPEARANCE -> {
                val projectFlow = remember(request) { viewModel.appearance.projectAppearance(request.serverId, request.projectId, request.directory) }
                val sessionFlow = remember(request) { viewModel.appearance.sessionAppearance(request.serverId, request.sessionId) }
                val project by projectFlow.collectAsState(initial = null)
                val session by sessionFlow.collectAsState(initial = null)
                val isProject = request.kind == PresentationPreferenceKind.PROJECT_APPEARANCE
                AppearanceSettingsDialog(
                    title = stringResource(if (isProject) R.string.appearance_project_title else R.string.appearance_session_title),
                    value = if (isProject) project else session,
                    inherited = if (isProject) app else resolveAppearance(app, project),
                    onSave = { value -> viewModel.save {
                        if (isProject) viewModel.appearance.saveProject(request.serverId, request.projectId, request.directory, value)
                        else viewModel.appearance.saveSession(request.serverId, request.sessionId, value)
                    } },
                    onDismiss = onDismiss,
                )
            }
            PresentationPreferenceKind.SERVER_TIMELINE, PresentationPreferenceKind.SESSION_TIMELINE -> {
                val serverFlow = remember(request) { viewModel.timeline.serverTimeline(request.serverId) }
                val sessionFlow = remember(request) { viewModel.timeline.sessionOverride(request.serverId, request.sessionId) }
                val server by serverFlow.collectAsState(initial = TimelinePreferences())
                val session by sessionFlow.collectAsState(initial = null)
                val isSession = request.kind == PresentationPreferenceKind.SESSION_TIMELINE
                TimelineSettingsDialog(
                    title = stringResource(R.string.timeline_title),
                    value = if (isSession) session else server,
                    inherited = server,
                    allowInherit = isSession,
                    onSave = { value -> viewModel.save {
                        if (isSession) viewModel.timeline.setSession(request.serverId, request.sessionId, value)
                        else viewModel.timeline.setServer(request.serverId, value ?: TimelinePreferences())
                    } },
                    onDismiss = onDismiss,
                )
            }
            else -> {
                val scope = when (request.kind) {
                    PresentationPreferenceKind.APP_NOTIFICATIONS -> NotificationPreferenceScope()
                    PresentationPreferenceKind.SERVER_NOTIFICATIONS -> NotificationPreferenceScope(request.serverId)
                    else -> NotificationPreferenceScope(request.serverId, request.sessionId)
                }
                val snapshot by viewModel.notifications.all.collectAsState(initial = null)
                val defaults by viewModel.notificationDefaults.collectAsState()
                val loadedSnapshot = snapshot
                val loadedDefaults = defaults
                if (loadedSnapshot == null || loadedDefaults == null) {
                    AppDialog(onDismissRequest = onDismiss) { CircularProgressIndicator() }
                } else NotificationPreferencesDialog(
                    preferences = loadedSnapshot.resolve(scope.serverId, scope.sessionId, loadedDefaults),
                    inherited = loadedSnapshot.overrides[scope] == null,
                    allowInheritance = scope.serverId != null,
                    onSave = { value -> viewModel.save { viewModel.notifications.set(scope, value) } },
                    onDismiss = onDismiss,
                )
            }
        }
    }
}
