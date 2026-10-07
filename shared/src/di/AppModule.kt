package org.andrew.jpad.di

import org.andrew.jpad.core.di.coreEngineModule
import org.andrew.jpad.presentation.EditorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    includes(coreEngineModule)
    viewModel { EditorViewModel(get()) }
}
