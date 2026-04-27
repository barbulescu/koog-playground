/**
 * Represents a single Red-Green-Refactor TDD cycle for one unit of behaviour.
 */
data class TddCycle(
    val id: Int,
    val description: String,
    val acceptanceCriteria: String,
)

/**
 * Outcome produced by the TestingAgent in the RED phase.
 */
data class RedPhaseResult(
    val cycleId: Int,
    val testCode: String,
    val testFileName: String,
    val failureReason: String,
)

/**
 * Outcome produced by the CodingAgent in the GREEN phase.
 */
data class GreenPhaseResult(
    val cycleId: Int,
    val implementationCode: String,
    val implementationFileName: String,
    val testsPassing: Boolean,
)

/**
 * Outcome produced by the RefactoringAgent in the REFACTOR phase.
 */
data class RefactorPhaseResult(
    val cycleId: Int,
    val refactoredCode: String,
    val changesSummary: String,
    val testsStillPassing: Boolean,
)

/**
 * Full result of one completed TDD cycle (Red → Green → Refactor).
 */
data class CompletedCycle(
    val cycle: TddCycle,
    val red: RedPhaseResult,
    val green: GreenPhaseResult,
    val refactor: RefactorPhaseResult,
)

/**
 * Aggregated result of the entire TDD planning session.
 */
data class TddSessionResult(
    val feature: String,
    val completedCycles: List<CompletedCycle>,
    val summary: String,
)
