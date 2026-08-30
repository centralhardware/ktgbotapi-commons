[![](https://jitpack.io/v/centralhardware/ktgbotapi-commons.svg)](https://jitpack.io/#centralhardware/ktgbotapi-commons)

# Telegram Bot Commons

A multi-module utility library for Kotlin Telegram bots ([ktgbotapi](https://github.com/InsanusMokrassar/ktgbotapi)) that provides common functionality for logging, access control, conversations, configuration, and tracing.

## Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| `ktgbotapi-commons-core` | `commons` | Bot setup helpers (long polling, logging, config) |
| `ktgbotapi-conversation` | `conversation` | Multi-step conversation / dialog state machine |
| `ktgbotapi-stdout-logging-middleware` | `stdout-logging-middleware` | Logs incoming/outgoing requests to stdout |
| `ktgbotapi-clickhouse-logging-middleware` | `clickhouse-logging-middleware` | Logs bot requests to ClickHouse |
| `ktgbotapi-restrict-access-middleware` | `restrict-access-middleware` | Restricts bot access to allowed users |

## Installation

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // pull the core library (transitively brings the middlewares)
    implementation("com.github.centralhardware.ktgbotapi-commons:commons:latest-version")

    // or add individual modules
    implementation("com.github.centralhardware.ktgbotapi-commons:conversation:latest-version")
    implementation("com.github.centralhardware.ktgbotapi-commons:clickhouse-logging-middleware:latest-version")
}
```

## Usage

### Basic Bot Setup

```kotlin
suspend fun main() {
    // Initialize application configuration
    AppConfig.init("myBotApp")

    // Start the bot with long polling
    longPolling {
        // Handle text messages
        onText {
            // Your message handling logic here
        }
    }.second.join() // Join the coroutine job to keep the application running
}
```

### Tracing Events

```kotlin
// Save a trace event with parameters
Trace.save("message_received", mapOf(
    "chat_id" to message.chat.id.toString(),
    "message_text" to message.text
))
```

## Configuration

The library uses environment variables for configuration:

| Variable | Description | Default |
|----------|-------------|---------|
| `BOT_TOKEN` | Telegram Bot API token | Required |
| `CLICKHOUSE_URL` | ClickHouse database URL for tracing | Required for tracing |
| `DEBUG` | Enable debug logging | `false` |

## Health Check

Every bot started through `longPolling` registers itself with a small built-in HTTP server that
exposes `GET /health` on port `8081`. A probe pings the Bot API with `getMe` for each registered
bot:

- `200 OK` — every registered bot answered
- `503 Service Unavailable` — a bot did not answer (bad token, revoked bot, no connectivity), the
  body lists the failing bots and the reason; also returned when no bot has been registered yet

```
$ curl -i localhost:8081/health
HTTP/1.1 200 OK
OK
```

Configuration:

| Variable | Description | Default |
|----------|-------------|---------|
| `HEALTHCHECK_ENABLED` | Start the endpoint at all | `true` |
| `HEALTHCHECK_PORT` | Port to listen on | `8081` |
| `HEALTHCHECK_TIMEOUT_MS` | Per-bot `getMe` timeout | `5000` |

### Docker

The Jib Gradle plugin cannot emit a `HEALTHCHECK` instruction, so the images add it in a thin layer
on top of the Jib output (see `Dockerfile` and `.github/workflows/jib.yml` in the bots that use this
library):

```dockerfile
ARG BASE_IMAGE
FROM ${BASE_IMAGE}
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD ["/bin/bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/${HEALTHCHECK_PORT:-8081} && printf 'GET /health HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3 && head -n 1 <&3 | grep -q ' 200 '"]
```

## License

This project is licensed under the [MIT License](LICENSE).

```
MIT License

Copyright (c) 2024 Alexey Fedechkin 

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
