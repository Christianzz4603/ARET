package app.aret.engine

/**
 * Raw JNI surface. Symbols are implemented in native/src/jni_api.rs.
 * Every call returns a JSON string {"ok":true,"data":...} or {"ok":false,"error":"..."}.
 * Prefer [Engine] / [BinarySession] over calling these directly.
 */
object NativeEngine {
    init {
        System.loadLibrary("aret_native")
    }

    @JvmStatic external fun nativeOpen(path: String, language: String): String?
    @JvmStatic external fun nativeClose(handle: Long): String?
    @JvmStatic external fun nativeInfo(handle: Long): String?
    @JvmStatic external fun nativeFunctions(handle: Long, offset: Int, limit: Int): String?
    @JvmStatic external fun nativeDisassemble(handle: Long, address: Long, count: Int): String?
    @JvmStatic external fun nativeDisassembleFunction(handle: Long, address: Long): String?
    @JvmStatic external fun nativeDecompile(handle: Long, address: Long): String?
    @JvmStatic external fun nativeStrings(handle: Long, offset: Int, limit: Int): String?
    @JvmStatic external fun nativeDataSymbols(handle: Long, offset: Int, limit: Int): String?
    @JvmStatic external fun nativeRelocations(handle: Long, offset: Int, limit: Int): String?
    @JvmStatic external fun nativeReferencesFrom(handle: Long, address: Long): String?
    @JvmStatic external fun nativeCallRelations(handle: Long, address: Long): String?
}
