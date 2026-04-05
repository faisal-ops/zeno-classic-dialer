package com.zeno.dialer

/**
 * Singleton bridge that lets [KeyHandler] scroll the Favorites LazyColumn
 * via D-pad without a direct Compose dependency.
 *
 * [FavoritesContent] registers [scrollUp]/[scrollDown] callbacks while it is
 * in the composition and clears them when it leaves.
 */
object FavoritesScrollController {
    var scrollUp:   (() -> Unit)? = null
    var scrollDown: (() -> Unit)? = null
}
