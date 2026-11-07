//! ARET native bridge.
//!
//! `core` is a plain-Rust, JSON-returning facade over `ghidra_decompiler::Program`.
//! `jni_api` exposes it to Kotlin. The engine stays the single source of truth;
//! nothing here re-implements disassembly or decompilation.

pub mod core;
pub mod jni_api;
