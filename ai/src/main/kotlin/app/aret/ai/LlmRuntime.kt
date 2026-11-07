package app.aret.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

sealed interface ModelStatus {
    data object NotInstalled : ModelStatus
    data object Loading : ModelStatus
    data class Ready(val name: String, val sizeBytes: Long) : ModelStatus
    data class Failed(val reason: String) : ModelStatus
}

data class GenerationParams(
    val maxNewTokens: Int = 512,
    val contextTokens: Int = 2048,
    val threads: Int = 2,
    val temperature: Float = 0.2f,
)

/**
 * Offline inference runtime. The real implementation (llama.cpp via JNI, bundled GGUF model)
 * is a later milestone; see docs/ROADMAP.md. Nothing in the app may fake model output.
 */
interface LlmRuntime : AutoCloseable {
    val status: StateFlow<ModelStatus>
    fun generate(system: String, user: String, params: GenerationParams = GenerationParams()): Flow<String>
}

/** Honest stand-in used until a model is bundled: reports NotInstalled and refuses to generate. */
class NoModelRuntime : LlmRuntime {
    override val status: StateFlow<ModelStatus> = MutableStateFlow(ModelStatus.NotInstalled)

    override fun generate(system: String, user: String, params: GenerationParams): Flow<String> = flow {
        throw IllegalStateException("No language model is installed in this build.")
    }

    override fun close() {}
}
