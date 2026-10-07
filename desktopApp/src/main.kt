package org.andrew.jpad

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.andrew.jpad.di.appModule
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule)
    }

    application {
        Window(onCloseRequest = ::exitApplication, title = "JPad - LINQPad for Java & JVM") {
            App()
        }
    }
}
