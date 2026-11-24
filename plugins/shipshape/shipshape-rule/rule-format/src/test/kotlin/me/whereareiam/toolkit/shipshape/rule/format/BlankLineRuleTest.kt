package me.whereareiam.toolkit.shipshape.rule.format

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BlankLineRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = BlankLineRule()

	@Test
	fun `six statements stand together, a short computation beside its return, a longer one apart from it`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", """
			public class Sessions {
				int six() {
					int a = 1;
					int b = 2;
					int c = 3;
					int d = 4;
					int e = 5;
					int f = 6;

					return a + b + c + d + e + f;
				}

				int doubled(int a) {
					int b = a * 2;
					return b;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a seventh statement without a blank line, and a return glued to a longer body, are refused`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", """
			public class Sessions {
				int seven() {
					int a = 1;
					int b = 2;
					// a comment is no blank line
					int c = 3;
					int d = 4;
					int e = 5;
					int f = 6;
					int g = 7;

					return a;
				}

				int glued(int a) {
					int b = a * 2;
					int c = b * 2;
					int d = c * 2;
					return d;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"blank-line $SESSIONS:12 runs on as the seventh statement without a blank line; a blank line separates the steps of a body",
			"blank-line $SESSIONS:21 returns right below more than two lines; a blank line stands before the final return",
		)
	}

	private companion object {
		const val SESSIONS = "session-common/src/main/java/com/example/session/Sessions.java"
	}
}
