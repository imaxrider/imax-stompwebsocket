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

data class StompMessage(
    val command: StompCommand,
    val headers: List<StompHeader>,
    val payload: String?
) {

    fun findHeader(key: String): String? {
        for (header in headers) {
            if (header.key == key) {
                return header.value
            }
        }
        return null
    }

    fun compile(): String {
        return compile(false)
    }

    fun compile(legacyWhitespace: Boolean): String {
        val builder = StringBuilder()
        builder.append(command.name).append('\n')
        headers.forEach { header ->
            builder.append(header.key).append(':').append(header.value).append('\n')
        }
        builder.append('\n')
        if (payload != null) {
            builder.append(payload)
            if (legacyWhitespace) {
                builder.append("\n\n")
            }
        }
        builder.append(TERMINATE_MESSAGE_SYMBOL)
        return builder.toString()
    }

    override fun toString(): String {
        return "StompMessage{command=$command, headers=$headers, payload='$payload'}"
    }

    companion object {
        const val TERMINATE_MESSAGE_SYMBOL = "\u0000"

        @JvmStatic
        fun from(data: String?): StompMessage {
            if (data.isNullOrBlank()) {
                return StompMessage(StompCommand.UNKNOWN, emptyList(), data)
            }

            val nullIndex = data.indexOf('\u0000')
            val content = if (nullIndex >= 0) data.substring(0, nullIndex) else data

            val lines = content.split("\n")
            if (lines.isEmpty()) {
                return StompMessage(StompCommand.UNKNOWN, emptyList(), data)
            }

            val commandString = lines[0].replace("\r", "").trim()
            val command = StompCommand.fromString(commandString)
            val headers = mutableListOf<StompHeader>()
            var bodyStartIndex = -1

            for (i in 1 until lines.size) {
                val line = lines[i].replace("\r", "")
                if (line.isEmpty()) {
                    bodyStartIndex = i + 1
                    break
                }
                val colonIndex = line.indexOf(':')
                if (colonIndex != -1) {
                    val key = line.substring(0, colonIndex).trim()
                    val value = line.substring(colonIndex + 1).trim()
                    headers.add(StompHeader(key, value))
                }
            }

            val payload = if (bodyStartIndex in lines.indices) {
                lines.subList(bodyStartIndex, lines.size).joinToString("\n")
            } else {
                null
            }

            return StompMessage(command, headers, payload)
        }
    }
}
