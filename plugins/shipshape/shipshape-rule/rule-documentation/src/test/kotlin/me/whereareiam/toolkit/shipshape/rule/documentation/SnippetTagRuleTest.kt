package me.whereareiam.toolkit.shipshape.rule.documentation

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SnippetTagRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = SnippetTagRule()

	@Test
	fun `an example in pre and code passes, and so does the tag where it is no Javadoc`() {
		val fixture = LayoutFixture.service(directory).java("session-api", "main", "com.example.session.SessionService", """
			/**
			 * Opens a session.
			 *
			 * <pre>{@code
			 * sessions.open(userId);
			 * }</pre>
			 */
			public interface SessionService {
				String NOTE = "{@snippet :";

				// {@snippet} is what the Javadocs here do not use.
				void open(String userId);
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a Javadoc that writes its example with snippet is refused, in main code and in tests`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.SessionService", """
				/**
				 * Opens a session.
				 *
				 * {@snippet :
				 * sessions.open(userId);
				 * }
				 */
				public interface SessionService {
					/** As {@snippet file="Open.java"} shows. */
					void open(String userId);
				}
			""".trimIndent())
			.java(
				"session-common", "test", "com.example.session.FakeSessionStore",
				"/**\n * {@snippet :\n * new FakeSessionStore();\n * }\n */\n$FAKE",
			)

		assertThat(fixture.breaches(rule)).containsExactly(
			"snippet-tag session-api/src/main/java/com/example/session/SessionService.java:6 $ASKED",
			"snippet-tag session-api/src/main/java/com/example/session/SessionService.java:11 $ASKED",
			"snippet-tag session-common/src/test/java/com/example/session/FakeSessionStore.java:4 $ASKED",
		)
	}

	private companion object {
		const val FAKE = "class FakeSessionStore {\n}\n"
		const val ASKED = "writes an example with @snippet; write it in <pre>{@code …}</pre>"
	}
}
