package ip.logistics.ai.agent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class LlmClient(private val apiKey: String) {
    private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
    private val objectMapper = jacksonObjectMapper()

    fun ask(prompt: String): String {
        val body = objectMapper.writeValueAsString(
            mapOf(
                "model" to "gpt-6-luna",
                "input" to prompt,
                "reasoning" to mapOf(
                    "effort" to "low"
                )
            )
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/responses"))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        )

        check(response.statusCode() in 200..299) {
            "OpenAI API error: HTTP ${response.statusCode()} - ${response.body()}"
        }

        val rootJsonNode = objectMapper.readTree(response.body())

        println("Usage:" + rootJsonNode.path("usage").toPrettyString())

        return rootJsonNode.path("output")
            .filter { it.path("type").asText() == "message" }
            .flatMap { it.path("content").toList() }
            .filter { it.path("type").asText() == "output_text" }
            .joinToString("\n") { it.path("text").asText() }
    }

}