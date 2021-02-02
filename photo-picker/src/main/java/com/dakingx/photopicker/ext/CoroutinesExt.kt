package com.dakingx.photopicker.ext

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume

fun <T> CancellableContinuation<T>.resumeSafely(value: T): Unit {
    if (isCompleted.not()) {
        resume(value)
    }
}
