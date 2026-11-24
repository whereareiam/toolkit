package me.whereareiam.toolkit.shipshape.rule.flow

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ElseIfRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = ElseIfRule()

	@Test
	fun `an early return and an else pass`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", """
			public class Sessions {
				String word(int count) {
					if (count == 0) return "none";

					if (count == 1) {
						return "one";
					} else {
						return "many";
					}
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `an else if is refused in main code, and passes in tests`() {
		val chain = """
			public class %s {
				String word(int count) {
					if (count == 0) {
						return "none";
					} else if (count == 1) {
						return "one";
					}

					return "many";
				}
			}
		""".trimIndent()
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.Sessions", chain.format("Sessions"))
			.java("session-common", "test", "com.example.session.SessionsTest", chain.format("SessionsTest"))

		assertThat(fixture.breaches(rule)).containsExactly(
			"else-if session-common/src/main/java/com/example/session/Sessions.java:7 writes else if; return early, or choose by a " +
				"pattern switch or a table",
		)
	}
}
