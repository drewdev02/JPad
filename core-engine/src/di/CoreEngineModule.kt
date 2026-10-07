package org.andrew.jpad.core.di

import org.andrew.jpad.core.ExecutionEngine
import org.andrew.jpad.core.compiler.InMemoryJavaCompiler
import org.andrew.jpad.core.compiler.JavaCompilerService
import org.andrew.jpad.core.dump.ObjectDumper
import org.andrew.jpad.core.parser.SnippetWrapper
import org.andrew.jpad.core.runner.JShellRunner
import org.andrew.jpad.core.runner.LocalInMemoryRunner
import org.andrew.jpad.core.runner.ScriptRunner
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreEngineModule = module {
    single { ObjectDumper() }
    singleOf(::InMemoryJavaCompiler) { bind<JavaCompilerService>() }
    singleOf(::SnippetWrapper)
    singleOf(::LocalInMemoryRunner) { bind<ScriptRunner>() }
    singleOf(::JShellRunner)
    singleOf(::ExecutionEngine)
}
