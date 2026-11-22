import com.sun.net.httpserver.HttpServer
import dev.inmo.tgbotapi.LogUpdateId
import dev.inmo.tgbotapi.listenRelayWebhooks
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviour
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.updateshandlers.FlowsUpdatesFilter
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URI
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.coroutines.Job
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class LogUpdateIdTest {
    private val sendMessageOk = """{"ok":true,"result":{"message_id":2,"date":1,"chat":{"id":1,"type":"private","first_name":"u"},"text":"ok"}}"""

    private fun update(id: Long) = """{"update_id":$id,"message":{"message_id":1,"date":1,
        "chat":{"id":1,"type":"private","first_name":"u"},"from":{"id":1,"is_bot":false,"first_name":"u"},"text":"hi"}}"""

    @Test
    fun `updates posted by the relay get handlers whose calls carry the log id, nested launches too`(): Unit = runBlocking {
        // Fake Bot API: records the header of every call.
        val seen = LinkedBlockingQueue<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { ex ->
                seen += ex.requestURI.path.substringAfterLast('/') + "=" + (ex.requestHeaders.getFirst(LogUpdateId.HEADER) ?: "none")
                ex.requestBody.readBytes()
                val body = sendMessageOk.toByteArray()
                ex.sendResponseHeaders(200, body.size.toLong()); ex.responseBody.write(body); ex.close()
            }
            start()
        }
        val bot = telegramBot("1:x", apiUrl = "http://127.0.0.1:${server.address.port}") {
            client = HttpClient(OkHttp) { install(LogUpdateId.plugin) }
        }
        val filter = FlowsUpdatesFilter()
        val scope = CoroutineScope(Dispatchers.IO)
        val ctx = bot.buildBehaviour(filter, scope, subcontextInitialAction = LogUpdateId.subcontextAction) {
            onText {
                reply(it, "a")
                launch { sendMessage(it.chat, "b") }.join()
            }
        }
        // Updates arrive the way the relay sends them: through the webhook route.
        val port = ServerSocket(0).use { it.localPort }
        val webhooks = listenRelayWebhooks(ctx, filter.asUpdateReceiver, port)
        fun post(updateId: Long, logId: Long? = null): Int {
            val body = update(updateId)
            val c = URI("http://127.0.0.1:$port/telegram/update").toURL().openConnection() as HttpURLConnection
            c.requestMethod = "POST"; c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            logId?.let {
                c.setRequestProperty(LogUpdateId.HEADER, it.toString())
                c.setRequestProperty(LogUpdateId.UPDATE_HEADER, updateId.toString())
            }
            c.outputStream.use { it.write(body.toByteArray()) }
            return c.responseCode
        }

        assertEquals(200, post(10, logId = 777))
        val first = listOf(seen.poll(10, TimeUnit.SECONDS), seen.poll(10, TimeUnit.SECONDS))
        assertEquals(listOf("sendMessage=777", "sendMessage=777"), first)

        // An update the relay gave no id: no header.
        assertEquals(200, post(11))
        assertEquals("sendMessage=none", seen.poll(10, TimeUnit.SECONDS))

        // A call outside any handler: no header.
        withTimeout(10_000) { bot.sendMessage(ChatId(RawChatId(1)), "c") }
        assertEquals("sendMessage=none", seen.poll(10, TimeUnit.SECONDS))

        server.stop(0)
        webhooks.stop(0, 0)
        ctx.coroutineContext[Job]?.cancel()
    }
}
