import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.rag.base.files.JVMFileSystemProvider.ReadOnly
import kotlinx.coroutines.runBlocking

val ollamaQwen3 = LLModel(
    id = "qwen3:8b",
    capabilities = listOf(LLMCapability.Tools),
    provider = LLMProvider.Ollama,
)

fun main(): Unit = runBlocking {
    val agent = AIAgent(
        promptExecutor = simpleOllamaAIExecutor(baseUrl = "http://localhost:11434/v1"),
        systemPrompt = "You are a Kotlin senior developer which values small incremental work and TDD.",
        llmModel = ollamaQwen3,
        toolRegistry = ToolRegistry {
            tool(ReadFileTool(ReadOnly))
            tool(SayToUser)
        })


    val answers = agent.run(
        """
            You need to plan fizz-buzz algorithm implementation. Don't implement anything yet.
            Write the plan as input for an LLM to implement the algorithm.
            """
    )
    println("""
        |---
        |$answers
        |---
    """.trimMargin())
}


