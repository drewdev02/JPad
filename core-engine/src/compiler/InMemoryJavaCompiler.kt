package org.andrew.jpad.core.compiler

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.net.URI
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.FileObject
import javax.tools.ForwardingJavaFileManager
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardJavaFileManager
import javax.tools.StandardLocation
import javax.tools.ToolProvider

class InMemoryJavaCompiler : JavaCompilerService {

    private val compiler = ToolProvider.getSystemJavaCompiler()
        ?: error("System JavaCompiler not available. Ensure JPad runs on a JDK, not a stripped JRE.")

    override fun compile(sources: List<JavaSource>, classpath: List<File>): CompilationResult {
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val standardFileManager: StandardJavaFileManager = compiler.getStandardFileManager(diagnostics, null, null)

        if (classpath.isNotEmpty()) {
            standardFileManager.setLocation(StandardLocation.CLASS_PATH, classpath)
        }

        val compiledClassBytecodes = mutableMapOf<String, ByteArray>()
        val memoryFileManager = MemoryJavaFileManager(standardFileManager, compiledClassBytecodes)

        val javaFileObjects = sources.map { source ->
            StringJavaFileObject(source.className, source.code)
        }

        val options = mutableListOf<String>()
        if (classpath.isNotEmpty()) {
            options.add("-classpath")
            options.add(classpath.joinToString(File.pathSeparator) { it.absolutePath })
        }

        val task = compiler.getTask(
            null,
            memoryFileManager,
            diagnostics,
            options,
            null,
            javaFileObjects
        )

        val success = task.call() ?: false
        memoryFileManager.flush()

        val mappedDiagnostics = diagnostics.diagnostics.map { diag ->
            CompilationDiagnostic(
                line = diag.lineNumber,
                column = diag.columnNumber,
                message = diag.getMessage(null) ?: "",
                kind = when (diag.kind) {
                    Diagnostic.Kind.ERROR -> DiagnosticKind.ERROR
                    Diagnostic.Kind.WARNING -> DiagnosticKind.WARNING
                    Diagnostic.Kind.MANDATORY_WARNING -> DiagnosticKind.MANDATORY_WARNING
                    Diagnostic.Kind.NOTE -> DiagnosticKind.NOTE
                    else -> DiagnosticKind.OTHER
                }
            )
        }

        return CompilationResult(
            isSuccess = success,
            compiledClasses = compiledClassBytecodes,
            diagnostics = mappedDiagnostics
        )
    }

    private class StringJavaFileObject(name: String, private val content: String) :
        SimpleJavaFileObject(URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE) {
        override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = content
    }

    private class ByteArrayJavaFileObject(name: String) :
        SimpleJavaFileObject(URI.create("bytes:///" + name.replace('.', '/') + JavaFileObject.Kind.CLASS.extension), JavaFileObject.Kind.CLASS) {
        private val outputStream = ByteArrayOutputStream()

        override fun openOutputStream(): OutputStream = outputStream

        fun getBytes(): ByteArray = outputStream.toByteArray()
    }

    private class MemoryJavaFileManager(
        fileManager: JavaFileManager,
        private val bytecodes: MutableMap<String, ByteArray>
    ) : ForwardingJavaFileManager<JavaFileManager>(fileManager) {

        private val classFileObjects = mutableMapOf<String, ByteArrayJavaFileObject>()

        override fun getJavaFileForOutput(
            location: JavaFileManager.Location?,
            className: String,
            kind: JavaFileObject.Kind?,
            sibling: FileObject?
        ): JavaFileObject {
            val fileObject = ByteArrayJavaFileObject(className)
            classFileObjects[className] = fileObject
            return fileObject
        }

        override fun close() {
            super.close()
            for ((className, fileObject) in classFileObjects) {
                bytecodes[className] = fileObject.getBytes()
            }
        }

        override fun flush() {
            super.flush()
            for ((className, fileObject) in classFileObjects) {
                bytecodes[className] = fileObject.getBytes()
            }
        }
    }
}
