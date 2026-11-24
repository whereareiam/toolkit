package me.whereareiam.toolkit.shipshape.rule.types

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class StringStateRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = StringStateRule()

	@Test
	fun `ids, codes and media types stay texts, a typed state passes, and an adapter reads the words of the wire`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.Session", """
				public class Session {
					private final String id;
					private final String userId;
					private final SessionState state;
					private final String typeface;
					private final String recoveryCode;
					private final String contentType;
				}
			""".trimIndent())
			.java("session-adapter/adapter-grpc", "main", "com.example.session.adapter.grpc.SessionMessageMapper", """
				public class SessionMessageMapper {
					SessionState state(String state) {
						return null;
					}
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a state, a kind or a status held as a String is refused in the api and in the logic`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.Session", """
				public class Session {
					private final String state;
					private final String deviceKind;
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.Problems", """
				public class Problems {
					void refuse(String deliveryStatus, String reason) {
					}
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"string-state session-api/src/main/java/com/example/session/model/Session.java:4 declares the field state as a String; a " +
				"state is an enum or a sealed type below type/",
			"string-state session-api/src/main/java/com/example/session/model/Session.java:5 declares the field deviceKind as a String; " +
				"a kind is an enum or a sealed type below type/",
			"string-state session-common/src/main/java/com/example/session/Problems.java:4 declares the parameter deliveryStatus of " +
				"refuse() as a String; a status is an enum or a sealed type below type/",
		)
	}
}
