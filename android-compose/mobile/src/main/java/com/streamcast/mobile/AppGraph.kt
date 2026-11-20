package com.streamcast.mobile

import android.content.Context
import com.streamcast.mobile.atv.AtvRemoteController
import com.streamcast.mobile.cast.AndroidCastAndRemoteManager
import com.streamcast.shared.data.PlaylistRepository
import com.streamcast.shared.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Process-wide singletons.
 *
 * The playlist repository, preferences and Cast manager must be shared between
 * the channel and cast ViewModels - two repository instances would mean two
 * caches, two network fetches and two copies of the channel list.
 */
object AppGraph {

    @Volatile
    private var repository: PlaylistRepository? = null

    @Volatile
    private var preferences: UserPreferences? = null

    @Volatile
    private var castManager: AndroidCastAndRemoteManager? = null

    fun playlistRepository(context: Context): PlaylistRepository =
        repository ?: synchronized(this) {
            repository ?: PlaylistRepository(context.applicationContext).also { repository = it }
        }

    fun userPreferences(context: Context): UserPreferences =
        preferences ?: synchronized(this) {
            preferences ?: UserPreferences(context.applicationContext).also { preferences = it }
        }

    fun castManager(context: Context): AndroidCastAndRemoteManager =
        castManager ?: synchronized(this) {
            castManager ?: AndroidCastAndRemoteManager(context.applicationContext).also {
                castManager = it
            }
        }

    @Volatile
    private var atvRemote: AtvRemoteController? = null

    /**
     * Scope for the Android TV session.
     *
     * The session is a single long-lived TCP connection that the TV will not
     * hold twice, so it has to outlive any one screen or ViewModel: leaving the
     * remote tab and coming back must not drop it. A SupervisorJob keeps one
     * failed read loop from cancelling the rest.
     */
    private val remoteScope = CoroutineScope(SupervisorJob())

    fun atvRemote(context: Context): AtvRemoteController =
        atvRemote ?: synchronized(this) {
            atvRemote ?: AtvRemoteController(
                context = context.applicationContext,
                scope = remoteScope,
                packageName = context.applicationContext.packageName,
                appVersion = APP_VERSION
            ).also { atvRemote = it }
        }

    /** Reported to the TV during the remote handshake. */
    private const val APP_VERSION = "1.0"
}
