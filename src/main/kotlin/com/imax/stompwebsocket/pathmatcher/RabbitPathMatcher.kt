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

package com.imax.stompwebsocket.pathmatcher

import com.imax.stompwebsocket.dto.StompHeader
import com.imax.stompwebsocket.dto.StompMessage

class RabbitPathMatcher : PathMatcher {
    override fun matches(path: String, msg: StompMessage): Boolean {
        val dest = msg.findHeader(StompHeader.DESTINATION) ?: return false
        val split = path.split(".")
        val transformed = ArrayList<String>()
        for (s in split) {
            when (s) {
                "*" -> transformed.add("[^.]+")
                "#" -> transformed.add(".*")
                else -> transformed.add(s.replace("*", ".*"))
            }
        }
        val join = transformed.joinToString("\\.")
        return dest.matches(Regex(join))
    }
}
