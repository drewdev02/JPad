# JPad Architecture & Technical Design

> **Vision:** A lightweight, instant-feedback scratchpad and execution engine for Java & JVM developers — the LINQPad equivalent for the Java ecosystem.

---

## 1. Executive Summary & Philosophy

JPad is designed as a **developer tool**, not a business-domain application. Consequently, the architecture prioritizes **throughput, isolation, low latency, and rich data inspection** over traditional enterprise tactical DDD layers.

### Key Architectural Choices

| Concern | Decision | Why |
|---|---|---|
| **Structural Style** | **Modular Monolith + Vertical Slices** | Separates technical subsystems cleanly while keeping feature workflows (e.g. execution, package resolution) cohesive and free of ceremonial boilerplate. |
| **Execution Model** | **Out-of-Process Worker (IPC)** | Isolates user code execution in a child JVM process to protect the IDE/UI from `OutOfMemoryError`, `System.exit()`, thread deadlocks, or native crashes. |
| **UI Architecture** | **MVVM / MVI with Compose Multiplatform** | Unidirectional Data Flow (UDF) via `StateFlow<UiState>` and explicit UI events for predictable desktop UI rendering. |
| **Dependency Injection** | **Lightweight / Pragmatic DI (Koin or Pure DI)** | Simple constructor-based injection and composition roots per module. |

---

## 2. System Subsystems

```
┌────────────────────────────────────────────────────────┐
│                      JPad Desktop                      │
│                                                        │
│  ┌─────────────────────────┐ ┌──────────────────────┐  │
│  │     UI / Editor Slice   │ │   UI / Results Slice │  │
│  │ (Code editor, syntax,   │ │ (.dump() tree/table, │  │
│  │  shortcuts, tabs)       │ │  stdout/stderr log)  │  │
│  └────────────┬────────────┘ └──────────▲───────────┘  │
│               │                         │              │
│  ┌────────────▼─────────────────────────┴───────────┐  │
│  │                   Core Engine                    │  │
│  │  - Script Parser & AST Preprocessor              │  │
│  │  - Dynamic Dependency Resolver (Maven/Ivy)       │  │
│  │  - Java/Kotlin In-Memory Compiler                │  │
│  │  - Worker Process Supervisor & Lifecycle Manager │  │
│  └──────────────────────┬───────────────────────────┘  │
└─────────────────────────┼──────────────────────────────┘
                          │ IPC (Sockets / Proto / JSON Streams)
┌─────────────────────────▼──────────────────────────────┐
│                  Isolated Worker JVM                   │
│                                                        │
│  - Dynamic ClassLoader & Classpath Manager             │
│  - User Snippet Invoker                                │
│  - Object Introspector / Dump Serializer (`.dump()`)   │
│  - Output Capture (stdout / stderr redirects)          │
└────────────────────────────────────────────────────────┘
```

---

## 3. Module & Package Structure

Organized by functional modules (aligned with Amper / Gradle modularity):

```
JPad/
├── desktopApp/                  # JVM Application Entry Point & Window Management
│   └── src/
│       └── main.kt
│
├── core-engine/                 # Compilation, Orchestration & Process Lifecycle
│   ├── compiler/                # In-memory javac / ecj compilation pipeline
│   ├── dependencies/            # Maven coordinate resolver & classpath builder
│   ├── parser/                  # Snippet wrapping (statement vs expression vs class)
│   └── process/                 # Worker process spawn, heartbeat, and supervisor
│
├── worker-runtime/              # Lightweight JAR executed in the child JVM
│   ├── agent/                   # Entry point for the isolated process
│   ├── dump/                    # Object reflection, traversal, and dump payload model
│   └── ipc/                     # Socket/Stream communication back to Core Engine
│
├── ui-features/                 # Compose UI Slices
│   ├── editor/                  # Code input, line numbers, syntax highlighting
│   ├── results/                 # Interactive tables, tree view for dumps, console
│   ├── dependencies_panel/      # NuGet/Maven dependency search dialog
│   └── shared_components/       # Split panes, toolbars, status bar, theme
│
└── shared/                      # Common models, IPC protocol definitions, and contracts
    ├── model/                   # ExecutionRequest, ExecutionResult, DumpNode
    └── protocol/                # IPC message formats and serialization
```

---

## 4. End-to-End Execution Pipeline

When the user presses **Run (F5 / Ctrl+Enter)**:

1. **Preprocessing:** 
   - Detect snippet mode: *Expression*, *Statements*, or *Full Class/Program*.
   - Extract header directives (e.g., `// @deps org.apache.commons:commons-lang3:3.14.0`).
   - Wrap raw statements into a runnable class (e.g. `GeneratedScript.java` with a static `main` method) if needed.
2. **Dependency Resolution:**
   - Resolve and cache dependencies from local `~/.m2` or remote Maven repositories.
   - Build effective classpath.
3. **Compilation:**
   - Compile in-memory using `javax.tools.JavaCompiler` or Eclipse ECJ.
   - Collect compiler diagnostics (errors/warnings with line/column coordinates) and report to editor.
4. **Execution Dispatch:**
   - Send bytecodes and classpath to the **Worker Process** via IPC.
   - If worker is stale or crashed, the supervisor launches a fresh child process.
5. **Streaming Output & `.dump()` Rendering:**
   - Worker streams execution events (`STDOUT`, `STDERR`, `DUMP_OBJECT`, `EXECUTION_COMPLETE`).
   - UI results view consumes the event stream and incrementally renders tabular/tree representations.

---

## 5. UI Architecture: MVVM / MVI Pattern

Each UI Slice follows strict Unidirectional Data Flow:

```
[ User Interaction ] ──( Intent / Event )──▶ [ ViewModel ]
                                                   │
                                            ( Runs Use Case )
                                                   │
[ Compose View ] ◀────( StateFlow<UiState> )───────┘
```

### State Design Rule
- State is **immutable** data classes.
- UI components are pure and reactive (`@Composable`).
- Long-running operations (compilation, networking, IPC) run on background Coroutine dispatchers (`Dispatchers.IO`), never on the Compose Desktop Swing EDT.

---

## 6. Implementation Checklist & Recommended Milestones

### Phase 1: Core Foundation (MVP)
- [ ] Define shared IPC contracts (`ExecutionPayload`, `DumpPayload`).
- [ ] Implement in-memory `JavaCompiler` wrapper for simple Java statements and classes.
- [ ] Build isolated `WorkerProcess` runner that executes compiled bytecode and captures stdout.
- [ ] Basic SplitPane UI (Left: Code Editor, Right: Raw Output Console).

### Phase 2: Rich Inspection (`.dump()`)
- [ ] Reflection-based object introspector in `worker-runtime` supporting:
  - Primitive values and strings.
  - Iterables / Collections (rendered as tabular grids).
  - POJOs / Records (rendered as property trees with depth limits).
  - Dictionaries / Maps (key-value tables).
- [ ] Interactive Compose UI Tree/Table component with collapsible nodes.

### Phase 3: Developer Ergonomics
- [ ] Dynamic Maven dependency downloader & classpath injector.
- [ ] Syntax highlighting and basic code completion for Java/Kotlin.
- [ ] Multi-tab snippet manager and scratchpad persistence (`.jpad` files).
- [ ] Database scratchpad: JDBC explorer and SQL snippet executor.
