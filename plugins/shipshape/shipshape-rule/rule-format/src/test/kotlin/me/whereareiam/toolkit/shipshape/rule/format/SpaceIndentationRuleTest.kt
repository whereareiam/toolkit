package me.whereareiam.toolkit.shipshape.rule.format

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SpaceIndentationRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = SpaceIndentationRule()

	@Test
	fun `tabs indent, a space aligns behind them, and a Javadoc and a text block are no code`() {
		val fixture = LayoutFixture.service(directory).file(SERVICE, listOf(
			"package com.example.session;",
			"",
			"/**",
			" * Opens sessions.",
			" */",
			"public class DefaultSessionService {",
			"\t/*",
			"    kept as it was",
			"\t */",
			"\tprivate final String page = \"\"\"",
			"    <p>",
			"      indented as the text asks",
			"    </p>",
			"\t\t\"\"\";",
			"",
			"\tboolean open(String user,",
			"\t             String device) {",
			"\t\treturn true;",
			"\t}",
			"}",
			"",
		).joinToString("\n"))

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a line of code that begins with a space is refused`() {
		val fixture = LayoutFixture.service(directory).file(SERVICE, listOf(
			"package com.example.session;",
			"",
			"public class DefaultSessionService {",
			"    boolean open() {",
			" \treturn true;",
			"\t}",
			"}",
			"",
		).joinToString("\n"))

		assertThat(fixture.breaches(rule)).containsExactly(
			"space-indentation $SERVICE:4 begins with a space; code is indented " +
				"with tabs, and a space only aligns behind them",
			"space-indentation $SERVICE:5 begins with a space; code is indented " +
				"with tabs, and a space only aligns behind them",
		)
	}
	private companion object {
		const val SERVICE = "session-common/src/main/java/com/example/session/DefaultSessionService.java"
	}
}
