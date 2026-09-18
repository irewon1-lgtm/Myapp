package com.futuretech.poweruser

import android.app.Application

/**
 * Application shell for CodingCoding.
 *
 * Textbook refresh is owned by the reader so each selected TRACK is refreshed independently.
 * Legacy V5 revision migration/cache code is intentionally removed because CodingCoding uses its
 * own project-locked cache namespace.
 */
class PowerUserApplication : Application()
