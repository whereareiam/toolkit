package me.whereareiam.toolkit.shipshape.rule.types

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class StringChoiceRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = StringChoiceRule()

	@Test
	fun `the mapper of an adapter or an integration and an enum compare words, and tests do as they like`() {
		val fixture = LayoutFixture.service(directory)
			.java(GRPC, "main", "com.example.session.adapter.grpc.SessionMessageMapper", CHOOSING.format("SessionMessageMapper"))
			.java(ACCOUNT, "main", "com.example.session.integration.account.AccountMapper", CHOOSING.format("AccountMapper"))
			.java("session-common", "test", "com.example.session.SessionsTest", CHOOSING.format("SessionsTest"))
			.java("session-api", "main", "com.example.session.type.SessionKind", """
				public enum SessionKind {
					WEB;

					static SessionKind of(String word) {
						return "web".equals(word) ? WEB : null;
					}
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a switch on a text and equals of a literal are refused elsewhere`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.Sessions", CHOOSING.format("Sessions"))
			.java(GRPC, "main", "com.example.session.adapter.grpc.GrpcSessionService", CHOOSING.format("GrpcSessionService"))

		assertThat(fixture.breaches(rule)).containsExactly(
			"string-choice session-common/src/main/java/com/example/session/Sessions.java:6 chooses by the text \"web\" in a switch; " +
				"$ASKED",
			"string-choice session-common/src/main/java/com/example/session/Sessions.java:12 compares a text with \"active\"; $ASKED",
			"string-choice session-adapter/adapter-grpc/src/main/java/com/example/session/adapter/grpc/GrpcSessionService.java:6 chooses " +
				"by the text \"web\" in a switch; $ASKED",
			"string-choice session-adapter/adapter-grpc/src/main/java/com/example/session/adapter/grpc/GrpcSessionService.java:12 " +
				"compares a text with \"active\"; $ASKED",
		)
	}

	private companion object {
		const val GRPC = "session-adapter/adapter-grpc"
		const val ACCOUNT = "session-integration/integration-account"

		const val ASKED = "a closed set of words is an enum or a sealed type, and the text becomes one in a mapper of the wire"

		/** A class that chooses by a word, its name to be filled in. */
		const val CHOOSING = "public class %s {\n\tint kind(String word) {\n\t\tswitch (word) {\n\t\t\tcase \"web\": return 1;\n" +
			"\t\t\tdefault: return 0;\n\t\t}\n\t}\n\n\tboolean active(String state) {\n\t\treturn state.equals(\"active\");\n\t}\n}\n"
	}
}
