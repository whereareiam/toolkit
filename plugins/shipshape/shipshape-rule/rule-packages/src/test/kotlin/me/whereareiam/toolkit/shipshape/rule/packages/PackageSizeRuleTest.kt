package me.whereareiam.toolkit.shipshape.rule.packages

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PackageSizeRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = PackageSizeRule()

	@Test
	fun `seven files lie directly in a package, and more below it`() {
		val fixture = classes(LayoutFixture.service(directory), "session-api", "main", "com.example.session.model", 7)

		assertThat(classes(fixture, "session-api", "main", "com.example.session.model.token", 3).breaches(rule)).isEmpty()
	}

	@Test
	fun `the eighth file makes a package a flat list, in main code and in tests`() {
		val fixture = classes(LayoutFixture.service(directory), "session-api", "main", "com.example.session.model", 8)

		assertThat(classes(fixture, "session-common", "test", "com.example.session", 9).breaches(rule)).containsExactly(
			"package-size session-api/src/main/java/com/example/session/model holds 8 files directly; a package holds at most 7, the " +
				"rest grouped below it by what they are about",
			"package-size session-common/src/test/java/com/example/session holds 9 files directly; a package holds at most 7, the rest " +
				"grouped below it by what they are about",
		)
	}

	@Test
	fun `the root of an api is a package as every other, whatever lies in it`() {
		val fixture = classes(LayoutFixture.service(directory), "session-api", "main", "com.example.session", 15)

		assertThat(classes(fixture, "session-common", "main", "com.example.session", 11).breaches(rule)).containsExactly(
			"package-size session-api/src/main/java/com/example/session holds 15 files directly; a package holds at most 7, the rest " +
				"grouped below it by what they are about",
			"package-size session-common/src/main/java/com/example/session holds 11 files directly; a package holds at most 7, the rest " +
				"grouped below it by what they are about",
		)
	}

	private fun classes(fixture: LayoutFixture, module: String, sourceSet: String, packageName: String, count: Int): LayoutFixture =
		(1..count).fold(fixture) { filled, number -> filled.java(module, sourceSet, "$packageName.Type$number") }
}
