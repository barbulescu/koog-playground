import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.file.tools.ReadFileTool
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels.Chat.GPT4o
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.rag.base.files.JVMFileSystemProvider
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val apiKey = requireNotNull(System.getenv("OPENAI_API_KEY")) { "OPENAI_API_KEY is required." }
    val azureOpenAIClientSettings = OpenAIClientSettings(
        baseUrl = "https://isaca-mfmlpbix-northcentralus.openai.azure.com/openai/deployments/gpt-4o-mini-marius/",
        chatCompletionsPath = "chat/completions?api-version=2025-01-01-preview"
    )

    val agent = AIAgent(
        executor = SingleLLMPromptExecutor(OpenAILLMClient(apiKey = apiKey, settings = azureOpenAIClientSettings)),
        systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
        llmModel = GPT4o,
        toolRegistry = ToolRegistry {
            tool(ReadFileTool(JVMFileSystemProvider.ReadOnly))
            tool(SayToUser)
        })


    val answers = agent.run(
        """
            Read only /home/marius/work/github/koog-playground/data/questions.txt file and answer the questions in it.
            Each line in the file is a question.
            Say to user answer to these questions.
            """
    )
    println("""
        |---
        |$answers
        |---
    """.trimMargin())
}


