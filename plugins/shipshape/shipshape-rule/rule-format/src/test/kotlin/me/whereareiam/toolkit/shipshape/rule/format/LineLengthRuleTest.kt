package me.whereareiam.toolkit.shipshape.rule.format

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LineLengthRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = LineLengthRule()

	@Test
	fun `a line of 140 columns passes, a tab counted as four`() {
		val fixture = LayoutFixture.service(directory)
			.java(
				"session-common", "main", "com.example.session.DefaultSessionService",
				"public class DefaultSessionService {\n\t// ${"x".repeat(133)}\n}\n",
			)

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a longer line is refused, a comment as much as code, in main code and in tests`() {
		val fixture = LayoutFixture.service(directory)
			.java(
				"session-common", "main", "com.example.session.DefaultSessionService",
				"public class DefaultSessionService {\n\t// ${"x".repeat(134)}\n}\n",
			)
			.java(
				"session-common", "test", "com.example.session.DefaultSessionServiceTest",
				"class DefaultSessionServiceTest {\n\t\tString text = \"${"x".repeat(150)}\";\n}\n",
			)

		assertThat(fixture.breaches(rule)).containsExactly(
			"line-length session-common/src/main/java/com/example/session/DefaultSessionService.java:4 is 141 columns wide; a line has " +
				"at most 140, a tab counted as 4",
			"line-length session-common/src/test/java/com/example/session/DefaultSessionServiceTest.java:4 is 175 columns wide; a line " +
				"has at most 140, a tab counted as 4",
		)
	}
}
