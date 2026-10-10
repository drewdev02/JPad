package org.andrew.jpad

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.andrew.jpad.core.ExecutionEngine
import org.andrew.jpad.core.dump.DumpValue
import org.andrew.jpad.di.appModule
import org.andrew.jpad.ui.components.grid.DumpTableExporter
import org.koin.core.context.startKoin
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        runCli(args)
        return
    }

    startKoin {
        modules(appModule)
    }

    application {
        Window(onCloseRequest = ::exitApplication, title = "JPad - LINQPad for Java & JVM") {
            App()
        }
    }
}

fun runCli(args: Array<String>) {
    var codeToExecute: String? = null
    var format = "text"
    var showDumps = true
    var showHelp = false

    var i = 0
    while (i < args.size) {
        when (val arg = args[i]) {
            "-h", "--help" -> {
                showHelp = true
                i++
            }
            "-e", "--eval" -> {
                if (i + 1 < args.size) {
                    codeToExecute = args[i + 1]
                    i += 2
                } else {
                    System.err.println("Error: -e/--eval requires a code string argument")
                    exitProcess(1)
                }
            }
            "-f", "--format" -> {
                if (i + 1 < args.size) {
                    format = args[i + 1].lowercase()
                    i += 2
                } else {
                    System.err.println("Error: -f/--format requires a format (text, json, csv, tsv)")
                    exitProcess(1)
                }
            }
            "--no-dump" -> {
                showDumps = false
                i++
            }
            else -> {
                if (!arg.startsWith("-") && codeToExecute == null) {
                    val file = File(arg)
                    if (file.exists()) {
                        codeToExecute = file.readText()
                    } else {
                        System.err.println("Error: File not found: $arg")
                        exitProcess(1)
                    }
                    i++
                } else {
                    i++
                }
            }
        }
    }

    if (showHelp || codeToExecute == null) {
        println(
            """
            JPad CLI (LINQPad equivalent for Java & JVM)
            
            Usage:
              jpad [options] <script-file>
              jpad -e "<code>" [options]

            Options:
              -e, --eval <code>        Execute Java/JPad snippet directly
              -f, --format <format>    Dump output format: text (default), json, csv, tsv
              --no-dump                Suppress dump output
              -h, --help               Show this help message

            Examples:
              jpad -e 'dump(List.of(1, 2, 3), "Numbers");'
              jpad -e 'var users = cache("users", () -> query("SELECT * FROM users")); dump(users);'
              jpad -e 'benchmark("Sorting", () -> Collections.sort(new ArrayList<>(List.of(3, 1, 2))));'
              jpad script.jpad
            """.trimIndent()
        )
        exitProcess(if (showHelp) 0 else 1)
    }

    val koinApp = startKoin {
        modules(appModule)
    }
    val engine = koinApp.koin.get<ExecutionEngine>()

    val response = engine.execute(codeToExecute)

    if (response.output.isNotBlank()) {
        print(response.output)
        if (!response.output.endsWith("\n")) println()
    }

    if (showDumps && response.dumps.isNotEmpty()) {
        for (dump in response.dumps) {
            renderDumpToCli(dump, format)
        }
    }

    if (response.error.isNotBlank()) {
        System.err.print(response.error)
        if (!response.error.endsWith("\n")) System.err.println()
    }

    exitProcess(if (response.isSuccess) 0 else 1)
}

fun renderDumpToCli(dump: DumpValue, format: String) {
    when (format) {
        "json" -> {
            when (dump) {
                is DumpValue.Table -> println(DumpTableExporter.toJson(dump.columns, dump.rows))
                is DumpValue.Primitive -> println("{\"type\": \"${dump.typeName ?: "scalar"}\", \"value\": ${dump.rawValue}}")
                is DumpValue.Text -> println("{\"text\": \"${dump.content}\"}")
                is DumpValue.KeyValueList -> println("{\"${dump.title}\": {${dump.entries.joinToString(", ") { "\"${it.first}\": \"${it.second}\"" }}}}")
            }
        }
        "csv" -> {
            if (dump is DumpValue.Table) {
                print(DumpTableExporter.toCsv(dump.columns, dump.rows))
            } else {
                println(dump)
            }
        }
        "tsv" -> {
            if (dump is DumpValue.Table) {
                print(DumpTableExporter.toTsv(dump.columns, dump.rows))
            } else {
                println(dump)
            }
        }
        else -> {
            when (dump) {
                is DumpValue.Table -> {
                    println("=== [Table: ${dump.typeName}] (${dump.rows.size} rows) ===")
                    print(DumpTableExporter.toAsciiTable(dump.columns, dump.rows))
                    println()
                }
                is DumpValue.KeyValueList -> {
                    println("=== [${dump.title}] ===")
                    val maxKeyLen = dump.entries.maxOfOrNull { it.first.length } ?: 10
                    for ((k, v) in dump.entries) {
                        val vStr = DumpTableExporter.extractCellString(v)
                        println("  ${k.padEnd(maxKeyLen)} : $vStr")
                    }
                    println()
                }
                is DumpValue.Primitive -> {
                    val t = if (dump.typeName != null) " (${dump.typeName})" else ""
                    println("Dump: ${dump.rawValue}$t")
                }
                is DumpValue.Text -> {
                    println("Dump: ${dump.content}")
                }
            }
        }
    }
}
