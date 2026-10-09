package org.andrew.jpad.core.compiler

import java.io.File

data class JavaSource(
    val className: String,
    val code: String
)

enum class DiagnosticKind {
    ERROR,
    WARNING,
    MANDATORY_WARNING,
    NOTE,
    OTHER
}

data class CompilationDiagnostic(
    val line: Long,
    val column: Long,
    val message: String,
    val kind: DiagnosticKind
) : java.io.Serializable

data class CompilationResult(
    val isSuccess: Boolean,
    val compiledClasses: Map<String, ByteArray>,
    val diagnostics: List<CompilationDiagnostic>
)

interface JavaCompilerService {
    fun compile(sources: List<JavaSource>, classpath: List<File> = emptyList()): CompilationResult
}
