//! JNI surface for `app.aret.engine.NativeEngine`.
//!
//! Every call returns a JSON string: {"ok":true,"data":...} or {"ok":false,"error":"..."}.
//! Panics are caught so a decompiler bug cannot take the app down.

use crate::core::Session;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use jni::JNIEnv;
use serde_json::{json, Value};
use std::collections::HashMap;
use std::panic::{catch_unwind, AssertUnwindSafe};
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::{Arc, Mutex, OnceLock};

type Sessions = Mutex<HashMap<i64, Arc<Mutex<Session>>>>;

static NEXT: AtomicI64 = AtomicI64::new(1);
static SESSIONS: OnceLock<Sessions> = OnceLock::new();

fn sessions() -> &'static Sessions {
    SESSIONS.get_or_init(|| Mutex::new(HashMap::new()))
}

fn reply(env: &mut JNIEnv, r: Result<Value, String>) -> jstring {
    let v = match r {
        Ok(d) => json!({"ok": true, "data": d}),
        Err(m) => json!({"ok": false, "error": m}),
    };
    match env.new_string(v.to_string()) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

fn guard<F: FnOnce() -> Result<Value, String>>(f: F) -> Result<Value, String> {
    match catch_unwind(AssertUnwindSafe(f)) {
        Ok(r) => r,
        Err(_) => Err("native panic inside engine".to_string()),
    }
}

fn jstr(env: &mut JNIEnv, s: &JString) -> Result<String, String> {
    env.get_string(s).map(String::from).map_err(|e| e.to_string())
}

fn with_session<F>(handle: jlong, f: F) -> Result<Value, String>
where
    F: FnOnce(&mut Session) -> Result<Value, String>,
{
    let s = sessions()
        .lock()
        .map_err(|_| "session table poisoned".to_string())?
        .get(&handle)
        .cloned()
        .ok_or_else(|| "invalid handle".to_string())?;
    let mut g = s.lock().map_err(|_| "session poisoned".to_string())?;
    guard(|| f(&mut g))
}

#[no_mangle]
pub extern "system" fn Java_app_aret_engine_NativeEngine_nativeOpen<'l>(
    mut env: JNIEnv<'l>,
    _c: JClass<'l>,
    path: JString<'l>,
    language: JString<'l>,
) -> jstring {
    let r = (|| {
        let p = jstr(&mut env, &path)?;
        let l = jstr(&mut env, &language)?;
        let lang = if l.is_empty() { None } else { Some(l) };
        guard(|| {
            let s = Session::open(&p, lang.as_deref())?;
            let h = NEXT.fetch_add(1, Ordering::Relaxed);
            sessions()
                .lock()
                .map_err(|_| "session table poisoned".to_string())?
                .insert(h, Arc::new(Mutex::new(s)));
            Ok(json!({"handle": h}))
        })
    })();
    reply(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_app_aret_engine_NativeEngine_nativeClose<'l>(
    mut env: JNIEnv<'l>,
    _c: JClass<'l>,
    handle: jlong,
) -> jstring {
    let r = sessions()
        .lock()
        .map_err(|_| "session table poisoned".to_string())
        .map(|mut m| json!({"closed": m.remove(&handle).is_some()}));
    reply(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_app_aret_engine_NativeEngine_nativeInfo<'l>(
    mut env: JNIEnv<'l>,
    _c: JClass<'l>,
    handle: jlong,
) -> jstring {
    let r = with_session(handle, |s| s.info());
    reply(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_app_aret_engine_NativeEngine_nativeFunctions<'l>(
    mut env: JNIEnv<'l>,
    _c: JClass<'l>,
    handle: jlong,
    offset: jint,
    limit: jint,
) -> jstring {
    let r = with_session(handle, |s| s.functions(offset.max(0) as usize, limit.max(0) as usize));
    reply(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_app_aret_engine_NativeEngine_nativeDisassemble<'l>(
    mut env: JNIEnv<'l>,
    _c: JClass<'l>,
    handle: jlong,
    address: jlong,
    count: jint,
) -> jstring {
    let r = with_session(handle, |s| s.disassemble(address as u64, count.max(0) as usize));
    reply(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_app_aret_engine_NativeEngine_nativeDisassembleFunction<'l>(
    mut env: JNIEnv<'l>,
    _c: JClass<'l>,
    handle: jlong,
    address: jlong,
) -> jstring {
    let r = with_session(handle, |s| s.disassemble_function(address as u64));
    reply(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_app_aret_engine_NativeEngine_nativeDecompile<'l>(
    mut env: JNIEnv<'l>,
    _c: JClass<'l>,
    handle: jlong,
    address: jlong,
) -> jstring {
    let r = with_session(handle, |s| s.decompile(address as u64));
    reply(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_app_aret_engine_NativeEngine_nativeStrings<'l>(
    mut env: JNIEnv<'l>,
    _c: JClass<'l>,
    handle: jlong,
    offset: jint,
    limit: jint,
) -> jstring {
    let r = with_session(handle, |s| s.strings(offset.max(0) as usize, limit.max(0) as usize));
    reply(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_app_aret_engine_NativeEngine_nativeDataSymbols<'l>(
    mut env: JNIEnv<'l>,
    _c: JClass<'l>,
    handle: jlong,
    offset: jint,
    limit: jint,
) -> jstring {
    let r = with_session(handle, |s| s.data_symbols(offset.max(0) as usize, limit.max(0) as usize));
    reply(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_app_aret_engine_NativeEngine_nativeRelocations<'l>(
    mut env: JNIEnv<'l>,
    _c: JClass<'l>,
    handle: jlong,
    offset: jint,
    limit: jint,
) -> jstring {
    let r = with_session(handle, |s| s.relocations(offset.max(0) as usize, limit.max(0) as usize));
    reply(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_app_aret_engine_NativeEngine_nativeReferencesFrom<'l>(
    mut env: JNIEnv<'l>,
    _c: JClass<'l>,
    handle: jlong,
    address: jlong,
) -> jstring {
    let r = with_session(handle, |s| s.references_from(address as u64));
    reply(&mut env, r)
}

#[no_mangle]
pub extern "system" fn Java_app_aret_engine_NativeEngine_nativeCallRelations<'l>(
    mut env: JNIEnv<'l>,
    _c: JClass<'l>,
    handle: jlong,
    address: jlong,
) -> jstring {
    let r = with_session(handle, |s| s.call_relations(address as u64));
    reply(&mut env, r)
}
