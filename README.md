# ARET — AI Reverse Engineering Studio

Standalone Android reverse-engineering app built on the Rust Ghidra port
(`ghidra-decompiler`, https://github.com/Christianzz4603/ghidra-rust-engine)
with an optional, fully offline, bundled small LLM assistant.

Status: **planning / scaffolding**. Nothing here has been built or tested yet.
See `docs/ARCHITECTURE.md`.

Principles
- The Rust engine is the single source of truth for analysis.
- AI is optional; all RE features work with it disabled.
- No network, no API keys, no model download. Model ships in the APK.
- The AI only sees structured engine output and must say when evidence is missing.
- Original binaries are never modified.
