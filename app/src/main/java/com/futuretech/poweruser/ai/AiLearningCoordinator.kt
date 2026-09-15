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

        if (request.mode == AiLearningMode.HINT) {
            val fallback = AiTutorPolicy.localHint(
                request.hintLevel,
                localHints.first,
                localHints.second,
                localHints.third
            )
            return AiTutorResponse(
                success = true,
                text = "[무료 로컬 힌트 ${request.hintLevel}/3] $fallback",
                usedLocalFallback = true
            )
        }

        return gateway.ask(apiKey, request)
    }
}
