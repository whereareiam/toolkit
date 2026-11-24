package me.whereareiam.toolkit.shipshape.rule.types

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class EnumWordRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = EnumWordRule()

	@Test
	fun `an enum below type spells its own word, and a prefix or an upper-cased text that is no enum's is none`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.type.SessionKind", """
				import java.util.Locale;

				public enum SessionKind {
					WEB, PROGRAM;

					public String word() {
						return name().toLowerCase(Locale.ROOT);
					}

					public static SessionKind of(String word) {
						return valueOf(word.toUpperCase(Locale.ROOT));
					}
				}
			""".trimIndent())
			.java(GRPC, "main", "com.example.session.adapter.grpc.GrpcSessionService", """
				import java.util.Locale;

				public class GrpcSessionService {
					private static final String KEY_PREFIX = "rsa-";
					private static final String NOTE = "kind.name().toLowerCase() and valueOf(word.toUpperCase())";

					String word(com.example.session.type.SessionKind kind) {
						return kind.word();
					}

					String shout(Object first, String second) {
						return String.valueOf(first.hashCode())
							.concat(second.toUpperCase(Locale.ROOT));
					}

					String loud(String text) {
						return String.valueOf(text.toUpperCase(Locale.ROOT));
					}

					String name() {
						return name().toLowerCase(Locale.ROOT);
					}
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `main code outside of type does not make or read an enum's word by its name`() {
		val fixture = LayoutFixture.service(directory).java(GRPC, "main", "com.example.session.adapter.grpc.SessionMessageMapper", """
			import java.util.Locale;

			public class SessionMessageMapper {
				private static final String KIND_PREFIX = "SESSION_KIND_";

				String word(SessionKind kind) {
					return kind.name().toLowerCase(Locale.ROOT);
				}

				SessionKind kind(String word) {
					return SessionKind.valueOf(KIND_PREFIX + word.toUpperCase(Locale.ROOT));
				}

				SessionKind stored(java.sql.ResultSet row) throws java.sql.SQLException {
					return SessionKind.valueOf(row.getString("kind").toUpperCase(Locale.ROOT));
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"enum-word $MAPPER:6 keeps the beginning of an enum's names as a constant; $ASKED",
			"enum-word $MAPPER:9 makes an enum's word from the name of its constant; $ASKED",
			"enum-word $MAPPER:13 finds an enum's constant by an upper-cased text; $ASKED",
			"enum-word $MAPPER:17 finds an enum's constant by an upper-cased text; $ASKED",
		)
	}

	@Test
	fun `a call is named at the line its name is written on, also where its chain began lines above`() {
		val fixture = LayoutFixture.service(directory).java(GRPC, "main", "com.example.session.adapter.grpc.SessionMessageMapper", """
			public class SessionMessageMapper {
				String word(SessionKind kind) {
					return kind
						.name()
						.substring(1);
				}

				SessionKind kind(String word) {
					return SessionKind
						.valueOf(
							word.toUpperCase());
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"enum-word $MAPPER:6 makes an enum's word from the name of its constant; $ASKED",
			"enum-word $MAPPER:12 finds an enum's constant by an upper-cased text; $ASKED",
		)
	}

	private companion object {
		const val GRPC = "session-adapter/adapter-grpc"
		const val MAPPER = "$GRPC/src/main/java/com/example/session/adapter/grpc/SessionMessageMapper.java"
		const val ASKED = "the enum knows its word, as a field or a method of its own"
	}
}
