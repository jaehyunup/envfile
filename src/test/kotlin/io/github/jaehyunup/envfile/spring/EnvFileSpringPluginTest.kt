package io.github.jaehyunup.envfile.spring

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class EnvFileSpringPluginTest {

    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `default priority - json overrides dotenv`() {
        writeProjectScaffold(priority = null)

        Files.writeString(projectDir.resolve(".env"), "FOO=dotenv\n", StandardCharsets.UTF_8)
        Files.writeString(projectDir.resolve(".env.json"), "{\"FOO\":\"json\"}", StandardCharsets.UTF_8)

        val result = runner()
            .withArguments("printFoo", "--stacktrace")
            .build()

        assertTrue(
            result.output.contains("FOO=json"),
            "Expected output to contain 'FOO=json' but was:\n${result.output}"
        )
    }

    @Test
    fun `default priority - local json overrides everything`() {
        writeProjectScaffold(priority = null)

        Files.writeString(projectDir.resolve(".env"), "FOO=dotenv\n", StandardCharsets.UTF_8)
        Files.writeString(projectDir.resolve(".env.local"), "FOO=dotenv-local\n", StandardCharsets.UTF_8)
        Files.writeString(projectDir.resolve(".env.json"), "{\"FOO\":\"json\"}", StandardCharsets.UTF_8)
        Files.writeString(projectDir.resolve(".env.local.json"), "{\"FOO\":\"json-local\"}", StandardCharsets.UTF_8)

        val result = runner()
            .withArguments("printFoo", "--stacktrace")
            .build()

        // Order (default JSON): dotenv (.env -> .env.local) then json (.env.json -> .env.local.json)
        // Last file wins -> .env.local.json
        assertTrue(
            result.output.contains("FOO=json-local"),
            "Expected output to contain 'FOO=json-local' but was:\n${result.output}"
        )
    }

    @Test
    fun `dotenv priority - dotenv overrides json`() {
        writeProjectScaffold(priority = "DOTENV")

        Files.writeString(projectDir.resolve(".env"), "FOO=dotenv\n", StandardCharsets.UTF_8)
        Files.writeString(projectDir.resolve(".env.json"), "{\"FOO\":\"json\"}", StandardCharsets.UTF_8)

        val result = runner()
            .withArguments("printFoo", "--stacktrace")
            .build()

        assertTrue(
            result.output.contains("FOO=dotenv"),
            "Expected output to contain 'FOO=dotenv' but was:\n${result.output}"
        )
    }

    @Test
    fun `dotenv priority - local dotenv overrides everything`() {
        writeProjectScaffold(priority = "DOTENV")

        Files.writeString(projectDir.resolve(".env"), "FOO=dotenv\n", StandardCharsets.UTF_8)
        Files.writeString(projectDir.resolve(".env.local"), "FOO=dotenv-local\n", StandardCharsets.UTF_8)
        Files.writeString(projectDir.resolve(".env.json"), "{\"FOO\":\"json\"}", StandardCharsets.UTF_8)
        Files.writeString(projectDir.resolve(".env.local.json"), "{\"FOO\":\"json-local\"}", StandardCharsets.UTF_8)

        val result = runner()
            .withArguments("printFoo", "--stacktrace")
            .build()

        // Order (DOTENV): json (.env.json -> .env.local.json) then dotenv (.env -> .env.local)
        // Last file wins -> .env.local
        assertTrue(
            result.output.contains("FOO=dotenv-local"),
            "Expected output to contain 'FOO=dotenv-local' but was:\n${result.output}"
        )
    }

    @Test
    fun `chaos - mixed files - dotenv priority still yields deterministic merge`() {
        writeProjectScaffold(priority = "DOTENV")

        // Create a messy mix: all four files exist with overlapping + distinct keys.
        Files.writeString(
            projectDir.resolve(".env.json"),
            "{\"FOO\":\"json\",\"BAR\":\"json-bar\"}",
            StandardCharsets.UTF_8
        )
        Files.writeString(
            projectDir.resolve(".env.local.json"),
            "{\"FOO\":\"json-local\",\"BAZ\":\"json-local-baz\"}",
            StandardCharsets.UTF_8
        )
        Files.writeString(projectDir.resolve(".env"), "FOO=dotenv\nBAR=dotenv-bar\n", StandardCharsets.UTF_8)
        Files.writeString(
            projectDir.resolve(".env.local"),
            "FOO=dotenv-local\nQUX=dotenv-local-qux\n",
            StandardCharsets.UTF_8
        )

        val result = runner()
            .withArguments("printFoo", "--stacktrace")
            .build()

        // Priority=DOTENV => JSON files applied first, then dotenv files.
        // Within each style, .local overrides its non-local counterpart.
        // Therefore:
        // - FOO should end up from .env.local (dotenv-local)
        // - BAR should end up from .env (dotenv-bar)
        // - BAZ should come from .env.local.json (json-local-baz) (not overridden by dotenv)
        // - QUX should come from .env.local (dotenv-local-qux)
        assertTrue(result.output.contains("FOO=dotenv-local"), "Expected FOO=dotenv-local but was:\n${result.output}")
        assertTrue(result.output.contains("BAR=dotenv-bar"), "Expected BAR=dotenv-bar but was:\n${result.output}")
        assertTrue(
            result.output.contains("BAZ=json-local-baz"),
            "Expected BAZ=json-local-baz but was:\n${result.output}"
        )
        assertTrue(
            result.output.contains("QUX=dotenv-local-qux"),
            "Expected QUX=dotenv-local-qux but was:\n${result.output}"
        )
    }

    @Test
    fun `system env should not be overridden by env files`() {
        writeProjectScaffold(priority = null)

        // Create env files that try to override FOO
        Files.writeString(projectDir.resolve(".env"), "FOO=dotenv\n", StandardCharsets.UTF_8)
        Files.writeString(projectDir.resolve(".env.json"), "{\"FOO\":\"json\"}", StandardCharsets.UTF_8)

        // Simulate an existing system environment variable
        val result = runner()
            .withEnvironment(mapOf("FOO" to "system"))
            .withArguments("printFoo", "--stacktrace")
            .build()

        // Since plugin uses onlyMissingOnSystemEnv(), existing system env must win
        assertTrue(
            result.output.contains("FOO=system"),
            "Expected FOO=system but was:\n${result.output}"
        )
    }

    private fun runner(): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()

    private fun writeProjectScaffold(priority: String?) {
        // settings.gradle
        Files.writeString(
            projectDir.resolve("settings.gradle"),
            "rootProject.name = 'test-project'\n",
            StandardCharsets.UTF_8
        )

        // minimal java source that prints env vars FOO, BAR, BAZ, QUX
        val srcDir = projectDir.resolve("src/main/java/com/example")
        Files.createDirectories(srcDir)
        Files.writeString(
            srcDir.resolve("PrintEnv.java"),
            """
            package com.example;
            public class PrintEnv {
              private static void print(String key) {
                String v = System.getenv(key);
                System.out.println(key + "=" + (v == null ? "<null>" : v));
              }
              public static void main(String[] args) {
                print("FOO");
                print("BAR");
                print("BAZ");
                print("QUX");
              }
            }
            """.trimIndent() + "\n",
            StandardCharsets.UTF_8
        )

        // build.gradle for the temp project
        val build = StringBuilder()
        build.append(
            """
            import io.github.jaehyunup.envfile.spring.enums.EnvFileStyle

            plugins {
              id 'java'
              id 'io.github.jaehyunup.envfile-spring'
            }

            repositories { mavenCentral() }
            """.trimIndent() + "\n"
        )

        if (priority != null) {
            build.append(
                """
                envfileSpring {
                  priority.set(EnvFileStyle.${priority})
                }

                """.trimIndent() + "\n"
            )
        }

        build.append(
            """
            tasks.register('printFoo', JavaExec) {
              dependsOn classes
              classpath = sourceSets.main.runtimeClasspath
              mainClass = 'com.example.PrintEnv'
            }
            """.trimIndent() + "\n"
        )

        Files.writeString(projectDir.resolve("build.gradle"), build.toString(), StandardCharsets.UTF_8)
    }
}
