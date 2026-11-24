package me.whereareiam.toolkit.shipshape.rule.size

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ClassSizeRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = ClassSizeRule()

	@Test
	fun `a type of 250 lines passes, and so does a longer one in the code of tests`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.Sessions", spanning("Sessions", 250))
			.java("session-common", "test", "com.example.session.SessionsTest", spanning("SessionsTest", 400))

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a type of main code over 250 lines is refused, its Javadoc not counted`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.Sessions", "/**\n * Sessions.\n */\n" + spanning("Sessions", 251))

		assertThat(fixture.breaches(rule)).containsExactly(
			"class-size session-common/src/main/java/com/example/session/Sessions.java:6 declares Sessions in 251 lines; a type has at " +
				"most 250, split it by what each part does",
		)
	}

	/** A class named [name] that spans [lines] lines, its braces among them. */
	private fun spanning(name: String, lines: Int): String =
		"public class $name {\n" + (1..lines - 2).joinToString("") { "\tint field$it;\n" } + "}\n"
}
