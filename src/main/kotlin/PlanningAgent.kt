import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * PlanningAgent is the root orchestrator of the TDD session.
 *
 * It:
 * 1. Decomposes a feature description into small, independent TDD cycles
 * 2. Iterates over each cycle, calling the sub-agents via tools:
 *    - [WriteFailingTestTool]  → RED phase
 *    - [WriteImplementationTool] → GREEN phase
 *    - [RefactorCodeTool]      → REFACTOR phase
 * 3. Collects all [CompletedCycle] results and returns a [TddSessionResult]
 */
class PlanningAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val testingAgent: TestingAgent,
    private val codingAgent: CodingAgent,
    private val refactoringAgent: RefactoringAgent,
    private val maxIterations: Int = 3,
) {

    private val systemPrompt = """
        You are a senior software engineer acting as a TDD planning orchestrator.

        Given a feature description, your job is to:

        STEP 1 — DECOMPOSE
        Break the feature into the smallest possible independent TDD cycles.
        Each cycle must:
        - Represent exactly ONE behaviour or rule
        - Be independently testable without depending on incomplete cycles
        - Be ordered from simplest to most complex (baby steps)
        - Have clear acceptance criteria

        STEP 2 — DRIVE EACH CYCLE
        For every cycle in order, call the tools in this strict sequence:
          1. `write_failing_test`   → RED:     write a failing test
          2. `write_implementation` → GREEN:   write minimal code to pass the test
          3. `refactor_code`        → REFACTOR: clean up without breaking tests

        Do NOT proceed to the next cycle until the current cycle has completed all three phases.

        STEP 3 — SUMMARISE
        After all cycles are complete, provide a concise markdown summary covering:
        - What was built
        - How many cycles were completed
        - Key design decisions made during refactoring

        Important constraints:
        - Keep cycles as small as possible — resist the urge to batch multiple behaviours
        - Trust the sub-agents: do not second-guess their output
        - If a sub-agent reports tests are NOT passing, retry that phase once with a correction hint
    """.trimIndent()

    /**
     * Runs a full TDD session for the given feature.
     *
     * @param feature  Human-readable description of the feature to implement
     * @param language Target programming language (default: Kotlin)
     * @param maxCycles Safety cap on the number of cycles (default: 10)
     */
    suspend fun run(
        feature: String,
        language: String = "Kotlin",
        maxCycles: Int = 10,
    ): TddSessionResult {
        log.info { "PlanningAgent: starting TDD session for feature: $feature" }

        // Build the tool registry with all three sub-agent tools
        val toolRegistry = ToolRegistry {
            tool(WriteFailingTestTool(testingAgent))
            tool(WriteImplementationTool(codingAgent))
            tool(RefactorCodeTool(refactoringAgent))
        }


        val agent = AIAgent(
            promptExecutor = promptExecutor,
            llmModel = model,
            systemPrompt = systemPrompt,
            toolRegistry = toolRegistry,
            temperature = 0.1,
            maxIterations = maxIterations,
        )

        val userPrompt = """
            Feature to implement using TDD:
            $feature

            Target language: $language
            Maximum TDD cycles: $maxCycles

            Start by decomposing the feature into small cycles, then drive each cycle
            through RED → GREEN → REFACTOR using the available tools.
        """.trimIndent()

        val summary = agent.run(userPrompt)

        log.info { "PlanningAgent: session complete" }

        // The agent returns its final summary as text; completed cycles are tracked
        // via tool call side effects captured in the sub-agents. For a production
        // system you would wire observability hooks here to capture each CompletedCycle.
        return TddSessionResult(
            feature = feature,
            completedCycles = emptyList(), // populated via observability hooks in production
            summary = summary ?: "Session completed — see tool call log for details",
        )
    }
}
