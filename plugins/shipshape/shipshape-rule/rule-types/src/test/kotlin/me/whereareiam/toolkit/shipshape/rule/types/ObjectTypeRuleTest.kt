package me.whereareiam.toolkit.shipshape.rule.types

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ObjectTypeRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = ObjectTypeRule()

	@Test
	fun `typed values pass, and so does what overrides a method of Object`() {
		val fixture = LayoutFixture.service(directory).java("session-api", "main", "com.example.session.model.Session", """
			public class Session {
				private final String id;

				@Override
				public boolean equals(Object other) {
					return other instanceof Session session && session.id.equals(id);
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `an Object as a field, a parameter or a result is refused in main code`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", """
			public class Sessions {
				private final java.lang.Object cache = null;

				Object read(Object key) {
					return key;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"object-type $SESSIONS:4 declares the field cache of Sessions as an Object; $ASKED",
			"object-type $SESSIONS:6 declares the result of read() of Sessions as an Object; $ASKED",
			"object-type $SESSIONS:6 declares the parameter key of read() of Sessions as an Object; $ASKED",
		)
	}

	private companion object {
		const val SESSIONS = "session-common/src/main/java/com/example/session/Sessions.java"

		const val ASKED = "a value is given its type once, at the boundary"
	}
}
