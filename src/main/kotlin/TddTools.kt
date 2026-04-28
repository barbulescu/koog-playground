import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Input / Output shapes (serializable so Koog can pass them through the LLM)
// ---------------------------------------------------------------------------

@Serializable
data class WriteFailingTestInput(
    val cycleId: Int,
    val description: String,
    val acceptanceCriteria: String,
    val language: String = "Kotlin",
)

@Serializable
data class WriteFailingTestOutput(
    val cycleId: Int,
    val testCode: String,
    val testFileName: String,
    val failureReason: String,
)

@Serializable
data class WriteImplementationInput(
    val cycleId: Int,
    val description: String,
    val testCode: String,
    val language: String = "Kotlin",
)

@Serializable
data class WriteImplementationOutput(
    val cycleId: Int,
    val implementationCode: String,
    val implementationFileName: String,
    val testsPassing: Boolean,
)

@Serializable
data class RefactorCodeInput(
    val cycleId: Int,
    val description: String,
    val testCode: String,
    val implementationCode: String,
    val language: String = "Kotlin",
)

@Serializable
data class RefactorCodeOutput(
    val cycleId: Int,
    val refactoredCode: String,
    val changesSummary: String,
    val testsStillPassing: Boolean,
)

// ---------------------------------------------------------------------------
// Tool: WriteFailingTest  (RED phase — delegates to TestingAgent)
// ---------------------------------------------------------------------------

class WriteFailingTestTool(
    private val testingAgent: TestingAgent,
) : Tool<WriteFailingTestInput, WriteFailingTestOutput>(
    WriteFailingTestInput.serializer(),
    WriteFailingTestOutput.serializer(),
    ToolDescriptor(
        name = "write_failing_test",
        description = """
            RED phase of TDD.
            Writes a failing test for a single behaviour described by the cycle.
            The test must compile but fail because the production code does not yet exist.
            Returns the test source code and the expected failure reason.
        """.trimIndent(),
        requiredParameters = listOf(
            ToolParameterDescriptor("cycleId", "Numeric ID of the TDD cycle", ToolParameterType.Integer),
            ToolParameterDescriptor("description", "What behaviour this cycle implements", ToolParameterType.String),
            ToolParameterDescriptor("acceptanceCriteria", "Acceptance criteria the test must cover", ToolParameterType.String),
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor("language", "Target programming language (default Kotlin)", ToolParameterType.String),
        ),
    )
) {


    override suspend fun execute(input: WriteFailingTestInput): WriteFailingTestOutput {
        val result: RedPhaseResult = testingAgent.writeFailingTest(
            cycleId = input.cycleId,
            description = input.description,
            acceptanceCriteria = input.acceptanceCriteria,
            language = input.language,
        )
        return WriteFailingTestOutput(
            cycleId = result.cycleId,
            testCode = result.testCode,
            testFileName = result.testFileName,
            failureReason = result.failureReason,
        )
    }
}

// ---------------------------------------------------------------------------
// Tool: WriteImplementation  (GREEN phase — delegates to CodingAgent)
// ---------------------------------------------------------------------------

class WriteImplementationTool(
    private val codingAgent: CodingAgent,
) : Tool<WriteImplementationInput, WriteImplementationOutput>(
    WriteImplementationInput.serializer(),
    WriteImplementationOutput.serializer(),
    ToolDescriptor(
        name = "write_implementation",
        description = """
            GREEN phase of TDD.
            Writes the minimal production code required to make the failing test pass.
            Must NOT over-engineer: only the simplest code that satisfies the test.
            Returns the implementation source and whether all tests are passing.
        """.trimIndent(),
        requiredParameters = listOf(
            ToolParameterDescriptor("cycleId", "Numeric ID of the TDD cycle", ToolParameterType.Integer),
            ToolParameterDescriptor("description", "What behaviour this cycle implements", ToolParameterType.String),
            ToolParameterDescriptor("testCode", "The failing test code from the RED phase", ToolParameterType.String),
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor("language", "Target programming language (default Kotlin)", ToolParameterType.String),
        ),
    )
) {


    override suspend fun execute(input: WriteImplementationInput): WriteImplementationOutput {
        val result: GreenPhaseResult = codingAgent.writeImplementation(
            cycleId = input.cycleId,
            description = input.description,
            testCode = input.testCode,
            language = input.language,
        )
        return WriteImplementationOutput(
            cycleId = result.cycleId,
            implementationCode = result.implementationCode,
            implementationFileName = result.implementationFileName,
            testsPassing = result.testsPassing,
        )
    }
}

// ---------------------------------------------------------------------------
// Tool: RefactorCode  (REFACTOR phase — delegates to RefactoringAgent)
// ---------------------------------------------------------------------------

class RefactorCodeTool(
    private val refactoringAgent: RefactoringAgent,
) : Tool<RefactorCodeInput, RefactorCodeOutput>(
    RefactorCodeInput.serializer(),
    RefactorCodeOutput.serializer(),
    ToolDescriptor(
        name = "refactor_code",
        description = """
            REFACTOR phase of TDD.
            Improves the structure, readability, and design of the green implementation
            WITHOUT changing observable behaviour.  All tests must still pass after refactoring.
            Returns the cleaned-up source and a human-readable summary of changes made.
        """.trimIndent(),
        requiredParameters = listOf(
            ToolParameterDescriptor("cycleId", "Numeric ID of the TDD cycle", ToolParameterType.Integer),
            ToolParameterDescriptor("description", "What behaviour this cycle implements", ToolParameterType.String),
            ToolParameterDescriptor("testCode", "The passing test code", ToolParameterType.String),
            ToolParameterDescriptor("implementationCode", "The green (but possibly messy) implementation", ToolParameterType.String),
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor("language", "Target programming language (default Kotlin)", ToolParameterType.String),
        ),
    )
) {


    override suspend fun execute(input: RefactorCodeInput): RefactorCodeOutput {
        val result: RefactorPhaseResult = refactoringAgent.refactor(
            cycleId = input.cycleId,
            description = input.description,
            testCode = input.testCode,
            implementationCode = input.implementationCode,
            language = input.language,
        )
        return RefactorCodeOutput(
            cycleId = result.cycleId,
            refactoredCode = result.refactoredCode,
            changesSummary = result.changesSummary,
            testsStillPassing = result.testsStillPassing,
        )
    }
}
