package me.whereareiam.toolkit.shipshape.api.buildfile

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BuildFileTest {
	@Test
	fun `the build files are the build and settings scripts of the root, the modules and an included build, none a build left behind`(
		@TempDir repository: File,
	) {
		val written = listOf(
			"build.gradle.kts", "session-adapter/adapter-grpc/build.gradle.kts", "build-logic/build.gradle.kts",
			"build/generated/build.gradle.kts", "session-api/.gradle/build.gradle.kts", "build-logic/settings.gradle.kts",
			"settings.gradle.kts", "session-api/gradle.properties",
		)
		written.forEach { repository.resolve(it).apply { parentFile.mkdirs() }.writeText("plugins {\n\tbase\n}\n") }

		val read = BuildFile.below(repository, emptySet())

		assertThat(read.map { it.path }).containsExactly(
			"build-logic/build.gradle.kts", "build-logic/settings.gradle.kts", "build.gradle.kts",
			"session-adapter/adapter-grpc/build.gradle.kts", "settings.gradle.kts",
		)
		assertThat(read.map { it.script }).containsExactly(
			BuildFile.Script.BUILD, BuildFile.Script.SETTINGS, BuildFile.Script.BUILD, BuildFile.Script.BUILD, BuildFile.Script.SETTINGS,
		)
		assertThat(read).allSatisfy { assertThat(it.applies("base")).isTrue() }
	}

	@Test
	fun `the plugins and the dependencies are read with their lines, and a comment is none of them`() {
		val file = BuildFile("session-common/build.gradle.kts", """
			plugins {
				`java-test-fixtures`
				// id("com.example.convention.adapter")
				id("com.example.convention.common")
			}

			description = "Listing sessions"

			dependencies {
				api(project(":session-api"))

				/* implementation(libs.slf4j.api) */
				testImplementation(libs.snakeyaml)
			}
		""".trimIndent())

		assertThat(file.plugins.map { it.line to it.name }).containsExactly(2 to "java-test-fixtures", 4 to "com.example.convention.common")
		assertThat(file.applies("com.example.convention.common")).isTrue()
		assertThat(file.applies("com.example.convention.adapter")).isFalse()
		assertThat(file.dependencies).hasSize(1)
		assertThat(file.dependencies.single().map { it.line to it.configuration }).containsExactly(10 to "api", 13 to "testImplementation")
	}

	@Test
	fun `an empty line separates two declarations, and a line of comment does not`() {
		val file = BuildFile("build.gradle.kts", """
			dependencies {
				api(project(":session-api"))
				// The driver, which only the tests name.
				implementation(libs.postgresql)

				// The fixture.
				testImplementation(libs.platform.test.fixture)
			}
		""".trimIndent())

		assertThat(file.dependencies.single().map { it.configuration to it.separated })
			.containsExactly("api" to false, "implementation" to false, "testImplementation" to true)
	}

	@Test
	fun `what a closure, a loop or a list inside the block writes is no declaration of the block`() {
		val file = BuildFile("build.gradle.kts", """
			dependencies {
				implementation(libs.awssdk.s3) {
					exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
				}
				listOf(
					libs.nimbus.protocol.account,
					libs.nimbus.protocol.session,
				).forEach { protocol("${'$'}{it.get().module}:${'$'}version") }
				constraints {
					api(libs.jetbrains.annotations)
				}
				add(
					"implementation",
					project(":session-api"))
				subprojects.forEach { published("${'$'}group:${'$'}{it.name}:${'$'}version") }
				runtimeOnly(libs.postgresql)
			}
		""".trimIndent())

		val declared = file.dependencies.single().map { it.line to it.configuration }

		assertThat(declared).containsExactly(2 to "implementation", 16 to "runtimeOnly")
	}

	@Test
	fun `a declaration is read over the lines it is written on, and a block that opens and closes on one line as well`() {
		val file = BuildFile("build.gradle.kts", """
			dependencies {
				implementation(
					"org.yaml:snakeyaml:2.6"
				)
				runtimeOnly(libs.postgresql)
			}

			subprojects {
				dependencies { testImplementation("org.assertj:assertj-core") }
			}
		""".trimIndent())

		val declared = file.dependencies.map { block -> block.map { Triple(it.line, it.configuration, it.coordinate) } }

		assertThat(declared).containsExactly(
			listOf(Triple(2, "implementation", "org.yaml:snakeyaml:2.6"), Triple(5, "runtimeOnly", null)),
			listOf(Triple(9, "testImplementation", "org.assertj:assertj-core")),
		)
	}

	@Test
	fun `what the constraints of a block declare is read apart, and every line of a block is kept with its number`() {
		val file = BuildFile("build.gradle.kts", """
			description = "Listing sessions"

			dependencies {
				implementation(libs.snakeyaml) {
					version { strictly("2.6") }
				}
				constraints {
					implementation("org.yaml:snakeyaml:2.5")
				}
			}
		""".trimIndent())

		assertThat(file.dependencies.single().map { it.line }).containsExactly(4)
		assertThat(file.constraints.map { it.line to it.coordinate }).containsExactly(8 to "org.yaml:snakeyaml:2.5")
		assertThat(file.dependencyLines.map { it.index }).containsExactly(3, 4, 5, 6, 7, 8, 9, 10)
		assertThat(file.dependencyLines.first { it.index == 5 }.value.trim()).isEqualTo("version { strictly(\"2.6\") }")
	}

	@Test
	fun `a brace or a parenthesis inside a string opens nothing`() {
		val file = BuildFile("build.gradle.kts", """
			description = "Counts ({ and ( both"

			dependencies {
				implementation(libs.snakeyaml)
			}
		""".trimIndent())

		assertThat(file.dependencies.single().map { it.accessor }).containsExactly("libs.snakeyaml")
	}

	@Test
	fun `every block of dependencies is read, also one that stands inside another block`() {
		val file = BuildFile("build.gradle.kts", """
			dependencies {
				api(libs.slf4j.api)
			}

			subprojects {
				dependencies {
					implementation(libs.snakeyaml)
				}
			}
		""".trimIndent())

		val named = file.dependencies.map { block -> block.map { it.accessor } }

		assertThat(named).containsExactly(listOf("libs.slf4j.api"), listOf("libs.snakeyaml"))
	}

	@Test
	fun `only the file's own block of plugins applies plugins`() {
		val file = BuildFile("build.gradle.kts", """
			plugins {
				`kotlin-dsl`
			}

			gradlePlugin {
				plugins {
					register("convention") {
						id = "me.whereareiam.toolkit.shipshape"
					}
					base
				}
			}
		""".trimIndent())

		assertThat(file.plugins.map { it.name }).containsExactly("kotlin-dsl")
	}

	@Test
	fun `the lines keep what a string says and lose what a comment says`() {
		val file = BuildFile("build.gradle.kts", "tasks.test {\n\t// the port of the stack\n\tenvironment(\"PORT\", \"18432\")\n}\n")

		assertThat(file.lines).containsExactly("tasks.test {", "\t", "\tenvironment(\"PORT\", \"18432\")", "}", "")
	}

	@Test
	fun `the strings are read with their lines, and none in a comment`() {
		val file = BuildFile("settings.gradle.kts", """
			// "org.yaml:snakeyaml:2.2"
			rootProject.name = "session-service" /* "a" */
			include("session-api", "say \"hi\"")
		""".trimIndent())

		assertThat(file.strings).containsExactly(
			IndexedValue(2, "session-service"),
			IndexedValue(3, "session-api"),
			IndexedValue(3, "say \\\"hi\\\""),
		)
	}

	@Test
	fun `a file of another name is no script of a build`() {
		assertThatThrownBy { BuildFile("session-api/gradle.properties", "") }.hasMessageContaining("gradle.properties is no script")
	}
}
