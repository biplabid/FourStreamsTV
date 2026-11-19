package com.streamcast.mobile

import android.content.Context
import com.streamcast.mobile.cast.AndroidCastAndRemoteManager
import com.streamcast.shared.data.PlaylistRepository
import com.streamcast.shared.data.UserPreferences

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
}
