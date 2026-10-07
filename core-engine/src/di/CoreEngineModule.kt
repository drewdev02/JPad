package org.andrew.jpad.core.di

import org.andrew.jpad.core.ExecutionEngine
import org.andrew.jpad.core.compiler.InMemoryJavaCompiler
import org.andrew.jpad.core.compiler.JavaCompilerService
import org.andrew.jpad.core.parser.SnippetWrapper
import org.andrew.jpad.core.runner.LocalInMemoryRunner
import org.andrew.jpad.core.runner.ScriptRunner
import org.koin.dsl.module

val coreEngineModule = module {
    single<JavaCompilerService> { InMemoryJavaCompiler() }
    single { SnippetWrapper() }
    single<ScriptRunner> { LocalInMemoryRunner() }
    single { ExecutionEngine(get(), get(), get()) }
}
