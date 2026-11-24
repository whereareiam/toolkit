package me.whereareiam.toolkit.shipshape.rule.buildfile

import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CatalogCoordinateRuleTest {
	private val rule = CatalogCoordinateRule()

	@Test
	fun `the rule reads the build scripts and the settings scripts`() {
		assertThat(rule.scripts).containsExactlyInAnyOrder(BuildFile.Script.BUILD, BuildFile.Script.SETTINGS)
	}

	@Test
	fun `the catalog, projects, a library without a version, task paths and comments pass`() {
		val file = BuildFile("session-common/build.gradle.kts", """
			// was "org.yaml:snakeyaml:2.2" before the catalog
			dependencies {
				implementation(platform(libs.spring.boot.dependencies))
				implementation(project(":session-api"))
				implementation(libs.slf4j.api)
				implementation("org.yaml:snakeyaml") /* "org.yaml:snakeyaml:2.2" */
			}

			tasks.named("check") {
				dependsOn(":session-api:check")
			}
		""".trimIndent())

		assertThat(LayoutFixture.breaches(rule, file)).isEmpty()
	}

	@Test
	fun `coordinates with a version are refused wherever a build script writes them`() {
		val file = BuildFile("session-common/build.gradle.kts", """
			buildscript {
				dependencies {
					classpath("org.ow2.asm:asm:9.7")
				}
			}

			dependencies {
				implementation("org.yaml:snakeyaml:2.2")
				testImplementation(group = "org.assertj", name = "assertj-core", version = "3.27.7")
				compileOnly("org.jetbrains:annotations:${'$'}annotations")
				constraints {
					implementation("com.google.guava:guava:[33.0,34.0)")
				}
			}
		""".trimIndent())

		assertThat(LayoutFixture.breaches(rule, file)).containsExactly(
			"$ID session-common/build.gradle.kts:3 writes out org.ow2.asm:asm:9.7 with its version; $ASKED",
			"$ID session-common/build.gradle.kts:8 writes out org.yaml:snakeyaml:2.2 with its version; $ASKED",
			"$ID session-common/build.gradle.kts:9 writes out org.assertj:assertj-core:3.27.7 with its version; $ASKED",
			"$ID session-common/build.gradle.kts:10 writes out org.jetbrains:annotations:${'$'}annotations with its version; $ASKED",
			"$ID session-common/build.gradle.kts:12 writes out com.google.guava:guava:[33.0,34.0) with its version; $ASKED",
		)
	}

	@Test
	fun `coordinates with a version are refused in a settings script`() {
		val file = BuildFile("settings.gradle.kts", """
			pluginManagement {
				resolutionStrategy {
					eachPlugin {
						if (requested.id.id == "com.example.livery") useModule("com.example:livery-gradle:0.1.2")
					}
				}
			}
		""".trimIndent())

		assertThat(LayoutFixture.breaches(rule, file)).containsExactly(
			"$ID settings.gradle.kts:4 writes out com.example:livery-gradle:0.1.2 with its version; $ASKED",
		)
	}

	@Test
	fun `a plugin's request resolved to its module at the version it asks for passes`() {
		val file = BuildFile("settings.gradle.kts", """
			pluginManagement {
				resolutionStrategy {
					eachPlugin {
						if (requested.id.id == "com.example.livery") useModule("com.example:livery-gradle:${'$'}{requested.version}")
					}
				}
			}
		""".trimIndent())

		assertThat(LayoutFixture.breaches(rule, file)).isEmpty()
	}

	private companion object {
		const val ID = "catalog-coordinate"
		const val ASKED = "name the library from the version catalog, gradle/libs.versions.toml, so that every version has one place"
	}
}
