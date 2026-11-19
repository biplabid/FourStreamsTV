package com.streamcast.mobile.cast

import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity

/**
 * Full-screen Cast controls, launched from the playback notification and the
 * lock screen. Supplied by the Cast framework; we only need to declare it so
 * [StreamCastOptionsProvider] has a target activity to point at.
 */
class ExpandedControlsActivity : ExpandedControllerActivity()
