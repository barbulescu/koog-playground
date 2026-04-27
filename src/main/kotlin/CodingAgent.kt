import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * CodingAgent owns the GREEN phase of each TDD cycle.
 *
 * Responsibilities:
 * - Write the MINIMAL production code that makes the failing test pass
 * - Resist the urge to add anything not tested yet (YAGNI)
 * - It is acceptable (encouraged!) for the code to be ugly at this stage
 */
class CodingAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val maxIterations: Int = 3,
) {

    private val systemPrompt = """
        You are a disciplined TDD practitioner in the GREEN phase.

        Your job: write the MINIMUM Kotlin production code that makes the given test pass.

        Rules:
        1. Do NOT write more than the test demands — no extra methods, no future-proofing.
        2. Hardcoding return values is acceptable if that is all the test requires.
        3. The code must compile against standard JVM libraries only.
        4. Return ONLY valid Kotlin source code, no markdown fences.
        5. Assume the test file is in the same package.

        Output format (strict JSON):
        {
          "implementationCode": "<full Kotlin source>",
          "implementationFileName": "<ClassName>.kt",
          "testsPassing": true
        }
    """.trimIndent()

    suspend fun writeImplementation(
        cycleId: Int,
        description: String,
        testCode: String,
        language: String,
    ): GreenPhaseResult {
        log.info { "CodingAgent: GREEN phase for cycle $cycleId — $description" }

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

            Failing test to make pass:
            ```kotlin
            $testCode
            ```

            Write the minimal implementation.
        """.trimIndent()

        val rawResponse = agent.run(userPrompt)

        return parseGreenPhaseResult(cycleId, rawResponse)
    }

    private fun parseGreenPhaseResult(cycleId: Int, raw: String): GreenPhaseResult {
        val implementationCode = extractJsonField(raw, "implementationCode")
        val implementationFileName = extractJsonField(raw, "implementationFileName")
        val testsPassing = raw.contains(""""testsPassing": true""") ||
            raw.contains(""""testsPassing":true""")

        return GreenPhaseResult(
            cycleId = cycleId,
            implementationCode = implementationCode,
            implementationFileName = implementationFileName,
            testsPassing = testsPassing,
        )
    }

    private fun extractJsonField(json: String, field: String): String =
        Regex(""""$field"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?: error("Field '$field' not found in CodingAgent response")
}
