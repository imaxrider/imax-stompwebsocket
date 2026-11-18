/*
 * Copyright (C) 2024-2026 IMAX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.imax.stompwebsocket

import android.util.Log

/**
 * Logger abstraction for STOMP client internals, allowing custom logging or disabling logs in production/tests.
 */
interface StompLogger {
    fun d(tag: String, message: String) {}
    fun i(tag: String, message: String) {}
    fun w(tag: String, message: String) {}
    fun e(tag: String, message: String, throwable: Throwable? = null) {}

    companion object {
        @JvmField
        val ANDROID: StompLogger = object : StompLogger {
            override fun d(tag: String, message: String) {
                Log.d(tag, message)
            }

            override fun i(tag: String, message: String) {
                Log.i(tag, message)
            }

            override fun w(tag: String, message: String) {
                Log.w(tag, message)
            }

            override fun e(tag: String, message: String, throwable: Throwable?) {
                Log.e(tag, message, throwable)
            }
        }

        @JvmField
        val NONE: StompLogger = object : StompLogger {}
    }
}
