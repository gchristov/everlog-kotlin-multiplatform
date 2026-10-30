package com.everlog.android.common.kotlin

import kotlin.math.max

/**
 * Brzycki's estimate, in the unit of [weight] - https://en.wikipedia.org/wiki/One-repetition_maximum
 */
fun estimate1RM(weight: Float, reps: Int): Float = max(0f, weight / (1.0278f - 0.0278f * reps))
