package com.futuretech.poweruser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * V1TextbookScreen also persists the selected chapter in TextbookProgressStore.
 * This helper intentionally mirrors the one-argument rememberSaveable call used
 * by that screen while durable textbook state remains in SharedPreferences.
 */
@Composable
internal fun <T> rememberSaveable(initializer: () -> T): T = remember { initializer() }
