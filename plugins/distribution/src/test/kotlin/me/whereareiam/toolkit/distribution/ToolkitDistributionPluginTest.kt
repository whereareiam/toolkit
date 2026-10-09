package me.whereareiam.toolkit.distribution

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class ToolkitDistributionPluginTest {

    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `collects files and archives under their final names`() {
        writeProject(
            """
            toolkitDistribution {
                file('API', ':api', 'jar')
                archive('PLATFORMS') {
                    file('VELOCITY', ':velocity', 'jar')
                    file('BUNGEECORD', ':bungeecord', 'jar')
                }
            }
            """
        )

        runner().withArguments("assembleDistribution", "--configuration-cache").build()

        assertEquals(listOf("Sample-API-1.2.3.jar", "Sample-PLATFORMS-1.2.3.zip"), distribution())
        ZipFile(projectDir.resolve("build/distribution/Sample-PLATFORMS-1.2.3.zip").toFile()).use { zip ->
            assertEquals(
                listOf("Sample-BUNGEECORD-1.2.3.jar", "Sample-VELOCITY-1.2.3.jar"),
                zip.entries().toList().map { it.name }
            )
        }
    }

    @Test
    fun `removes files of an earlier version`() {
        writeProject("toolkitDistribution { file('API', ':api', 'jar') }")

        runner().withArguments("assembleDistribution").build()
        runner().withArguments("assembleDistribution", "-Pversion=2.0.0").build()

        assertEquals(listOf("Sample-API-2.0.0.jar"), distribution())
    }

    @Test
    fun `uses the configured name and directory`() {
        writeProject(
            """
            toolkitDistribution {
                name = 'Other'
                directory = layout.buildDirectory.dir('out')
                file('API', project(':api').tasks.named('jar'))
            }
            """,
            evaluateChildrenFirst = true
        )

        runner().withArguments("assembleDistribution").build()

        assertEquals(listOf("Other-API-1.2.3.jar"), projectDir.resolve("build/out").listDirectoryEntries().map { it.name })
    }

    @Test
    fun `rejects two files with the same name`() {
        writeProject(
            """
            toolkitDistribution {
                file('API', ':api', 'jar')
                file('API', ':velocity', 'jar')
            }
            """
        )

        val result = runner().withArguments("assembleDistribution").buildAndFail()

        assertTrue(result.output.contains("Duplicate files in the distribution"))
    }

    @Test
    fun `rejects a distribution without files`() {
        writeProject("")

        val result = runner().withArguments("assembleDistribution").buildAndFail()

        assertTrue(result.output.contains("toolkitDistribution declares no files"))
    }

    private fun distribution(): List<String> =
        projectDir.resolve("build/distribution").listDirectoryEntries().map { it.name }.sorted()

    private fun writeProject(configuration: String, evaluateChildrenFirst: Boolean = false) {
        listOf("api", "velocity", "bungeecord").forEach { Files.createDirectories(projectDir.resolve(it)) }
        Files.writeString(
            projectDir.resolve("settings.gradle"),
            "rootProject.name = 'Sample'\ninclude 'api', 'velocity', 'bungeecord'\n"
        )
        Files.writeString(
            projectDir.resolve("build.gradle"),
            """
            plugins {
                id 'me.whereareiam.toolkit.distribution'
            }

            allprojects {
                version = providers.gradleProperty('version').getOrElse('1.2.3')
            }
            subprojects {
                apply plugin: 'java'
            }
            ${if (evaluateChildrenFirst) "evaluationDependsOnChildren()" else ""}

            ${configuration.trimIndent()}
            """.trimIndent()
        )
    }

    private fun runner(): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
}
