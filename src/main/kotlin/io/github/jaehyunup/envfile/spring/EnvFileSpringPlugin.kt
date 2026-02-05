package io.github.jaehyunup.envfile.spring

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.jaehyunup.envfile.extensions.onlyMissingOnSystemEnv
import io.github.jaehyunup.envfile.spring.enums.EnvFileStyle
import io.github.jaehyunup.envfile.spring.gradle.EnvFileSpringGradleExtension
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test

class EnvFileSpringPlugin : Plugin<Project> {
    private val objectMapper = ObjectMapper()
    private val logger = Logging.getLogger(EnvFileSpringPlugin::class.java)

    // Default behavior: JSON files are loaded LAST, so JSON overrides DOTENV on conflicts.
    private var defaultPriority: EnvFileStyle = EnvFileStyle.JSON

    private data class EnvSource(
        val file: File,
        val style: EnvFileStyle,
    )

    override fun apply(project: Project) {
        val root = project.rootProject
        val ext =
            root.extensions.findByType(EnvFileSpringGradleExtension::class.java)
                ?: root.extensions.create("envfileSpring", EnvFileSpringGradleExtension::class.java)

        val detectedFiles = detectEnvFiles(project, ext)

        logDetection(project, detectedFiles, ext)

        val envMap = loadAndMergeEnv(detectedFiles)
        val injectEnv = envMap.onlyMissingOnSystemEnv()

        // Apply to root + all subprojects no matter where this plugin is applied
        project.rootProject.allprojects { p ->
            p.tasks.withType(JavaExec::class.java).configureEach {
                logger.debug("[envfileSpring] applying env to JavaExec task: {}", it.name)
                it.environment(injectEnv)
            }
            p.tasks.withType(Test::class.java).configureEach {
                logger.debug("[envfile] applying env to Test task: {}", it.name)
                it.environment(injectEnv)
            }
        }
    }

    private fun loadAndMergeEnv(detectedFiles: List<EnvSource>): Map<String, String> =
        detectedFiles
            .asSequence()
            .map { loadEnvFile(it.file, it.style) }
            // Later files overwrite earlier ones (last value wins)
            .fold(emptyMap()) { acc, next -> acc + next }

    private fun detectEnvFiles(project: Project, ext: EnvFileSpringGradleExtension): List<EnvSource> {
        val root = project.rootDir

        val priority: EnvFileStyle = if (ext.priority.isPresent) ext.priority.get() else defaultPriority

        val jsonCandidates = listOf(
            EnvSource(File(root, ".env.json"), EnvFileStyle.JSON),
            EnvSource(File(root, ".env.local.json"), EnvFileStyle.JSON),
        )

        val dotenvCandidates = listOf(
            EnvSource(File(root, ".env"), EnvFileStyle.DOTENV),
            EnvSource(File(root, ".env.local"), EnvFileStyle.DOTENV),
        )

        val orderedCandidates = when (priority) {
            EnvFileStyle.JSON -> dotenvCandidates + jsonCandidates
            EnvFileStyle.DOTENV -> jsonCandidates + dotenvCandidates
        }

        val files = orderedCandidates.filter { it.file.exists() && it.file.isFile }
        if (files.isEmpty()) {
            logger.info(
                "[envfile] no env file found in rootDir={}, candidates={}",
                root,
                orderedCandidates.map { it.file.name },
            )
            return emptyList()
        }

        return files
    }

    private fun parseDotenvFile(file: File): Map<String, String> =
        file.readLines(Charsets.UTF_8)
            .asSequence()
            .mapIndexed { idx, line ->
                // strip UTF-8 BOM if present on first line
                if (idx == 0 && line.isNotEmpty() && line[0] == '\uFEFF') line.substring(1) else line
            }
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                // accept `export KEY=value` and `export\tKEY=value`
                val cleaned = line.removePrefix("export ").removePrefix("export\t").trim()
                val idx = cleaned.indexOf('=')
                if (idx <= 0) return@mapNotNull null

                val key = cleaned.substring(0, idx).trim()
                var value = cleaned.substring(idx + 1)

                // allow empty values: KEY=
                value = value.trim()

                val quoted =
                    (value.startsWith('"') && value.endsWith('"')) || (value.startsWith('\'') && value.endsWith('\''))
                if (quoted) {
                    value = value.substring(1, value.length - 1)
                } else {
                    // strip inline comments for unquoted values: KEY=val # comment
                    val hash = value.indexOf('#')
                    if (hash >= 0) value = value.substring(0, hash).trimEnd()
                }

                if (key.isBlank()) return@mapNotNull null
                key to value
            }
            .toMap()

    private fun parseJsonEnvFile(file: File): Map<String, String> {
        val typeRef = object : TypeReference<Map<String, String>>() {}
        return objectMapper.readValue(file, typeRef)
    }

    private fun loadEnvFile(file: File, mode: EnvFileStyle): Map<String, String> {
        return when (mode) {
            EnvFileStyle.DOTENV -> parseDotenvFile(file)
            EnvFileStyle.JSON -> parseJsonEnvFile(file)
        }
    }

    private fun logDetection(project: Project, detectedFiles: List<EnvSource>, ext: EnvFileSpringGradleExtension) {
        logger.info(
            "[envfile] files={}, priority={}, rootDir={}",
            if (detectedFiles.isEmpty()) "<none>" else detectedFiles.joinToString(",") { it.file.name },
            if (ext.priority.isPresent) ext.priority.get() else defaultPriority,
            project.rootDir,
        )

        if (detectedFiles.isNotEmpty()) {
            logger.info(
                "[envfile] detected env files (in read order): {}",
                detectedFiles.joinToString(",") { "${it.file.name}:${it.style}" },
            )
        }
    }
}
