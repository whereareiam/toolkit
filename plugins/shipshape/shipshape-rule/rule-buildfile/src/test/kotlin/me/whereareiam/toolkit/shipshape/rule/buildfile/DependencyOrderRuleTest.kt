package me.whereareiam.toolkit.shipshape.rule.buildfile

import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DependencyOrderRuleTest {
	private val rule = DependencyOrderRule()

	@Test
	fun `the roles in their order, the projects first and each sorted pass, with a configuration of the file's own anywhere`() {
		val file = BuildFile("build.gradle.kts", """
			dependencies {
				contract(libs.nimbus.api.contract)

				api(project(":depot-api"))
				api(libs.platform.error.response.api)

				implementation(project(":depot-adapter:adapter-grpc"))
				implementation(project(":depot-common"))
				implementation(libs.platform.error.response)
				implementation(libs.platform.error.response.adapter.web)
				implementation(libs.spring.boot.starter.webmvc)
				implementation("org.yaml:snakeyaml")

				compileOnly(libs.jakarta.validation.api)
				compileOnlyApi(libs.slf4j.api)

				runtimeOnly(libs.postgresql)

				testFixturesApi(libs.platform.test.fixture)

				testFixturesImplementation(libs.testcontainers)

				testImplementation(testFixtures(project(":depot-adapter:adapter-s3")))
				testImplementation(testFixtures(project(":depot-common")))
				testImplementation(gradleTestKit())
				testImplementation(libs.platform.test.fixture)

				testRuntimeOnly(libs.nimbus.api.contract)

				testAnnotationProcessor(libs.lombok)
			}
		""".trimIndent())

		assertThat(LayoutFixture.breaches(rule, file)).isEmpty()
	}

	@Test
	fun `a role behind one that comes later is named, of the main code and across the source sets`() {
		val file = BuildFile("session-common/build.gradle.kts", """
			dependencies {
				implementation(libs.slf4j.api)

				api(project(":session-api"))

				testImplementation(libs.snakeyaml)

				testFixturesApi(libs.platform.test.fixture)

				runtimeOnly(libs.postgresql)
			}
		""".trimIndent())

		assertThat(LayoutFixture.breaches(rule, file)).containsExactly(
			"dependency-order session-common/build.gradle.kts:4 declares api behind implementation; $ROLES",
			"dependency-order session-common/build.gradle.kts:8 declares testFixturesApi behind testImplementation; $ROLES",
			"dependency-order session-common/build.gradle.kts:10 declares runtimeOnly behind testFixturesApi; $ROLES",
		)
	}

	@Test
	fun `a project behind what the catalog names is named`() {
		val file = BuildFile("build.gradle.kts", """
			dependencies {
				implementation(libs.spring.boot.starter.jdbc)
				implementation(project(":session-common"))
			}
		""".trimIndent())

		assertThat(LayoutFixture.breaches(rule, file)).containsExactly(
			"dependency-order build.gradle.kts:3 names the project :session-common behind libs.spring.boot.starter.jdbc; " +
				"in a role the projects stand before what the catalog names",
		)
	}

	@Test
	fun `what is not sorted is named, among the projects and among what the catalog names`() {
		val file = BuildFile("build.gradle.kts", """
			dependencies {
				implementation(project(":session-common"))
				implementation(project(":session-api"))
				implementation(libs.spring.boot.starter.grpc.server)
				implementation(libs.platform.error.response)

				testImplementation(libs.nimbus.protocol.session)
				testImplementation(libs.nimbus.protocol.account)
			}
		""".trimIndent())

		assertThat(LayoutFixture.breaches(rule, file)).containsExactly(
			"dependency-order build.gradle.kts:3 names :session-api behind :session-common; $SORTED",
			"dependency-order build.gradle.kts:5 names libs.platform.error.response behind libs.spring.boot.starter.grpc.server; $SORTED",
			"dependency-order build.gradle.kts:8 names libs.nimbus.protocol.account behind libs.nimbus.protocol.session; $SORTED",
		)
	}

	@Test
	fun `every block is held by itself`() {
		val file = BuildFile("build.gradle.kts", """
			dependencies {
				testImplementation(libs.snakeyaml)
			}

			subprojects {
				dependencies {
					api(libs.slf4j.api)
				}
			}
		""".trimIndent())

		assertThat(LayoutFixture.breaches(rule, file)).isEmpty()
	}

	private companion object {
		const val ROLES = "the roles stand as api, implementation, compileOnly, embedded, runtimeOnly, annotationProcessor: " +
			"of the main code, then of the test fixtures, then of the tests"
		const val SORTED = "in a role the projects, and then what the catalog names, are sorted by the alphabet"
	}
}
