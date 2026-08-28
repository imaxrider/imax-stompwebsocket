# imax-stompwebsocket `v2.1.0`

An Android STOMP WebSocket client library, originally written in Java, now fully migrated to **Kotlin** and optimized by **IMAX** to use **Kotlin Coroutines & Flows**.

This library provides a simple and reactive interface to communicate with STOMP brokers over WebSockets using OkHttp directly as the WebSocket engine.

## Features

- **100% Kotlin**: Fully rewritten in idiomatic Kotlin with native null-safety and JVM compatibility.
- **RxJava Removed**: Migrated completely to Kotlin Coroutines (`Job`, `suspend` functions) and Flows (`Flow`, `SharedFlow`, `StateFlow`).
- **OkHttp Engine**: Directly embeds **OkHttp 5.0.0-alpha.14** as the WebSocket engine, removing deprecated alternatives (like Java-WebSocket) to minimize dependencies.
- **Data Classes**: Modeling STOMP messages and headers using clean Kotlin `data class` structures and type-safe `StompCommand` enum.
- **STOMP 1.1 & 1.2 Support**: Fully compliant with STOMP specifications.
- **Flexible Path Matching**: Supports exact destination matching (`SimplePathMatcher`), subscription-based matching (`SubscriptionPathMatcher`), and RabbitMQ-style wildcards (`RabbitPathMatcher`).
- **Heartbeat & Keep-Alive**: Configurable heartbeat check-in powered by Coroutine-based delay timers.

## Installation

### 1. Add JitPack repository
Add JitPack repository to your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add dependency
Add the library dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.imaxrider:imax-stompwebsocket:v2.1.0")
    // Or as a local sub-module:
    // implementation(project(":imax-stompwebsocket"))
}
```

## Requirements

- **Gradle**: `9.5.0` or higher (configured in `gradle-wrapper.properties`)
- **Android Gradle Plugin (AGP)**: `9.3.0` or higher (configured in `build.gradle.kts`)
- **JDK**: `17` or higher (required for AGP 9.x and Java 17 target compatibility)

### Kotlin Built-in Compilation
This library is configured using AGP 9.x's built-in Kotlin compilation. If you are importing this project as a sub-module, ensure your root project runs on a compatible Gradle/AGP environment.

## Usage

### 1. Initialize STOMP Client

You can create a STOMP client by passing your server URI and optional HTTP headers / custom OkHttpClient instance.

```kotlin
import com.imax.stompwebsocket.Stomp
import okhttp3.OkHttpClient

val okHttpClient = OkHttpClient.Builder().build()
val stompClient = Stomp.over(
    "ws://your-server-address/websocket",
    mapOf("Authorization" to "Bearer token123"), // Headers map or null
    okHttpClient // Custom client or null
)
```

---

### 2. Configure Heartbeat (Optional)

```kotlin
stompClient
    .withClientHeartbeat(10000) // Client heartbeat interval in ms
    .withServerHeartbeat(10000) // Server heartbeat interval in ms
```

---

### 3. Connection & Lifecycle Events

Collect connection lifecycle events using standard Kotlin Flow:

```kotlin
import com.imax.stompwebsocket.dto.LifecycleEvent
import kotlinx.coroutines.launch

lifecycleScope.launch {
    stompClient.lifecycleFlow.collect { lifecycleEvent ->
        when (lifecycleEvent.type) {
            LifecycleEvent.Type.OPENED -> {
                Log.d("STOMP", "Connection opened")
            }
            LifecycleEvent.Type.CLOSED -> {
                Log.d("STOMP", "Connection closed")
            }
            LifecycleEvent.Type.ERROR -> {
                Log.e("STOMP", "Error occurred", lifecycleEvent.exception)
            }
            LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> {
                Log.e("STOMP", "Server heartbeat failed")
            }
        }
    }
}

// Connect to the broker
stompClient.connect()
```

To check connection state, collect `connectionState`:
```kotlin
lifecycleScope.launch {
    stompClient.connectionState.collect { isConnected ->
        Log.d("STOMP", "Is connected: $isConnected")
    }
}
```

---

### 4. Subscribe to a Topic

Subscribe and collect messages reactively using Kotlin Flows:

```kotlin
lifecycleScope.launch {
    stompClient.topic("/topic/messages")
        .collect { stompMessage ->
            Log.d("STOMP", "Received: ${stompMessage.payload}")
        }
}
```

*Note: The STOMP `SUBSCRIBE` frame is automatically sent when the first collector starts collecting the topic Flow, and the `UNSUBSCRIBE` frame is sent when the collection finishes or is cancelled.*

---

### 5. Send a Message

Sending messages is a suspendable action that waits automatically until a connection is established:

```kotlin
lifecycleScope.launch {
    try {
        stompClient.send("/app/chat", "Hello, World!")
        Log.d("STOMP", "Message sent successfully")
    } catch (e: Exception) {
        Log.e("STOMP", "Failed to send message", e)
    }
}
```

---

### 6. Disconnect

Disconnect from the STOMP server:

```kotlin
stompClient.disconnect()
```

---

## License

Modified by IMAX. Distributed under the MIT License.
