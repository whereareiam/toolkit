package me.whereareiam.toolkit.shipshape.rule.lombok

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class AssigningConstructorRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = AssigningConstructorRule()

	@Test
	fun `Lombok writes the constructor, and one that does more than assign is written out`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.DefaultSessionService", """
				import lombok.RequiredArgsConstructor;

				@RequiredArgsConstructor
				public class DefaultSessionService {
					private final SessionStore store;
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.OpaqueTokens", """
				public class OpaqueTokens {
					private final int bits;
					private final String form;

					public OpaqueTokens(int bits, String form) {
						if (bits < 128) throw new IllegalArgumentException("bits");

						this.bits = bits;
						this.form = form;
					}

					OpaqueTokens(String form) {
						this(256, form);
					}

					@Deprecated
					OpaqueTokens(int bits) {
						this.bits = bits;
					}

					OpaqueTokens(String form, int length) {
						this.form = form.trim();
						this.bits = length;
					}
				}
			""".trimIndent())
			.java(
				"session-common", "test", "com.example.session.FakeSessionStore", ASSIGNING.format("FakeSessionStore", "FakeSessionStore"),
			)

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a constructor that only assigns its parameters is refused in main code`() {
		val fixture = LayoutFixture.service(directory).java(
			"session-common", "main", "com.example.session.DefaultSessionService",
			ASSIGNING.format("DefaultSessionService", "DefaultSessionService"),
		)

		assertThat(fixture.breaches(rule)).containsExactly(
			"assigning-constructor session-common/src/main/java/com/example/session/DefaultSessionService.java:7 writes the constructor " +
				"of DefaultSessionService by hand, and it only assigns its parameters; Lombok writes it, in the order of the fields",
		)
	}

	@Test
	fun `the constructor a builder calls is Lombok's as well, where it only assigns`() {
		val fixture = LayoutFixture.service(directory).java("session-api", "main", "com.example.session.model.SessionToken", """
			import lombok.Builder;

			public class SessionToken {
				private final String hash;
				private final String sessionId;

				@Builder
				private SessionToken(String hash, String sessionId) {
					this.hash = hash;
					this.sessionId = sessionId;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"assigning-constructor session-api/src/main/java/com/example/session/model/SessionToken.java:9 writes the constructor of " +
				"SessionToken by hand, and it only assigns its parameters; Lombok writes it, in the order of the fields",
		)
	}

	private companion object {
		/** A class whose constructor only assigns, with the class's name to be filled in twice. */
		const val ASSIGNING = "public class %s {\n\tprivate final Object store;\n\tprivate final Object clock;\n\n" +
			"\tpublic %s(Object store, Object clock) {\n\t\tthis.clock = clock;\n\t\tthis.store = store;\n\t}\n}\n"
	}
}
