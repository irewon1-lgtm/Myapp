package com.futuretech.poweruser.ai

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class UrlConnectionAiHttpTransport : AiHttpTransport {
    override suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int
    ): AiHttpResult = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            doInput = true
            doOutput = true
            useCaches = false
            instanceFollowRedirects = false
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }
        try {
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.use { input -> readLimited(input, 256 * 1024) }.orEmpty()
            AiHttpResult(status, responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun readLimited(input: java.io.InputStream, maxBytes: Int): String {
        val source = BufferedInputStream(input)
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = source.read(buffer)
            if (read <= 0) break
            val allowed = minOf(read, maxBytes - total)
            if (allowed > 0) out.write(buffer, 0, allowed)
            total += read
            if (total >= maxBytes) break
        }
        return out.toString(Charsets.UTF_8.name())
    }
}

class OpenAiTutorGateway(
    private val transport: AiHttpTransport = UrlConnectionAiHttpTransport(),
    private val endpoint: String = "https://api.openai.com/v1/responses",
    private val model: String = DEFAULT_MODEL,
    private val connectTimeoutMs: Int = 12_000,
    private val readTimeoutMs: Int = 35_000
) : AiTutorGateway {

    override suspend fun ask(apiKey: String?, request: AiTutorRequest): AiTutorResponse {
        val decision = AiTutorPolicy.authorize(request)
        if (!decision.allowed) {
            return AiTutorResponse(
                success = false,
                failureKind = decision.failureKind,
                userMessage = decision.message,
                model = model
            )
        }

        val cleanKey = apiKey?.trim().orEmpty()
        if (cleanKey.isBlank()) {
            return AiTutorResponse(
                success = false,
                failureKind = AiFailureKind.MISSING_KEY,
                userMessage = "실제 AI 연결용 API Key가 없습니다. AI 연결 설정에서 Key를 입력하거나 로컬 힌트를 사용하세요.",
                model = model
            )
        }

        val payload = JsonObject().apply {
            addProperty("model", model)
            addProperty("instructions", AiTutorPolicy.buildInstructions(request))
            addProperty("input", AiTutorPolicy.buildInput(request))
            addProperty("max_output_tokens", if (request.mode == AiLearningMode.HINT) 350 else 900)
            addProperty("store", false)
        }.toString()

        return try {
            val http = transport.postJson(
                endpoint,
                mapOf(
                    "Authorization" to "Bearer $cleanKey",
                    "Content-Type" to "application/json",
                    "Accept" to "application/json"
                ),
                payload,
                connectTimeoutMs,
                readTimeoutMs
            )
            if (http.statusCode !in 200..299) {
                return mapHttpFailure(http.statusCode, http.body)
            }

            val text = extractOutputText(http.body)
                ?: return AiTutorResponse(
                    success = false,
                    failureKind = AiFailureKind.BAD_RESPONSE,
                    userMessage = "AI 응답 형식을 읽지 못했습니다. 잠시 후 다시 시도하세요.",
                    httpStatus = http.statusCode,
                    model = model
                )

            val guard = AiTutorPolicy.guardResponse(request, text)
            if (!guard.allowed) {
                AiTutorResponse(
                    success = false,
                    failureKind = guard.failureKind,
                    userMessage = guard.message,
                    httpStatus = http.statusCode,
                    model = model
                )
            } else {
                AiTutorResponse(
                    success = true,
                    text = text.trim(),
                    httpStatus = http.statusCode,
                    model = model
                )
            }
        } catch (_: SocketTimeoutException) {
            failure(AiFailureKind.TIMEOUT, "AI 응답 시간이 초과되었습니다. 인터넷 상태를 확인하고 다시 시도하세요.")
        } catch (_: UnknownHostException) {
            failure(AiFailureKind.OFFLINE, "인터넷에 연결되지 않았습니다. 기본 학습은 계속할 수 있고 AI 기능만 일시 중지됩니다.")
        } catch (_: ConnectException) {
            failure(AiFailureKind.OFFLINE, "AI 서버에 연결할 수 없습니다. 기본 학습은 계속할 수 있습니다.")
        } catch (_: SSLException) {
            failure(AiFailureKind.OFFLINE, "안전한 AI 연결을 만들지 못했습니다. 네트워크를 바꾼 뒤 다시 시도하세요.")
        } catch (_: Exception) {
            failure(AiFailureKind.UNKNOWN, "AI 요청 처리 중 오류가 발생했습니다. Key는 로그에 기록하지 않았습니다.")
        }
    }

    internal fun extractOutputText(body: String): String? {
        if (body.isBlank()) return null
        return try {
            val root = JsonParser.parseString(body).takeIf { it.isJsonObject }?.asJsonObject ?: return null
            val convenience = root.get("output_text")
                ?.takeIf { it.isJsonPrimitive }
                ?.asString
                ?.trim()
                .orEmpty()
            if (convenience.isNotEmpty()) return convenience

            val output = root.getAsJsonArray("output") ?: return null
            val pieces = mutableListOf<String>()
            output.forEach { itemElement ->
                val item = itemElement.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                val content = item.getAsJsonArray("content") ?: return@forEach
                content.forEach { partElement ->
                    val part = partElement.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                    val type = part.get("type")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                    if (type == "output_text" || type == "text") {
                        val text = part.get("text")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
                        if (text.isNotEmpty()) pieces += text
                    }
                }
            }
            pieces.takeIf { it.isNotEmpty() }?.joinToString("\n")
        } catch (_: Exception) {
            null
        }
    }

    private fun mapHttpFailure(status: Int, body: String): AiTutorResponse {
        val providerMessage = safeErrorMessage(body)
        return when (status) {
            400 -> AiTutorResponse(false, failureKind = AiFailureKind.BAD_REQUEST, userMessage = "AI 요청 형식이 거절되었습니다.${providerMessage.toSuffix()}", httpStatus = status, model = model)
            401, 403 -> AiTutorResponse(false, failureKind = AiFailureKind.AUTH, userMessage = "API Key가 유효하지 않거나 권한이 없습니다. AI 연결 설정을 확인하세요.", httpStatus = status, model = model)
            408 -> AiTutorResponse(false, failureKind = AiFailureKind.TIMEOUT, userMessage = "AI 서버 응답 시간이 초과되었습니다.", httpStatus = status, model = model)
            429 -> AiTutorResponse(false, failureKind = AiFailureKind.RATE_LIMIT, userMessage = "AI 사용 한도 또는 Rate Limit에 도달했습니다. 잠시 후 다시 시도하세요.${providerMessage.toSuffix()}", httpStatus = status, model = model)
            in 500..599 -> AiTutorResponse(false, failureKind = AiFailureKind.SERVER, userMessage = "AI 서비스가 일시적으로 불안정합니다. 기본 학습은 계속할 수 있습니다.", httpStatus = status, model = model)
            else -> AiTutorResponse(false, failureKind = AiFailureKind.UNKNOWN, userMessage = "AI 요청이 실패했습니다. HTTP $status", httpStatus = status, model = model)
        }
    }

    private fun safeErrorMessage(body: String): String? {
        return try {
            val root = JsonParser.parseString(body).takeIf { it.isJsonObject }?.asJsonObject
                ?: return null
            root.getAsJsonObject("error")
                ?.get("message")
                ?.takeIf { it.isJsonPrimitive }
                ?.asString
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.take(180)
        } catch (_: Exception) {
            null
        }
    }

    private fun String?.toSuffix(): String = if (this.isNullOrBlank()) "" else " ($this)"

    private fun failure(kind: AiFailureKind, message: String) = AiTutorResponse(
        success = false,
        failureKind = kind,
        userMessage = message,
        model = model
    )

    companion object {
        const val DEFAULT_MODEL = "gpt-5.6-luna"
    }
}
