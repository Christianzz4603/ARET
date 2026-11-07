# Architecture & Plan

## Engine facts (from the engine README, verified 2026-11-07)
- Crate: `ghidra-decompiler` 2.1.0, edition 2024, Apache-2.0. Builds rlib/cdylib/staticlib.
- **No C ABI / JNI exists.** README states a wrapper layer must be added.
- Loading: `Program::open`, `from_image` (ELF/PE/Mach-O), `from_raw_bytes`.
- Per-function: `Program::{functions, find_function_at, disassemble, pcode, function_pcode, decompile, call_graph, strings, data_symbols, code_sections, architecture}`.
- Whole-program: `AnalysisEngine::{open, analyze, analyze_function, analyze_selected}` with `AnalysisOptions::low_memory()` for phones and streaming `AnalysisEvent`s.
- Evidence: `BinaryIntelligence` (xrefs, CFG, dataflow, fingerprints, prototype evidence), `DecompilerReconstructor` (regions, params/returns evidence, compiler patterns), `abi` (SystemVx86_64, WindowsX64, Aapcs64).
- Exports (CLI `analyze`): `.c`, `.asm`, `.csv`, `.xrefs.csv`, `.cfg.csv`, `.report.txt`.
- Android: CI builds all 4 ABIs with NDK 27.2, min API 24. Use `--no-default-features --features mobile` for ARM-only specs.

## Known gaps to implement (in the engine repo, feature-gated `jni`)
1. JNI/C-ABI wrapper exposing: open/close, function list (paged), disassemble, decompile, call graph, xrefs, strings, data symbols, progress events, cancel.
2. Structured ELF explorer API (headers, sections, segments, dynamic, relocations). Not documented in the README; verify in source first.
3. Context-builder output: a serde JSON schema over real engine types.

## Evidence caveats the AI prompt MUST convey
- Xrefs (except direct calls), CFG branch targets, prototypes and types are heuristic.
- Decompiled C is inferred pseudocode, not original source.
- Undecodable gaps are marked in asm output; unreachable code may be missed.

## Modules
```
app/        Compose UI shell, navigation
engine/     Kotlin bindings to Rust (JNI), workspace/project mgmt
analysis/   Kotlin models mirroring engine JSON
ai/         llama.cpp JNI, prompt + context builder, model manager
native/     Rust cdylib crate `aret-native` (depends on ghidra-decompiler)
ui/         Compose screens: Home, Explorer, Disasm, Decompiler, Functions, Assistant, Settings
```

## Model (to verify before committing to one)
Candidates: SmolLM2-135M/360M-Instruct, Qwen2.5-0.5B-Instruct (Apache-2.0).
Must check: exact GGUF Q4 size, license text, tokens/sec on mid/low ARM64, RAM at chosen context.
Runtime: llama.cpp via NDK/JNI, CPU only, configurable threads and n_ctx.

## Milestones
1. Gradle project + cargo-ndk build + CI producing an arm64 debug APK.
2. JNI wrapper + ELF API in engine, with tests.
3. Explorer / Functions / Disassembler / Decompiler screens on real data.
4. llama.cpp integration + context builder + chat UI.
5. Settings, exports, performance pass on low-end devices.
