package org.andrew.jpad.core.runner

import org.andrew.jpad.core.dump.DumpValue
import org.andrew.jpad.core.dump.ObjectDumper
import org.andrew.jpad.runtime.JPad
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader

class LocalInMemoryRunner(
    private val objectDumper: ObjectDumper
) : ScriptRunner {

    override fun run(
        compiledClasses: Map<String, ByteArray>,
        mainClassName: String,
        args: Array<String>,
        classpath: List<File>
    ): ExecutionOutput {
        val parentLoader = if (classpath.isNotEmpty()) {
            URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)
        } else {
            javaClass.classLoader
        }
        val classLoader = MemoryClassLoader(compiledClasses, parentLoader)
        val originalOut = System.out
        val originalErr = System.err

        val stdoutStream = ByteArrayOutputStream()
        val stderrStream = ByteArrayOutputStream()

        val capturedOut = PrintStream(stdoutStream, true, Charsets.UTF_8)
        val capturedErr = PrintStream(stderrStream, true, Charsets.UTF_8)

        var exitCode = 0
        var caughtThrowable: Throwable? = null
        val dumps = mutableListOf<DumpValue>()

        try {
            System.setOut(capturedOut)
            System.setErr(capturedErr)
            JPad.setDumpConsumer { obj, title ->
                dumps.add(objectDumper.dump(obj, title))
            }

            val loadedClass = classLoader.loadClass(mainClassName)
            val mainMethod = loadedClass.getMethod("main", Array<String>::class.java)
            mainMethod.invoke(null, args)
        } catch (ite: InvocationTargetException) {
            exitCode = 1
            caughtThrowable = ite.targetException ?: ite
            caughtThrowable.printStackTrace(capturedErr)
        } catch (t: Throwable) {
            exitCode = 1
            caughtThrowable = t
            t.printStackTrace(capturedErr)
        } finally {
            JPad.setDumpConsumer(null)
            System.setOut(originalOut)
            System.setErr(originalErr)
            capturedOut.flush()
            capturedErr.flush()
        }

        return ExecutionOutput(
            stdout = stdoutStream.toString(Charsets.UTF_8),
            stderr = stderrStream.toString(Charsets.UTF_8),
            exitCode = exitCode,
            throwable = caughtThrowable,
            dumps = dumps
        )
    }

    private class MemoryClassLoader(
        private val classes: Map<String, ByteArray>,
        parent: ClassLoader
    ) : ClassLoader(parent) {
        override fun findClass(name: String): Class<*> {
            val bytes = classes[name] ?: return super.findClass(name)
            return defineClass(name, bytes, 0, bytes.size)
        }
    }
}
