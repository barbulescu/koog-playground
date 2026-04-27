import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * RefactoringAgent owns the REFACTOR phase of each TDD cycle.
 *
 * Responsibilities:
 * - Clean up the green implementation without changing behaviour
 * - Improve naming, extract functions, remove duplication
 * - Apply idiomatic Kotlin patterns (expression bodies, scope functions, data classes, etc.)
 * - All tests must still pass — no new logic is added
 */
class RefactoringAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val maxIterations: Int = 3,
) {

    private val systemPrompt = """
        You are an expert Kotlin developer performing the REFACTOR phase of TDD.

        All tests are currently green. Your job is to improve code quality without
        altering observable behaviour.

        Refactoring goals (apply as appropriate):
        1. Clarify intent through better naming (classes, functions, variables).
        2. Extract private helper functions to reduce method length.
        3. Remove duplication (DRY).
        4. Apply idiomatic Kotlin: expression bodies, `let`/`run`/`apply`, data classes,
           sealed classes, extension functions where they improve readability.
        5. Improve error handling — use `Result` or sealed outcomes instead of raw exceptions.
        6. Ensure single-responsibility: one reason to change per class.

        Rules:
        - Do NOT add new behaviour or new tests.
        - Do NOT remove or rename public API that the tests rely on.
        - Tests must still compile and pass after your changes.

        Output format (strict JSON):
        {
          "refactoredCode": "<full refactored Kotlin source>",
          "changesSummary": "<bullet list of changes made>",
          "testsStillPassing": true
        }
    """.trimIndent()

    suspend fun refactor(
        cycleId: Int,
        description: String,
        testCode: String,
        implementationCode: String,
        language: String
    ): RefactorPhaseResult {
        log.info { "RefactoringAgent: REFACTOR phase for cycle $cycleId — $description" }

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

            Test code (must still pass after refactoring):
            ```kotlin
            $testCode
            ```

            Current green implementation to refactor:
            ```kotlin
            $implementationCode
            ```

            Refactor the implementation.
        """.trimIndent()

        val rawResponse = agent.run(userPrompt)

        return parseRefactorPhaseResult(cycleId, rawResponse)
    }

    private fun parseRefactorPhaseResult(cycleId: Int, raw: String): RefactorPhaseResult {
        val refactoredCode = extractJsonField(raw, "refactoredCode")
        val changesSummary = extractJsonField(raw, "changesSummary")
        val testsStillPassing = raw.contains(""""testsStillPassing": true""") ||
            raw.contains(""""testsStillPassing":true""")

        return RefactorPhaseResult(
            cycleId = cycleId,
            refactoredCode = refactoredCode,
            changesSummary = changesSummary,
            testsStillPassing = testsStillPassing,
        )
    }

    private fun extractJsonField(json: String, field: String): String =
        Regex(""""$field"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?: error("Field '$field' not found in RefactoringAgent response")
}
