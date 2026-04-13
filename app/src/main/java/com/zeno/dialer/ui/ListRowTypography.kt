package com.zeno.dialer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Shared text styles for contact rows on Favorites (suggestions) and Home (recents).
 * Single source of truth so name/subtitle sizing cannot drift between screens.
 */
@Composable
internal fun contactListPrimaryTextStyle(selected: Boolean): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
    )

@Composable
internal fun contactListSecondaryTextStyle(): TextStyle =
    MaterialTheme.typography.bodySmall
