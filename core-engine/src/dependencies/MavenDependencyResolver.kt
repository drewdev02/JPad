package org.andrew.jpad.core.dependencies

import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

data class MavenCoordinate(
    val groupId: String,
    val artifactId: String,
    val version: String
) {
    val relativeJarPath: String
        get() = "${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.jar"

    val relativePomPath: String
        get() = "${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.pom"

    override fun toString(): String = "$groupId:$artifactId:$version"

    companion object {
        fun parse(coordinate: String): MavenCoordinate? {
            val parts = coordinate.trim().split(':')
            if (parts.size < 3) return null
            val g = parts[0].trim()
            val a = parts[1].trim()
            val v = parts[2].trim()
            if (g.isBlank() || a.isBlank() || v.isBlank()) return null
            return MavenCoordinate(g, a, v)
        }
    }
}

data class ResolutionResult(
    val isSuccess: Boolean,
    val classpathJars: List<File> = emptyList(),
    val errorMessage: String? = null
)

interface DependencyResolver {
    fun extractDependencies(sourceCode: String): List<MavenCoordinate>
    fun resolve(
        coordinates: List<MavenCoordinate>,
        onProgress: ((description: String, progress: Float?) -> Unit)? = null
    ): ResolutionResult
}

class MavenDependencyResolver(
    private val localCacheDir: File = File(System.getProperty("user.home"), ".jpad/cache/m2"),
    private val m2RepoDir: File = File(System.getProperty("user.home"), ".m2/repository"),
    private val remoteRepos: List<String> = listOf("https://repo1.maven.org/maven2/"),
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
) : DependencyResolver {

    override fun extractDependencies(sourceCode: String): List<MavenCoordinate> {
        val result = mutableListOf<MavenCoordinate>()
        for (line in sourceCode.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
                val clean = trimmed.removePrefix("//").removePrefix("#").trim()
                if (clean.startsWith("@deps", ignoreCase = true) || clean.startsWith("deps:", ignoreCase = true)) {
                    val content = clean.substringAfter("deps").removePrefix(":").trim()
                    val tokens = content.split(',', ';', ' ').map { it.trim() }.filter { it.isNotBlank() }
                    for (token in tokens) {
                        MavenCoordinate.parse(token)?.let { result.add(it) }
                    }
                }
            }
        }
        return result.distinct()
    }

    override fun resolve(
        coordinates: List<MavenCoordinate>,
        onProgress: ((description: String, progress: Float?) -> Unit)?
    ): ResolutionResult {
        if (coordinates.isEmpty()) {
            return ResolutionResult(isSuccess = true, classpathJars = emptyList())
        }

        onProgress?.invoke("Resolving ${coordinates.size} dependencies...", 0f)

        val resolvedFiles = mutableListOf<File>()
        val visitedCoordinates = mutableSetOf<String>()
        val queue = ArrayDeque<MavenCoordinate>()
        queue.addAll(coordinates)

        var processedCount = 0

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val coordKey = "${current.groupId}:${current.artifactId}"
            if (!visitedCoordinates.add(coordKey)) {
                continue
            }

            processedCount++
            val stepFraction = (processedCount.toFloat() / (processedCount + queue.size)).coerceIn(0.05f, 0.95f)
            onProgress?.invoke("Resolving ${current.artifactId}:${current.version}...", stepFraction)

            // 1. Locate or download JAR
            val jarFile = try {
                findOrDownloadArtifact(current.relativeJarPath, onProgress)
            } catch (e: Exception) {
                return ResolutionResult(
                    isSuccess = false,
                    errorMessage = "Failed to resolve dependency $current: ${e.message}"
                )
            }

            if (jarFile == null) {
                return ResolutionResult(
                    isSuccess = false,
                    errorMessage = "Artifact not found: $current in configured repositories."
                )
            }

            resolvedFiles.add(jarFile)

            // 2. Best-effort transitive dependencies from POM
            try {
                onProgress?.invoke("Checking POM for ${current.artifactId}...", stepFraction)
                val pomFile = findOrDownloadArtifact(current.relativePomPath, onProgress)
                if (pomFile != null && pomFile.exists()) {
                    val transitive = parsePomDependencies(pomFile, current.version)
                    for (trans in transitive) {
                        if (!visitedCoordinates.contains("${trans.groupId}:${trans.artifactId}")) {
                            queue.add(trans)
                        }
                    }
                }
            } catch (_: Exception) {
                // Transitive resolution is best-effort; don't fail primary artifact if POM parsing fails
            }
        }

        onProgress?.invoke("Resolved ${resolvedFiles.size} dependencies", 1.0f)
        return ResolutionResult(isSuccess = true, classpathJars = resolvedFiles.distinct())
    }

    private fun findOrDownloadArtifact(
        relativePath: String,
        onProgress: ((description: String, progress: Float?) -> Unit)? = null
    ): File? {
        val fileName = relativePath.substringAfterLast('/')

        // Check ~/.m2/repository
        val inM2 = File(m2RepoDir, relativePath)
        if (inM2.exists() && inM2.length() > 0) {
            onProgress?.invoke("Using cached $fileName", null)
            return inM2
        }

        // Check local cache ~/.jpad/cache/m2
        val inCache = File(localCacheDir, relativePath)
        if (inCache.exists() && inCache.length() > 0) {
            onProgress?.invoke("Using cached $fileName", null)
            return inCache
        }

        // Download from remote repos
        for (repo in remoteRepos) {
            val baseUrl = if (repo.endsWith("/")) repo else "$repo/"
            val url = URI.create("$baseUrl$relativePath")

            try {
                onProgress?.invoke("Downloading $fileName...", null)
                val request = HttpRequest.newBuilder(url)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
                if (response.statusCode() == 200) {
                    val contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L)
                    inCache.parentFile?.mkdirs()
                    val tempFile = File.createTempFile("jpad_dl_", ".tmp", inCache.parentFile)
                    
                    response.body().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead = 0L
                            var n: Int
                            var lastReportTime = 0L
                            while (input.read(buffer).also { n = it } != -1) {
                                output.write(buffer, 0, n)
                                bytesRead += n
                                val now = System.currentTimeMillis()
                                if (now - lastReportTime >= 50 || (contentLength > 0 && bytesRead == contentLength)) {
                                    lastReportTime = now
                                    val fraction = if (contentLength > 0) {
                                        (bytesRead.toFloat() / contentLength).coerceIn(0f, 1f)
                                    } else null
                                    val downloadedKb = bytesRead / 1024
                                    val totalKbText = if (contentLength > 0) " / ${contentLength / 1024} KB" else " KB"
                                    onProgress?.invoke("Downloading $fileName ($downloadedKb$totalKbText)...", fraction)
                                }
                            }
                        }
                    }
                    if (tempFile.length() > 0) {
                        if (inCache.exists()) inCache.delete()
                        tempFile.renameTo(inCache)
                        return inCache
                    } else {
                        tempFile.delete()
                    }
                }
            } catch (_: Exception) {
                // Try next repo
            }
        }

        return null
    }

    private fun parsePomDependencies(pomFile: File, parentVersion: String): List<MavenCoordinate> {
        val result = mutableListOf<MavenCoordinate>()
        val factory = DocumentBuilderFactory.newInstance()
        factory.isExpandEntityReferences = false
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        } catch (_: Exception) {}

        val doc = factory.newDocumentBuilder().parse(pomFile)
        val properties = mutableMapOf<String, String>()
        properties["project.version"] = parentVersion
        properties["version"] = parentVersion

        val propNodes = doc.getElementsByTagName("properties")
        if (propNodes.length > 0) {
            val propElement = propNodes.item(0) as? Element
            val children = propElement?.childNodes
            if (children != null) {
                for (i in 0 until children.length) {
                    val child = children.item(i)
                    if (child is Element) {
                        properties[child.tagName] = child.textContent.trim()
                    }
                }
            }
        }

        val depNodes = doc.getElementsByTagName("dependency")
        for (i in 0 until depNodes.length) {
            val dep = depNodes.item(i) as? Element ?: continue
            val scope = dep.getElementsByTagName("scope").item(0)?.textContent?.trim() ?: "compile"
            val optional = dep.getElementsByTagName("optional").item(0)?.textContent?.trim() ?: "false"

            if (scope == "test" || scope == "provided" || optional.equals("true", ignoreCase = true)) {
                continue
            }

            val g = dep.getElementsByTagName("groupId").item(0)?.textContent?.trim() ?: continue
            val a = dep.getElementsByTagName("artifactId").item(0)?.textContent?.trim() ?: continue
            var v = dep.getElementsByTagName("version").item(0)?.textContent?.trim() ?: ""

            if (v.startsWith("\${") && v.endsWith("}")) {
                val propKey = v.removePrefix("\${").removeSuffix("}")
                v = properties[propKey] ?: ""
            }

            if (v.isNotBlank()) {
                result.add(MavenCoordinate(g, a, v))
            }
        }

        return result
    }
}
