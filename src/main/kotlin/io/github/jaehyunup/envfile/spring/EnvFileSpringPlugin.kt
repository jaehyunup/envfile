package io.github.jaehyunup.envfile.spring

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.jaehyunup.envfile.spring.enums.EnvFileStyle
import io.github.jaehyunup.envfile.extensions.onlyMissingEnv
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test

class EnvFileSpringPlugin : Plugin<Project> {
    private val objectMapper = ObjectMapper()
    private val logger = Logging.getLogger(EnvFileSpringPlugin::class.java)

    override fun apply(project: Project) {
        val detected = detectEnvFile(project)
        val envFileStyle = detected?.second
        val env = if (detected == null) emptyMap() else loadEnvFile(detected.first, detected.second)
        val injectEnv = if (isSystemEnvOverrideable(project)) env else env.onlyMissingEnv()

        logger.info(
            "[envfile] file={}, style={}, override={}, rootDir={}",
            detected?.first?.name ?: "<none>",
            envFileStyle ?: "<none>",
            isSystemEnvOverrideable(project),
            project.rootDir
        )

        // Apply to root + all subprojects no matter where this plugin is applied
        project.rootProject.allprojects { p ->
            p.tasks.withType(JavaExec::class.java).configureEach {
                if (shouldApplyToJavaExec(it, project)) {
                    logger.debug("[envfileSpring] applying env to JavaExec task: {}", it.name)
                    it.environment(injectEnv)
                }
            }
            p.tasks.withType(Test::class.java).configureEach {
                logger.debug("[envfile] applying env to Test task: {}", it.name)
                it.environment(injectEnv)
            }
        }
    }

    private fun detectEnvFile(project: Project): Pair<File, EnvFileStyle>? {
        val root = project.rootDir

        // Priority: .env.local > .env > .env.local.json > .env.json (prefer .env.local over .env)
        val candidates = listOf(
            File(root, ".env.local"),
            File(root, ".env"),
            File(root, ".env.local.json"),
            File(root, ".env.json")
        )

        val file = candidates.firstOrNull { it.exists() && it.isFile }
        if (file == null) {
            logger.info("[envfile] no env file found in rootDir={}, candidates={}", root, candidates.map { it.name })
            return null
        }

        val style = if (file.name.endsWith(".json")) EnvFileStyle.JSON else EnvFileStyle.DOTENV
        logger.info("[envfile] detected env file: {} (style={})", file.name, style)
        return file to style
    }

    private fun isSystemEnvOverrideable(project: Project): Boolean {
        val raw =
            project.findProperty("envFileOverride") as String?
                ?: System.getProperty("envFileOverride")
                ?: System.getenv("ENV_FILE_OVERRIDE")
                ?: "false"

        return raw.equals("true", ignoreCase = true)
    }

    private fun loadEnvFile(file: File, mode: EnvFileStyle): Map<String, String> {
        return when (mode) {
            EnvFileStyle.DOTENV -> parseDotenvFile(file)
            EnvFileStyle.JSON -> parseJsonEnvFile(file)
        }
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

    private fun shouldApplyToJavaExec(task: JavaExec, project: Project): Boolean {
        val raw =
            project.findProperty("envFileApplyToAllJavaExec") as String?
                ?: System.getProperty("envFileApplyToAllJavaExec")
                ?: System.getenv("ENV_FILE_APPLY_TO_ALL_JAVAEXEC")
                ?: "false"

        val applyAll = raw.equals("true", ignoreCase = true)
        return applyAll || task.name == "bootRun"
    }
}