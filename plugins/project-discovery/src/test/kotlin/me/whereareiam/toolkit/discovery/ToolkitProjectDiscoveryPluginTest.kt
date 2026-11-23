package me.whereareiam.toolkit.discovery

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ToolkitProjectDiscoveryPluginTest {

	@TempDir
	lateinit var projectDir: Path

	@Test
	fun `includes every directory with a build script`() {
		writeSettings()
		project("app")
		project("libs/core")
		project("libs/core/core-api")

		assertEquals(listOf(":app", ":libs", ":libs:core", ":libs:core:core-api"), projects())
	}

	@Test
	fun `leaves out separate builds and everything below them`() {
		writeSettings()
		project("app")
		project("examples/demo")
		write("examples/demo/settings.gradle.kts", "")
		project("examples/demo/module")
		project("build-logic/conventions")
		project(".hidden/module")

		assertEquals(listOf(":app"), projects())
	}

	@Test
	fun `leaves out excluded directories and everything below them`() {
		writeSettings(
			"""
			projectDiscovery {
				exclude("tools/ide")
			}
			""".trimIndent()
		)
		project("app")
		project("tools/cli")
		project("tools/ide")
		project("tools/ide/ide-api")

		assertEquals(listOf(":app", ":tools", ":tools:cli"), projects())
	}

	@Test
	fun `generates type-safe accessors that agree with the included projects`() {
		writeSettings(
			"""
			enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

			if (providers.gradleProperty("withoutIde").isPresent)
				projectDiscovery { exclude("tools/ide") }
			""".trimIndent()
		)
		project("tools/cli")
		project("tools/ide")
		write("build.gradle.kts", "")
		// Gradle keeps generated accessors in its user home, shared by every build of the same project tree.
		runner("projects", "-PwithoutIde").build()
		write(
			"build.gradle.kts",
			"""
			tasks.register("printIde") {
				val path = projects.tools.ide.path
				doLast { println("ide=${'$'}path") }
			}
			""".trimIndent()
		)

		val result = runner("printIde").build()

		assertTrue(result.output.contains("ide=:tools:ide"), result.output)
	}

	@Test
	fun `fails for an exclusion that holds no discovered project`() {
		writeSettings(
			"""
			projectDiscovery {
				exclude("tools/missing")
			}
			""".trimIndent()
		)
		project("app")

		val result = runner("projects").buildAndFail()

		assertTrue(result.output.contains("projectDiscovery excludes tools/missing, which hold no discovered project"))
	}

	private fun projects(): List<String> {
		write(
			"build.gradle.kts",
			"""
			tasks.register("printProjects") {
				val paths = subprojects.map { it.path }.sorted()
				doLast { paths.forEach { println("project=${'$'}it") } }
			}
			""".trimIndent()
		)
		return runner("printProjects").build().output.lines()
			.filter { it.startsWith("project=") }
			.map { it.removePrefix("project=") }
	}

	private fun writeSettings(body: String = "") {
		write(
			"settings.gradle.kts",
			"""
			plugins {
				id("me.whereareiam.toolkit.project-discovery")
			}

			rootProject.name = "discovery-test"

			$body
			""".trimIndent()
		)
	}

	private fun project(directory: String) = write("$directory/build.gradle.kts", "")

	private fun write(relativePath: String, content: String) {
		val file = projectDir.resolve(relativePath)
		Files.createDirectories(file.parent)
		Files.writeString(file, content)
	}

	private fun runner(vararg arguments: String): GradleRunner = GradleRunner.create()
		.withProjectDir(projectDir.toFile())
		.withPluginClasspath()
		.withArguments(*arguments, "--stacktrace")
}
