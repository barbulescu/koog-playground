import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

private val log = KotlinLogging.logger {}

fun main() = runBlocking {
    val promptExecutor = simpleOllamaAIExecutor(baseUrl = "http://localhost:11434/v1")
    val ollamaQwen3 = LLModel(
        id = "qwen3:8b",
        capabilities = listOf(LLMCapability.Tools),
        provider = LLMProvider.Ollama,
    )

    // Wire sub-agents
    val testingAgent = TestingAgent(
        promptExecutor = promptExecutor,
        model = ollamaQwen3
    )
    val codingAgent = CodingAgent(
        promptExecutor = promptExecutor,
        model = ollamaQwen3
    )
    val refactoringAgent = RefactoringAgent(
        promptExecutor = promptExecutor,
        model = ollamaQwen3
    )

    // Wire orchestrator
    val planningAgent = PlanningAgent(
        promptExecutor = promptExecutor,
        model = ollamaQwen3,
        testingAgent = testingAgent,
        codingAgent = codingAgent,
        refactoringAgent = refactoringAgent,
    )

    val feature = """
        Implement a simple bank account with the following rules:
        - An account can be opened with an initial balance of zero
        - Money can be deposited; the balance increases accordingly
        - Money can be withdrawn; the balance decreases accordingly
        - A withdrawal that would make the balance negative must be rejected
        - The current balance can be queried at any time
    """.trimIndent()

    val result = planningAgent.run(
        feature = feature,
        language = "Kotlin",
        maxCycles = 5,
    )

    log.info { "\n\n=== TDD Session Summary ===\n${result.summary}" }
}
