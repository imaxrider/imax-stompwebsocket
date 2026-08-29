# imax-stomp-websocket Sample App (`:sample`)

This module is an interactive Android application that demonstrates how to integrate and use the **`imax-stompwebsocket`** library in a real Android project.

---

## Features Demonstrated

- :: **Connection & Disconnection**: Initialize and connect to any STOMP broker over WebSocket using `Stomp.over(url)`.
- :: **Reactive Lifecycle Monitoring**: Collect connection lifecycle events (`OPENED`, `CLOSED`, `ERROR`, `FAILED_SERVER_HEARTBEAT`) using `stompClient.lifecycleFlow`.
- :: **Connection State Tracking**: Observe real-time connection state using `stompClient.connectionState`.
- :: **Topic Subscriptions**: Subscribe to STOMP destinations dynamically using Kotlin `Flow` (`stompClient.topic(destination)`).
- :: **Sending Messages**: Send STOMP messages asynchronously using `stompClient.send(destination, payload)`.
- :: **Live Console Output**: Scrollable real-time log displaying all WebSocket activity.

---

## Requirements

- **Android Studio**: Ladybug / 2024.2.1 or newer
- **Android Device / Emulator**: Android 5.0 (API level 21) or higher
- **JDK**: Java 17

---

## How to Run in Android Studio

1. Open the repository root folder in **Android Studio**.
2. Wait for Gradle Sync to complete.
3. In the top toolbar run configurations dropdown, select **`sample`**.
4. Select an Android Emulator or connected physical device.
5. Click **Run** (or press `Shift + F10`).

---

## How to Test Connection

1. Enter your WebSocket server URI (e.g. `ws://10.0.2.2:8080/ws` for local server on Android Emulator).
2. Click **Connect**.
3. Once connected, enter a topic destination (e.g. `/topic/messages`) and click **Subscribe**.
4. Enter a destination (e.g. `/app/chat`) and payload, then click **Send**.
5. Observe incoming messages and lifecycle logs in the **Console Log** section.
