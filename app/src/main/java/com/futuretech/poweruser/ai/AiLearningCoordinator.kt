package com.futuretech.poweruser.ai

class AiLearningCoordinator(
    private val gateway: AiTutorGateway
) {
    suspend fun run(
        apiKey: String?,
        request: AiTutorRequest,
        localHints: Triple<String, String, String>
    ): AiTutorResponse {
        val decision = AiTutorPolicy.authorize(request)
        if (!decision.allowed) {
            return AiTutorResponse(
                success = false,
                failureKind = decision.failureKind,
                userMessage = decision.message
            )
        }

        val response = gateway.ask(apiKey, request)
        if (response.success) return response

        if (request.mode == AiLearningMode.HINT && response.failureKind in FALLBACK_FAILURES) {
            val fallback = AiTutorPolicy.localHint(
                request.hintLevel,
                localHints.first,
                localHints.second,
                localHints.third
            )
            val reason = response.userMessage?.let { "$it\n\n" }.orEmpty()
            return AiTutorResponse(
                success = true,
                text = reason + "[로컬 힌트 ${request.hintLevel}/3] $fallback",
                failureKind = response.failureKind,
                httpStatus = response.httpStatus,
                model = response.model,
                usedLocalFallback = true
            )
        }
        return response
    }

    companion object {
        private val FALLBACK_FAILURES = setOf(
            AiFailureKind.MISSING_KEY,
            AiFailureKind.OFFLINE,
            AiFailureKind.AUTH,
            AiFailureKind.RATE_LIMIT,
            AiFailureKind.TIMEOUT,
            AiFailureKind.SERVER,
            AiFailureKind.BAD_RESPONSE,
            AiFailureKind.UNKNOWN,
            AiFailureKind.POLICY_BLOCKED
        )
    }
}
