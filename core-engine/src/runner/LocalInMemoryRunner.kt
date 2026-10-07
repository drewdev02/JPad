package org.andrew.jpad.core.runner

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException

class LocalInMemoryRunner : ScriptRunner {

    override fun run(
        compiledClasses: Map<String, ByteArray>,
        mainClassName: String,
        args: Array<String>
    ): ExecutionOutput {
        val classLoader = MemoryClassLoader(compiledClasses, javaClass.classLoader)
        val originalOut = System.out
        val originalErr = System.err

        val stdoutStream = ByteArrayOutputStream()
        val stderrStream = ByteArrayOutputStream()

        val capturedOut = PrintStream(stdoutStream, true, Charsets.UTF_8)
        val capturedErr = PrintStream(stderrStream, true, Charsets.UTF_8)

        var exitCode = 0
        var caughtThrowable: Throwable? = null

        try {
            System.setOut(capturedOut)
            System.setErr(capturedErr)

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
            System.setOut(originalOut)
            System.setErr(originalErr)
            capturedOut.flush()
            capturedErr.flush()
        }

        return ExecutionOutput(
            stdout = stdoutStream.toString(Charsets.UTF_8),
            stderr = stderrStream.toString(Charsets.UTF_8),
            exitCode = exitCode,
            throwable = caughtThrowable
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
