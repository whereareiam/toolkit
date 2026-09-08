package me.whereareiam.toolkit.architecture

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ToolkitArchitecturePluginTest {

	@TempDir
	lateinit var projectDir: Path

	@Test
	fun `nested implementations use their own API and the case insensitive shared root API`() {
		project(
			":anvil-api" to "",
			":environment:java" to "dependencies { implementation project(':environment:java:java-api') }",
			":environment:java:java-api" to "dependencies { api project(':anvil-api') }",
			":environment:java:vendor:adapter" to """
				dependencies {
					compileOnly project(':environment:java:java-api')
					implementation project(':anvil-api')
				}
			""".trimIndent(),
			rootName = "Anvil"
		)

		assertVerified(verify())
	}

	@Test
	fun `same family APIs may reexport through api and configurations inherited by api`() {
		project(
			":api" to "",
			":feature:base-api" to "dependencies { api project(':api') }",
			":feature:extended-api" to """
				configurations {
					publicContracts
					api.extendsFrom(publicContracts)
				}
				dependencies { publicContracts project(':feature:base-api') }
			""".trimIndent(),
			":feature:implementation" to "dependencies { implementation project(':feature:extended-api') }"
		)

		assertVerified(verify())
	}

	@Test
	fun `grouping ancestors do not permit foreign API dependencies or reexports`() {
		project(
			":environment:cache:cache-api" to "",
			":environment:java:java-api" to "dependencies { api project(':environment:cache:cache-api') }",
			":environment:java" to "dependencies { implementation project(':environment:cache:cache-api') }"
		)

		val result = reject()

		assertDiagnostic(result, "family", ":environment:java:java-api", ":environment:java:implementation", ":environment:cache:cache-api")
	}

	@Test
	fun `arbitrary top level APIs are not shared root APIs`() {
		project(
			":other-api" to "",
			":consumer" to "dependencies { implementation project(':other-api') }"
		)

		assertDiagnostic(reject(), "family", ":consumer", ":other-api")
	}

	@Test
	fun `shared root API cannot depend on a project`() {
		project(
			":anvil-api" to "dependencies { api project(':feature:feature-api') }",
			":feature:feature-api" to ""
		)

		assertDiagnostic(reject(), ":anvil-api", "must not depend", ":feature:feature-api")
	}

	@Test
	fun `API dependencies must be exposed through api`() {
		project(
			":feature:base-api" to "",
			":feature:extended-api" to "dependencies { compileOnlyApi project(':feature:base-api') }"
		)

		assertDiagnostic(reject(), ":feature:extended-api", "api", "compileOnlyApi")
	}

	@Test
	fun `explicit family joins flat modules and propagates from an owning module`() {
		project(
			":contract-api" to "architecture { family = 'shared-feature' }",
			":flat-implementation" to """
				architecture { family = 'shared-feature' }
				dependencies { implementation project(':contract-api') }
			""".trimIndent(),
			":feature" to "architecture { family = 'shared-feature' }",
			":feature:feature-api" to "dependencies { api project(':contract-api') }",
			":feature:nested:adapter" to "dependencies { implementation project(':contract-api') }"
		)

		assertVerified(verify())
	}

	@Test
	fun `explicit root API can use a nested custom name and replace inferred root`() {
		project(
			":anvil-api" to "architecture { rootApi = false }",
			":contracts:shared" to "architecture { kind = api; rootApi = true }",
			":consumer" to "dependencies { implementation project(':contracts:shared') }"
		)

		assertVerified(verify())
	}

	@Test
	fun `multiple shared root APIs are rejected`() {
		project(
			":anvil-api" to "",
			":contracts:shared-api" to "architecture { rootApi = true }"
		)

		assertDiagnostic(reject(), "root", ":anvil-api", ":contracts:shared-api")
	}

	@Test
	fun `implementation cannot declare itself as shared root API`() {
		project(":service" to "architecture { kind = implementation; rootApi = true }")

		assertDiagnostic(reject(), "root", ":service", "API")
	}

	@Test
	fun `root plugin checks modules without their own plugin application`() {
		project(
			":feature:feature-api" to "",
			":other:implementation" to "dependencies { implementation project(':feature:feature-api') }",
			modulePlugin = false
		)

		assertDiagnostic(reject(), "family", ":other:implementation", ":feature:feature-api")
	}

	@Test
	fun `embedded compileOnlyApi and inherited production dependencies cannot bypass family checks`() {
		project(
			":foreign:foreign-api" to "",
			":feature:feature-api" to "",
			":feature:adapter" to """
				configurations {
					embedded
					borrowed
					intermediate
					intermediate.extendsFrom(borrowed)
					implementation.extendsFrom(intermediate)
				}
				dependencies {
					embedded project(':foreign:foreign-api')
					compileOnlyApi project(':foreign:foreign-api')
					borrowed project(':foreign:foreign-api')
				}
			""".trimIndent()
		)

		assertDiagnostic(reject(), "family", ":feature:adapter", ":foreign:foreign-api", "embedded", "compileOnlyApi", "borrowed")
	}

	@Test
	fun `test and test fixture dependencies are excluded unless inherited into production`() {
		project(
			":foreign:implementation" to "",
			":consumer" to """
				apply plugin: 'java-test-fixtures'
				dependencies {
					testImplementation project(':foreign:implementation')
					testFixturesImplementation project(':foreign:implementation')
				}
			""".trimIndent()
		)

		assertVerified(verify())
		append(":consumer", "configurations.implementation.extendsFrom(configurations.testFixturesImplementation)")

		assertDiagnostic(reject(), ":consumer", ":foreign:implementation", "testFixturesImplementation")
	}

	@Test
	fun `same family implementation dependencies remain prohibited`() {
		project(
			":feature:feature-api" to "dependencies { api project(':feature:implementation') }",
			":feature:implementation" to "",
			":feature:adapter" to "dependencies { implementation project(':feature:implementation') }"
		)

		assertDiagnostic(reject(), ":feature:feature-api", ":feature:adapter", ":feature:implementation", "API")
	}

	@Test
	fun `explicit and source free assemblies may compose multiple families`() {
		project(
			":first:first-api" to "",
			":second:implementation" to "",
			":explicit" to """
				architecture { kind = assembly }
				dependencies {
					implementation project(':first:first-api')
					implementation project(':second:implementation')
				}
			""".trimIndent(),
			":bundle" to "dependencies { implementation project(':second:implementation') }",
			sourceFree = setOf(":bundle")
		)

		assertVerified(verify())
	}

	@Test
	fun `custom main source roots remain implementations before generated sources exist`() {
		project(
			":foreign:foreign-api" to "",
			":consumer" to """
				sourceSets.main.java.setSrcDirs(['generated/main'])
				dependencies { implementation project(':foreign:foreign-api') }
			""".trimIndent(),
			sourceFree = setOf(":consumer")
		)

		assertTrue(Files.notExists(moduleDirectory(":consumer").resolve("src/main")))
		assertTrue(Files.notExists(moduleDirectory(":consumer").resolve("generated/main")))
		assertDiagnostic(reject(), "family", ":consumer", ":foreign:foreign-api")
	}

	@Test
	fun `adding main sources invalidates cached source free assembly classification`() {
		project(
			":foreign:foreign-api" to "",
			":consumer" to "dependencies { implementation project(':foreign:foreign-api') }",
			sourceFree = setOf(":consumer")
		)

		assertVerified(verify())
		val reused = verify()
		assertVerified(reused)
		assertDiagnostic(reused, "Reusing configuration cache")
		Files.createDirectories(moduleDirectory(":consumer").resolve("src/main/java"))

		assertDiagnostic(reject(), "family", ":consumer", ":foreign:foreign-api")
	}

	@Test
	fun `main source producer establishes implementation role without executing the producer`() {
		project(
			":foreign:foreign-api" to "",
			":consumer" to """
				def generateSources = tasks.register('generateSources') {
					outputs.dir(layout.projectDirectory.dir('src/main/java'))
					doLast { throw new GradleException('Source producer must not run during architecture verification') }
				}
				sourceSets.main.java.srcDir(generateSources)
				dependencies { implementation project(':foreign:foreign-api') }
			""".trimIndent(),
			sourceFree = setOf(":consumer")
		)

		assertTrue(Files.notExists(moduleDirectory(":consumer").resolve("src/main")))
		val result = reject()
		assertNull(result.task(":consumer:generateSources"), result.output)
		assertDiagnostic(result, "family", ":consumer", ":foreign:foreign-api")
	}

	@Test
	fun `default named module with main sources has no implicit assembly exemption`() {
		project(
			":foreign:foreign-api" to "",
			":feature:default" to "dependencies { implementation project(':foreign:foreign-api') }"
		)

		assertDiagnostic(reject(), "family", ":feature:default", ":foreign:foreign-api")
	}

	@Test
	fun `classpath and published element hierarchies cannot hide foreign API dependencies`() {
		project(
			":foreign:foreign-api" to "",
			":consumer" to """
				configurations {
					compileBorrowed
					runtimeBorrowed
					apiBorrowed
					elementsBorrowed
					compileClasspath.extendsFrom(compileBorrowed)
					runtimeClasspath.extendsFrom(runtimeBorrowed)
					apiElements.extendsFrom(apiBorrowed)
					runtimeElements.extendsFrom(elementsBorrowed)
				}
				dependencies {
					compileBorrowed project(':foreign:foreign-api')
					runtimeBorrowed project(':foreign:foreign-api')
					apiBorrowed project(':foreign:foreign-api')
					elementsBorrowed project(':foreign:foreign-api')
				}
			""".trimIndent()
		)

		assertDiagnostic(reject(), "family", ":consumer", ":foreign:foreign-api",
			"compileBorrowed", "runtimeBorrowed", "apiBorrowed", "elementsBorrowed")
	}

	@Test
	fun `same family API dependencies published only through apiElements must be declared through api`() {
		project(
			":feature:base-api" to "",
			":feature:extended-api" to """
				configurations {
					publishedOnly
					apiElements.extendsFrom(publishedOnly)
				}
				dependencies { publishedOnly project(':feature:base-api') }
			""".trimIndent()
		)

		assertDiagnostic(reject(), ":feature:extended-api", ":feature:base-api", "through api", "publishedOnly")
	}

	@Test
	fun `configuration cache reuses verification and detects changed dependencies`() {
		project(
			":anvil-api" to "",
			":foreign:foreign-api" to "",
			":consumer" to "dependencies { implementation project(':anvil-api') }"
		)

		assertVerified(verify())
		val reused = verify()
		assertVerified(reused)
		assertDiagnostic(reused, "Reusing configuration cache")
		append(":consumer", "dependencies { runtimeOnly project(':foreign:foreign-api') }")

		assertDiagnostic(reject(), "family", ":consumer", ":foreign:foreign-api")
	}

	private fun project(
		vararg modules: Pair<String, String>,
		rootName: String = "anvil",
		modulePlugin: Boolean = true,
		sourceFree: Set<String> = emptySet()
	) {
		Files.writeString(
			projectDir.resolve("settings.gradle"),
			"rootProject.name = '$rootName'\n" + modules.joinToString("\n") { "include '${it.first}'" }
		)
		Files.writeString(
			projectDir.resolve("build.gradle"),
			"plugins { id 'me.whereareiam.toolkit.architecture' }\n"
		)
		modules.forEach { (path, script) ->
			val directory = moduleDirectory(path)
			Files.createDirectories(directory)
			if (path !in sourceFree)
				Files.createDirectories(directory.resolve("src/main/java"))
			val architecturePlugin = if (modulePlugin) "id 'me.whereareiam.toolkit.architecture'" else ""
			Files.writeString(directory.resolve("build.gradle"), """
				plugins {
					id 'java-library'
					$architecturePlugin
				}
				$script
			""".trimIndent() + "\n")
		}
	}

	private fun moduleDirectory(path: String): Path = projectDir.resolve(path.trimStart(':').replace(':', '/'))

	private fun append(path: String, script: String) {
		val build = moduleDirectory(path).resolve("build.gradle")
		Files.writeString(build, Files.readString(build) + "\n$script\n")
	}

	private fun runner(): GradleRunner = GradleRunner.create()
		.withProjectDir(projectDir.toFile())
		.withPluginClasspath()
		.withArguments("verifyArchitecture", "--configuration-cache", "--stacktrace")

	private fun verify(): BuildResult = runner().build()

	private fun reject(): BuildResult = runner().buildAndFail()

	private fun assertVerified(result: BuildResult) {
		assertEquals(TaskOutcome.SUCCESS, result.task(":verifyArchitecture")?.outcome, result.output)
	}

	private fun assertDiagnostic(result: BuildResult, vararg fragments: String) {
		fragments.forEach { fragment ->
			assertTrue(result.output.contains(fragment, ignoreCase = true), "Expected '$fragment' in:\n${result.output}")
		}
	}
}
