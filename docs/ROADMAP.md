# Roadmap (foundation → product)

Legend: [x] done and CI-verified, [~] scaffolded but not verified on a device, [ ] not started.

## Foundation
- [x] Rust `aret-native` crate over the real engine (JSON facade + JNI), host test + arm64 build in CI
- [~] Gradle project: `:app` (Compose), `:engine` (Kotlin bindings), `:ai` (context builder, runtime interface)
- [~] CI: unit tests + debug APK with the arm64 `.so` bundled
- [~] Minimal real UI: open binary → function list (paged) → decompiled / disassembly / AI-context tabs

## Engine bridge (Rust, `native/`)
- [ ] Expose `intelligence` (xrefs, CFG, dataflow, prototype evidence) and `reconstruction` via JNI
- [ ] Binary metadata: sections/segments/imports/exports (check `ghidra_host::ProgramSnapshot`, `object_arch` first)
- [ ] Background whole-program analysis with progress events + cancel (`AnalysisEngine::analyze`, `AnalysisOptions::low_memory()`)
- [ ] Strings-per-function and xrefs-per-function for context
- [ ] Export: `.c`, `.asm`, `.csv` via the engine's existing exporters
- [ ] ARM64 fixture test (committed small `.so`) so CI verifies the real target, not only x86-64

## UI
- [ ] Home: recent projects, shared-file import
- [ ] Binary Explorer (sections, symbols, strings, imports, exports)
- [ ] Disassembler view with function boundaries and xrefs; decompiler view with syntax highlighting
- [ ] Function Explorer with call relationships; Settings; export/share

## AI (offline, optional)
- [ ] Pick the model: verify GGUF size, license text, speed and RAM on ARM64 (candidates: SmolLM2-135M/360M-Instruct, Qwen2.5-0.5B-Instruct)
- [ ] llama.cpp via NDK/JNI as a separate native lib; implement `LlmRuntime`
- [ ] Package the model as an APK asset (no downloads); configurable threads/context
- [ ] Chat UI wired to `ContextBuilder`; context expansion on demand; grounding evaluation set

## Local dev (optional; CI does this for you)
```
cargo ndk -t arm64-v8a -P 24 -o engine/src/main/jniLibs build --release -p aret-native
gradle :app:assembleDebug
```
