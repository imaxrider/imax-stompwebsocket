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

package com.imax.stompwebsocket.dto

import java.util.TreeMap

data class LifecycleEvent @JvmOverloads constructor(
    val type: Type,
    val exception: Exception? = null,
    val message: String? = null
) {
    enum class Type {
        OPENED, CLOSED, ERROR, FAILED_SERVER_HEARTBEAT
    }

    var handshakeResponseHeaders: TreeMap<String, String> = TreeMap()
}
