package me.whereareiam.toolkit.versioning

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ToolkitVersioningPluginTest {

    @TempDir
    lateinit var projectDir: Path

    @Test
    fun `sets project version from VERSION environment variable`() {
        writeBuild(
            """
            plugins {
                id 'me.whereareiam.toolkit.versioning'
            }

            tasks.register('printVersioning') {
                doLast {
                    println("version=${'$'}{project.version}")
                    println("channel=${'$'}{toolkitVersioning.resolvedChannel().get()}")
                }
            }
            """.trimIndent()
        )

        val result = runner(mapOf("VERSION" to "1.2.3")).withArguments("printVersioning").build()

        assertTrue(result.output.contains("version=1.2.3"))
        assertTrue(result.output.contains("channel=release"))
    }

    @Test
    fun `falls back to dev when VERSION is absent`() {
        writeBuild(
            """
            plugins {
                id 'me.whereareiam.toolkit.versioning'
            }

            tasks.register('printVersioning') {
                doLast {
                    println("version=${'$'}{project.version}")
                    println("channel=${'$'}{toolkitVersioning.resolvedChannel().get()}")
                }
            }
            """.trimIndent()
        )

        val result = runner(emptyMap()).withArguments("printVersioning").build()

        assertTrue(result.output.contains("version=dev"))
        assertTrue(result.output.contains("channel=development"))
    }

    @Test
    fun `classifies semver prerelease versions as release`() {
        writeBuild(
            """
            plugins {
                id 'me.whereareiam.toolkit.versioning'
            }

            tasks.register('printVersioning') {
                doLast {
                    println("channel=${'$'}{toolkitVersioning.resolvedChannel().get()}")
                }
            }
            """.trimIndent()
        )

        val result = runner(mapOf("VERSION" to "v1.2.3-beta.2")).withArguments("printVersioning").build()

        assertTrue(result.output.contains("channel=release"))
    }

    @Test
    fun `respects custom environment variable default version and channel override`() {
        writeBuild(
            """
            plugins {
                id 'me.whereareiam.toolkit.versioning'
            }

            toolkitVersioning {
                channelOverride.set('release')
                defaultVersion.set('fallback')
                environmentVariableName.set('CUSTOM_VERSION')
            }

            tasks.register('printVersioning') {
                doLast {
                    println("version=${'$'}{project.version}")
                    println("channel=${'$'}{toolkitVersioning.resolvedChannel().get()}")
                }
            }
            """.trimIndent()
        )

        val fallbackResult = runner(emptyMap()).withArguments("printVersioning").build()
        val customEnvResult = runner(mapOf("CUSTOM_VERSION" to "branch-abcdef1")).withArguments("printVersioning").build()

        assertTrue(fallbackResult.output.contains("version=fallback"))
        assertTrue(fallbackResult.output.contains("channel=release"))
        assertTrue(customEnvResult.output.contains("version=branch-abcdef1"))
        assertTrue(customEnvResult.output.contains("channel=release"))
    }

    private fun runner(environment: Map<String, String>): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withEnvironment(environment)
            .forwardOutput()

    private fun writeBuild(buildScript: String) {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'test-project'\n")
        Files.writeString(projectDir.resolve("build.gradle"), buildScript)
    }
}

