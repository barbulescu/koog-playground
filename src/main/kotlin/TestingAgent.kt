import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

private val systemPrompt = """
        You are an expert TDD practitioner specialising in Kotlin and JUnit 5.

        Your sole responsibility is the RED phase: write ONE failing test per cycle.

        Rules:
        1. Use JUnit 5 + AssertJ (or kotlin.test if simpler).
        2. The test class name must end with `Test`.
        3. Method names follow: `should_<expected_behaviour>_when_<condition>`.
        4. The test MUST fail because production code does not yet exist — do NOT write the implementation.
        5. Keep tests focused: one logical assertion per test.
        6. Add a one-line comment explaining WHY this test matters.
        7. Return ONLY valid Kotlin source code, no markdown fences.

        Output format (strict JSON):
        {
          "testCode": "<full Kotlin source>",
          "testFileName": "<ClassName>Test.kt",
          "failureReason": "<why this test currently fails>"
        }
    """.trimIndent()


/**
 * TestingAgent owns the RED phase of each TDD cycle.
 *
 * Responsibilities:
 * - Write a focused, expressive failing test that documents intent
 * - Ensure the test would compile but fail with "class/method not found" or an assertion error
 * - Follow naming conventions: `should_<behaviour>_when_<condition>`
 */
class TestingAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val maxIterations: Int = 3,
) {

    suspend fun writeFailingTest(
        cycleId: Int,
        description: String,
        acceptanceCriteria: String,
        language: String,
    ): RedPhaseResult {
        log.info { "TestingAgent: RED phase for cycle $cycleId — $description" }

        val agent = AIAgent(
            promptExecutor = promptExecutor,
            llmModel = model,
            systemPrompt = systemPrompt,
            temperature = 0.1,
            maxIterations = maxIterations,
        )

        val userPrompt = """
            Cycle ID: $cycleId
            Language: $language
            Description: $description
            Acceptance criteria: $acceptanceCriteria

            Write the failing test for this cycle.
        """.trimIndent()

        val rawResponse = agent.run(userPrompt)

        return parseRedPhaseResult(cycleId, rawResponse)
    }

    private fun parseRedPhaseResult(cycleId: Int, raw: String): RedPhaseResult {
        // Naive JSON extraction — in production swap for kotlinx.serialization
        val testCode = extractJsonField(raw, "testCode")
        val testFileName = extractJsonField(raw, "testFileName")
        val failureReason = extractJsonField(raw, "failureReason")

        return RedPhaseResult(
            cycleId = cycleId,
            testCode = testCode,
            testFileName = testFileName,
            failureReason = failureReason,
        )
    }

    private fun extractJsonField(json: String, field: String): String =
        Regex(""""$field"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?: error("Field '$field' not found in TestingAgent response")
}
