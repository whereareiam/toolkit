package me.whereareiam.toolkit.shipshape.rule.flow

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ComplexityRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = ComplexityRule()

	@Test
	fun `a flow of fifteen passes`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", """
			public class Sessions {
				int count(java.util.List<String> names) {
					int count = 0;
					for (String name : names) {
						if (name.isEmpty()) continue;
						if (name.length() > 3 && name.length() < 9) count++;
						if (name.startsWith("a") || name.startsWith("b")) count += 2;
					}
					while (count > 100) {
						count = count > 200 ? count / 2 : count - 1;
					}

					return count;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a flow over fifteen is refused, a structure counted with how deep it stands`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", """
			public class Sessions {
				int count(java.util.List<java.util.List<String>> groups) {
					int count = 0;
					for (java.util.List<String> names : groups) {
						for (String name : names) {
							if (name.isEmpty()) {
								try {
									count++;
								} catch (RuntimeException failed) {
									if (count > 2) count--;
								}
							} else {
								count += name.length() > 3 && name.length() < 9 || name.isBlank() ? 1 : 2;
							}
						}
					}

					return count;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"cognitive-complexity session-common/src/main/java/com/example/session/Sessions.java:4 writes count() with a cognitive " +
				"complexity of 22; at most 15, the decisions told apart by early returns, a table or methods of their own",
		)
	}
}
