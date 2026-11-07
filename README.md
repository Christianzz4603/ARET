# ARET — AI Reverse Engineering Studio

Standalone Android reverse-engineering app built on the Rust Ghidra port
(`ghidra-decompiler`, https://github.com/Christianzz4603/ghidra-rust-engine)
with an optional, fully offline, bundled small LLM assistant (planned).

Status: **foundation**. See `docs/ROADMAP.md` for what is done and what is not,
and `docs/ARCHITECTURE.md` for design.

- `native/` Rust crate `aret-native`: JSON facade + JNI over the engine
- `engine/` Kotlin bindings (`NativeEngine`, `BinarySession`)
- `ai/` context builder + `LlmRuntime` interface (no model yet; never fakes output)
- `app/` Jetpack Compose UI

Principles: the Rust engine is the source of truth; AI is optional; no network or API keys;
the AI only sees structured engine output and must say when evidence is missing;
original binaries are never modified (the app analyzes a private copy).
