package me.whereareiam.toolkit.shipshape.rule.format

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class OneLineGuardRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = OneLineGuardRule()

	@Test
	fun `a guard on one line, one too wide without braces, one with an else, a comment or more to do stand as written`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", """
			public class Sessions {
				private boolean closed;

				void close() {
					if (closed) return;

					closed = true;
				}

				void wide(String reason) {
					if (reason.isBlank() || reason.length() > 1000 || reason.startsWith("a reason no one reads before the line ends"))
						throw new IllegalArgumentException("a reason is given, short enough to read: " + reason);
				}

				int pick(int a) {
					if (a > 0) {
						return a;
					} else {
						return -a;
					}
				}

				void explained(int a) {
					if (a < 0) {
						return; // a negative count means nothing to close
					}
				}

				void more(int a) {
					if (a < 0) {
						closed = true;
						return;
					}
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a guard that fits on one line is refused however it is wrapped or braced, and shown as one line`() {
		val main = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", """
			public class Sessions {
				void close(boolean closed, boolean stopped) {
					if (closed) {
						return;
					}
					if (closed
							|| stopped) {
						return;
					}
					if (stopped) { return; }
				}

				void each(java.util.List<String> names) {
					for (String name : names) {
						if (name.isBlank())
							continue;
					}
				}
			}
		""".trimIndent())
		main.java("session-common", "test", "com.example.session.SessionsTest", """
			class SessionsTest {
				void refuse(String name) {
					if (name == null) {
						throw new IllegalArgumentException(
								"a name");
					}
				}
			}
		""".trimIndent())

		assertThat(main.breaches(rule)).containsExactly(
			"one-line-guard session-common/src/main/java/com/example/session/Sessions.java:5 ${OneLineGuardRule.SPREAD}if (closed) return;",
			"one-line-guard session-common/src/main/java/com/example/session/Sessions.java:8 ${OneLineGuardRule.SPREAD}" +
				"if (closed || stopped) return;",
			"one-line-guard session-common/src/main/java/com/example/session/Sessions.java:12 ${OneLineGuardRule.SPREAD}" +
				"if (stopped) return;",
			"one-line-guard session-common/src/main/java/com/example/session/Sessions.java:17 ${OneLineGuardRule.SPREAD}" +
				"if (name.isBlank()) continue;",
			"one-line-guard session-common/src/test/java/com/example/session/SessionsTest.java:5 ${OneLineGuardRule.SPREAD}" +
				"if (name == null) throw new IllegalArgumentException(\"a name\");",
		)
	}

	@Test
	fun `a guard too wide for one line is refused in braces`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", """
			public class Sessions {
				void wide(String reason) {
					if (reason.isBlank() || reason.length() > 1000 || reason.startsWith("a reason no one reads before the line ends")) {
						throw new IllegalArgumentException("a reason is given, short enough to read: " + reason);
					}
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"one-line-guard session-common/src/main/java/com/example/session/Sessions.java:5 ${OneLineGuardRule.BRACED}",
		)
	}
}
