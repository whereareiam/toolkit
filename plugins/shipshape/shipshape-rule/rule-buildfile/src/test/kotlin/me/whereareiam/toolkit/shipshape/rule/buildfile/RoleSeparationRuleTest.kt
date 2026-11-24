package me.whereareiam.toolkit.shipshape.rule.buildfile

import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RoleSeparationRuleTest {
	private val rule = RoleSeparationRule()

	@Test
	fun `roles that an empty line separates pass, also with a comment above the next one`() {
		val file = BuildFile("build.gradle.kts", """
			dependencies {
				api(project(":gateway-api"))

				implementation(libs.slf4j.api)
				implementation(libs.spring.boot.starter.webmvc)

				// The generated interfaces carry Bean Validation's annotations.
				compileOnly(libs.jakarta.validation.api)

				contract(libs.nimbus.api.contract)
			}
		""".trimIndent())

		assertThat(LayoutFixture.breaches(rule, file)).isEmpty()
	}

	@Test
	fun `two roles that run together are named, and a line of comment between them is no empty line`() {
		val file = BuildFile("depot-adapter/adapter-s3/build.gradle.kts", """
			dependencies {
				api(project(":depot-api"))
				// The S3 client.
				implementation(libs.awssdk.s3)

				testFixturesApi(libs.platform.test.fixture)
				testFixturesImplementation(libs.testcontainers)
			}
		""".trimIndent())

		assertThat(LayoutFixture.breaches(rule, file)).containsExactly(
			"role-separation depot-adapter/adapter-s3/build.gradle.kts:4 " +
				"declares implementation directly behind api; an empty line stands between two roles",
			"role-separation depot-adapter/adapter-s3/build.gradle.kts:7 " +
				"declares testFixturesImplementation directly behind testFixturesApi; an empty line stands between two roles",
		)
	}
}
