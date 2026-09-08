package me.whereareiam.toolkit.architecture

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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

	@Test
	fun `shared contracts permit separate client and server families and invalidate cached permissions`() {
		agentFamilies()

		assertVerified(verify())
		val reused = verify()
		assertVerified(reused)
		assertDiagnostic(reused, "Reusing configuration cache")
		append(":agent:client", "architecture { sharedApis = [] as Set }")

		assertDiagnostic(reject(), ":agent:client:client-api", ":agent:agent-api", "family")
	}

	@ParameterizedTest
	@ValueSource(strings = ["client", "server"])
	fun `sharing contracts does not allow opposite role APIs or implementations`(role: String) {
		agentFamilies()
		val other = if (role == "client") "server" else "client"
		append(":agent:$role", """
			dependencies {
				implementation project(':agent:$other:$other-api')
				implementation project(':agent:$other')
			}
		""".trimIndent())
		append(":agent:$role:$role-api", "dependencies { api project(':agent:$other:$other-api') }")

		assertDiagnostic(reject(), ":agent:$role:implementation -> :agent:$other:$other-api", "family",
			":agent:$role:implementation -> :agent:$other may depend only on APIs",
			":agent:$role:$role-api:api -> :agent:$other:$other-api")
	}

	@Test
	fun `sharing is inherited by own adapters but not nested API owners or unrelated families`() {
		agentFamilies(
			":agent:client:adapter" to "dependencies { implementation project(':agent:agent-api') }",
			":agent:client:extension:extension-api" to "",
			":agent:client:extension" to "dependencies { implementation project(':agent:agent-api') }",
			":other:other-api" to "",
			":other" to "dependencies { implementation project(':agent:agent-api') }"
		)

		val result = reject()
		assertDiagnostic(result, ":agent:client:extension:implementation -> :agent:agent-api", ":other:implementation -> :agent:agent-api")
		assertTrue(!result.output.contains(":agent:client:adapter:implementation -> :agent:agent-api"), result.output)
	}

	@Test
	fun `a shared contract cannot reexport a role API`() {
		agentFamilies()
		append(":agent:agent-api", "dependencies { api project(':agent:server:server-api') }")

		assertDiagnostic(reject(), ":agent:agent-api:api -> :agent:server:server-api", "family",
			":agent:client:client-api:api -> :agent:agent-api exposes unapproved API :agent:server:server-api")
	}

	@Test
	fun `approved sharing is not transitively granted through another API`() {
		project(
			":domain:domain-api" to "",
			":domain:middle:middle-api" to """
				architecture { sharedApis = [':domain:domain-api'] as Set }
				dependencies { api project(':domain:domain-api') }
			""".trimIndent(),
			":domain:middle:consumer" to """
				architecture { sharedApis = [':domain:middle:middle-api'] as Set }
				dependencies { implementation project(':domain:middle:consumer:consumer-api') }
			""".trimIndent(),
			":domain:middle:consumer:consumer-api" to "dependencies { api project(':domain:middle:middle-api') }"
		)

		assertDiagnostic(reject(), ":domain:middle:consumer:implementation -> :domain:middle:consumer:consumer-api exposes unapproved API :domain:domain-api",
			":domain:middle:consumer:consumer-api:api -> :domain:middle:middle-api exposes unapproved API :domain:domain-api", "not transitive")
		append(":domain:middle:consumer", "architecture { sharedApis = [':domain:middle:middle-api', ':domain:domain-api'] as Set }")

		assertVerified(verify())
	}

	@Test
	fun `shared declarations cannot allow sibling role or unrelated APIs`() {
		agentFamilies(
			":unrelated:unrelated-api" to "",
			":independent-api" to ""
		)
		append(":agent:client", """
			architecture { sharedApis = [':agent:agent-api', ':agent:server:server-api', ':unrelated:unrelated-api', ':independent-api'] as Set }
		""".trimIndent())

		assertDiagnostic(reject(), "sharedApis target :agent:server:server-api must belong to a strict ancestor",
			"sharedApis target :unrelated:unrelated-api must belong to a strict ancestor",
			"sharedApis target :independent-api must belong to a strict ancestor")
	}

	@Test
	fun `shared contract declarations require a real foreign API and an API owner`() {
		agentFamilies(
			":invalid" to "architecture { sharedApis = [':agent:agent-api'] as Set }"
		)
		append(":agent:client", """
			architecture {
				sharedApis = [':missing', 'agent:agent-api', ':agent:server', ':anvil-api', ':agent:client:client-api'] as Set
			}
		""".trimIndent())
		append(":agent:client:client-api", "architecture { sharedApis = [':agent:client:client-api'] as Set }")
		append(":anvil-api", "architecture { sharedApis = [':agent:agent-api'] as Set }")

		assertDiagnostic(reject(), "unknown API project ':missing'", "unknown API project 'agent:agent-api'",
			"target :agent:server must have API kind", "must not include the shared root API :anvil-api",
			"must not include its own API family: :agent:client:client-api",
			":invalid: sharedApis must be declared on an API or its owning project",
			":anvil-api: shared root API must not declare sharedApis")
	}

	@Test
	fun `directed shared contract declarations must not form cycles`() {
		project(
			":first" to "architecture { sharedApis = [':second:second-api'] as Set }",
			":first:first-api" to "",
			":second" to "architecture { sharedApis = [':first:first-api'] as Set }",
			":second:second-api" to ""
		)

		assertDiagnostic(reject(), "sharedApis must not form a cycle", ":first:first-api", ":second:second-api")
	}

	private fun agentFamilies(vararg additionalModules: Pair<String, String>) {
		project(
			":anvil-api" to "",
			":agent:agent-api" to "dependencies { api project(':anvil-api') }",
			":agent:client" to """
				architecture { sharedApis = [':agent:agent-api'] as Set }
				dependencies {
					implementation project(':agent:client:client-api')
					implementation project(':agent:agent-api')
				}
			""".trimIndent(),
			":agent:client:client-api" to "dependencies { api project(':agent:agent-api') }",
			":agent:server" to """
				architecture { sharedApis = [':agent:agent-api'] as Set }
				dependencies { implementation project(':agent:server:server-api') }
			""".trimIndent(),
			":agent:server:server-api" to "dependencies { api project(':agent:agent-api') }",
			*additionalModules
		)
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
