package com.futuretech.poweruser.ai

enum class AiLearningMode {
    SOLO,
    HINT,
    COLLABORATE
}

enum class AiTutorTask {
    HINT,
    CODE_EXPLANATION,
    DEBUG_GUIDANCE,
    COLLABORATE,
    EXPLANATION_FEEDBACK
}

enum class AiFailureKind {
    NONE,
    MISSING_KEY,
    POLICY_BLOCKED,
    SENSITIVE_INPUT,
    INVALID_INPUT,
    OFFLINE,
    AUTH,
    RATE_LIMIT,
    TIMEOUT,
    SERVER,
    BAD_REQUEST,
    BAD_RESPONSE,
    UNKNOWN
}

data class AiTutorRequest(
    val lessonId: String,
    val curriculumType: String,
    val level: Int,
    val moduleTitle: String,
    val lessonTitle: String,
    val expectedOutcome: String,
    val practiceLanguage: String,
    val mode: AiLearningMode,
    val task: AiTutorTask,
    val userMessage: String,
    val code: String = "",
    val hintLevel: Int = 1,
    val practiceAttempted: Boolean = false,
    val explicitFullSolutionRequest: Boolean = false
)

data class AiTutorResponse(
    val success: Boolean,
    val text: String = "",
    val failureKind: AiFailureKind = AiFailureKind.NONE,
    val userMessage: String? = null,
    val httpStatus: Int? = null,
    val model: String? = null,
    val usedLocalFallback: Boolean = false
)

data class AiModeAvailability(
    val soloEnabled: Boolean = true,
    val hintEnabled: Boolean = true,
    val collaborateEnabled: Boolean,
    val fullSolutionEnabled: Boolean,
    val explanation: String
)

data class AiPolicyDecision(
    val allowed: Boolean,
    val failureKind: AiFailureKind = AiFailureKind.NONE,
    val message: String? = null
)

data class AiHttpResult(
    val statusCode: Int,
    val body: String
)

interface AiHttpTransport {
    suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int
    ): AiHttpResult
}

interface AiTutorGateway {
    suspend fun ask(apiKey: String?, request: AiTutorRequest): AiTutorResponse
}
