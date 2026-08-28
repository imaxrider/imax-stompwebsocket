/*
 * Copyright (C) 2016 Naver Corp. (naik-software)
 * Copyright (C) 2017 Forrest Hopkins III (forresthopkinsa)
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
import okhttp3.OkHttpClient

object Stomp {

    @Deprecated("JWS is deprecated. Use over(uri, headers, client) instead.")
    @JvmStatic
    fun over(connectionProvider: ConnectionProvider, uri: String): StompClient {
        return over(connectionProvider, uri, null, null)
    }

    @Deprecated("JWS is deprecated. Use over(uri, headers, client) instead.")
    @JvmStatic
    fun over(
        connectionProvider: ConnectionProvider,
        uri: String,
        connectHttpHeaders: Map<String, String>?
    ): StompClient {
        return over(connectionProvider, uri, connectHttpHeaders, null)
    }

    @Deprecated("JWS is deprecated. Use over(uri, headers, client) instead.")
    @JvmStatic
    fun over(
        connectionProvider: ConnectionProvider,
        uri: String,
        connectHttpHeaders: Map<String, String>?,
        okHttpClient: OkHttpClient?
    ): StompClient {
        if (connectionProvider == ConnectionProvider.JWS) {
            Log.w("Stomp", "JWS connection provider is deprecated and not supported. OkHttp client will be used.")
        }
        val client = okHttpClient ?: OkHttpClient()
        return StompClient(uri, connectHttpHeaders, client)
    }

    @JvmStatic
    @JvmOverloads
    fun over(
        uri: String,
        connectHttpHeaders: Map<String, String>? = null,
        okHttpClient: OkHttpClient? = null
    ): StompClient {
        val client = okHttpClient ?: OkHttpClient()
        return StompClient(uri, connectHttpHeaders, client)
    }

    enum class ConnectionProvider {
        OKHTTP, JWS
    }
}
