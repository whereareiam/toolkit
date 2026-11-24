package me.whereareiam.toolkit.shipshape.rule.size

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class MethodSizeRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = MethodSizeRule()

	@Test
	fun `a body of 40 lines between its braces passes`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.Sessions", "public class Sessions {\n${method("open", 40)}}\n")

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a method or a constructor of more than 40 lines is refused`() {
		val body = "public class Sessions {\n${method("open", 41)}\n\tSessions() {\n" + lines(45) + "\t}\n}\n"
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", body)

		assertThat(fixture.breaches(rule)).containsExactly(
			"method-size session-common/src/main/java/com/example/session/Sessions.java:4 writes open() in 41 lines; a body has at most " +
				"40, each step a method named after what it does",
			"method-size session-common/src/main/java/com/example/session/Sessions.java:48 writes the constructor of Sessions in 45 " +
				"lines; a body has at most 40, each step a method named after what it does",
		)
	}

	private fun method(name: String, lines: Int): String = "\tvoid $name() {\n" + lines(lines) + "\t}\n"

	private fun lines(count: Int): String = (1..count).joinToString("") { "\t\tSystem.out.println($it);\n" }
}
