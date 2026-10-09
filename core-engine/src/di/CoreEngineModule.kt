package org.andrew.jpad.core.di

import org.andrew.jpad.core.ExecutionEngine
import org.andrew.jpad.core.compiler.InMemoryJavaCompiler
import org.andrew.jpad.core.compiler.JavaCompilerService
import org.andrew.jpad.core.completion.CompletionService
import org.andrew.jpad.core.completion.JShellCompletionService
import org.andrew.jpad.core.dependencies.DependencyResolver
import org.andrew.jpad.core.dependencies.MavenDependencyResolver
import org.andrew.jpad.core.dump.ObjectDumper
import org.andrew.jpad.core.parser.SnippetWrapper
import org.andrew.jpad.core.runner.JShellRunner
import org.andrew.jpad.core.runner.LocalInMemoryRunner
import org.andrew.jpad.core.runner.ScriptRunner
import org.andrew.jpad.core.runner.SnippetRunner
import org.andrew.jpad.core.worker.RemoteScriptRunner
import org.andrew.jpad.core.worker.RemoteSnippetRunner
import org.andrew.jpad.core.worker.WorkerSupervisor
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreEngineModule = module {
    single { ObjectDumper() }
    singleOf(::InMemoryJavaCompiler) { bind<JavaCompilerService>() }
    singleOf(::SnippetWrapper)
    single<DependencyResolver> { MavenDependencyResolver() }
    single<CompletionService> { JShellCompletionService() }
    // In-process runners: only the worker JVM (and tests of the runners themselves) use these.
    singleOf(::LocalInMemoryRunner)
    singleOf(::JShellRunner)
    // User code runs in an isolated worker JVM.
    single { WorkerSupervisor() }
    singleOf(::RemoteScriptRunner) { bind<ScriptRunner>() }
    singleOf(::RemoteSnippetRunner) { bind<SnippetRunner>() }
    single { org.andrew.jpad.core.database.DatabaseService() }
    singleOf(::ExecutionEngine)
}
