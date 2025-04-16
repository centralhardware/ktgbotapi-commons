[![](https://jitpack.io/v/centralhardware/telegram-bot-commons.svg)](https://jitpack.io/#centralhardware/telegram-bot-commons)

# Telegram Bot Commons

A utility library for Kotlin Telegram bots that provides common functionality for logging, health checks, configuration, and tracing.

## Features

- **Easy Bot Setup**: Simplified setup for Telegram bots with long polling
- **Health Check Endpoint**: Built-in health check endpoint on port 81
- **Logging**: Configurable logging with KSLog
- **Tracing**: Event tracing with ClickHouse integration
- **Configuration**: Simple application configuration management

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.centralhardware:telegram-bot-commons:latest-version")
}
```

### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.centralhardware:telegram-bot-commons:latest-version'
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

The library automatically starts a health check endpoint on port 81. You can check the health of your bot by making a GET request to `/health`. The endpoint returns:

- `200 OK` if all registered bots are functioning properly
- `400 Bad Request` if any bot is not responding

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
