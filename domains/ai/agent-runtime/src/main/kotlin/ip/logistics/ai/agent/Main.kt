package ip.logistics.ai.agent

fun main(args: Array<String>) {
    val apiKey = System.getenv("OPENAI_API_KEY")
        ?: error("Missing OPENAI_API_KEY")

    val llm = LlmClient(apiKey)

    val response = llm.ask(
        "Explain in one sentence what an AI agent is."
    )

    println("Response:" + response)
}
