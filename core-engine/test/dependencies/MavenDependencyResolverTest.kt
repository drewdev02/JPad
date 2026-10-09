package org.andrew.jpad.core.dependencies

import org.andrew.jpad.core.ExecutionEngine
import org.andrew.jpad.core.compiler.JavaCompilerService
import org.andrew.jpad.core.dump.ObjectDumper
import org.andrew.jpad.core.parser.SnippetWrapper
import org.andrew.jpad.core.runner.LocalInMemoryRunner
import org.andrew.jpad.core.runner.JShellRunner
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MavenDependencyResolverTest {

    private val resolver = MavenDependencyResolver()

    @Test
    fun `parses coordinate strings correctly`() {
        val coord = MavenCoordinate.parse("com.google.guava:guava:33.0.0-jre")
        assertEquals(MavenCoordinate("com.google.guava", "guava", "33.0.0-jre"), coord)
        assertEquals("com/google/guava/guava/33.0.0-jre/guava-33.0.0-jre.jar", coord?.relativeJarPath)
        assertEquals("com/google/guava/guava/33.0.0-jre/guava-33.0.0-jre.pom", coord?.relativePomPath)
        assertEquals("com.google.guava:guava:33.0.0-jre", coord.toString())

        assertNull(MavenCoordinate.parse("invalid"))
        assertNull(MavenCoordinate.parse("group:artifact"))
        assertNull(MavenCoordinate.parse(" : : "))
    }

    @Test
    fun `extracts dependencies from comments`() {
        val script = """
            // @deps com.google.guava:guava:33.0.0-jre
            // deps: org.apache.commons:commons-lang3:3.14.0, com.google.code.gson:gson:2.10.1
            # @deps com.squareup.okhttp3:okhttp:4.12.0
            
            var x = 10;
        """.trimIndent()

        val deps = resolver.extractDependencies(script)
        assertEquals(4, deps.size)
        assertEquals("com.google.guava", deps[0].groupId)
        assertEquals("guava", deps[0].artifactId)
        assertEquals("33.0.0-jre", deps[0].version)

        assertEquals("org.apache.commons", deps[1].groupId)
        assertEquals("commons-lang3", deps[1].artifactId)

        assertEquals("com.google.code.gson", deps[2].groupId)
        assertEquals("gson", deps[2].artifactId)

        assertEquals("com.squareup.okhttp3", deps[3].groupId)
        assertEquals("okhttp", deps[3].artifactId)
    }

    @Test
    fun `extractDependencies ignores duplicate coordinates`() {
        val script = """
            // @deps com.google.guava:guava:33.0.0-jre
            // @deps com.google.guava:guava:33.0.0-jre
        """.trimIndent()

        val deps = resolver.extractDependencies(script)
        assertEquals(1, deps.size)
    }

    @Test
    fun `resolves jar from local cache directory`() {
        val tempCache = Files.createTempDirectory("jpad_cache_test").toFile()
        try {
            val coord = MavenCoordinate("test.group", "test-artifact", "1.0.0")
            val jarFile = File(tempCache, coord.relativeJarPath)
            jarFile.parentFile.mkdirs()
            jarFile.writeText("fake-jar-content")

            val customResolver = MavenDependencyResolver(
                localCacheDir = tempCache,
                m2RepoDir = File(tempCache, "nonexistent-m2"),
                remoteRepos = emptyList()
            )

            val result = customResolver.resolve(listOf(coord))
            assertTrue(result.isSuccess)
            assertEquals(1, result.classpathJars.size)
            assertEquals(jarFile.absolutePath, result.classpathJars[0].absolutePath)
        } finally {
            tempCache.deleteRecursively()
        }
    }

    @Test
    fun `returns failure when artifact cannot be found`() {
        val tempCache = Files.createTempDirectory("jpad_cache_test_empty").toFile()
        try {
            val customResolver = MavenDependencyResolver(
                localCacheDir = tempCache,
                m2RepoDir = tempCache,
                remoteRepos = emptyList() // No remote repo to download from
            )

            val coord = MavenCoordinate("unknown", "unknown", "1.0.0")
            val result = customResolver.resolve(listOf(coord))
            assertFalse(result.isSuccess)
            assertTrue(result.errorMessage?.contains("Artifact not found") == true)
        } finally {
            tempCache.deleteRecursively()
        }
    }

    @Test
    fun `ExecutionEngine fails fast if dependency resolution fails`() {
        val failingResolver = object : DependencyResolver {
            override fun extractDependencies(sourceCode: String): List<MavenCoordinate> {
                return listOf(MavenCoordinate("fail.group", "fail-artifact", "1.0.0"))
            }

            override fun resolve(coordinates: List<MavenCoordinate>): ResolutionResult {
                return ResolutionResult(isSuccess = false, errorMessage = "Connection timed out")
            }
        }

        val dumper = ObjectDumper()
        val compiler = org.andrew.jpad.core.compiler.InMemoryJavaCompiler()
        val wrapper = SnippetWrapper()
        val scriptRunner = LocalInMemoryRunner(dumper)
        val snippetRunner = JShellRunner(dumper)

        val engine = ExecutionEngine(
            compiler = compiler,
            wrapper = wrapper,
            runner = scriptRunner,
            snippetRunner = snippetRunner,
            dependencyResolver = failingResolver
        )

        val response = engine.execute("// @deps fail.group:fail-artifact:1.0.0\nSystem.out.println(1);")
        assertFalse(response.isSuccess)
        assertTrue(response.error.contains("Dependency resolution failed"))
        assertTrue(response.error.contains("Connection timed out"))
    }
}
