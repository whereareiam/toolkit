package me.whereareiam.toolkit.shipshape.rule.models

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TopLevelRecordRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = TopLevelRecordRule()

	@Test
	fun `a record nested in its owner is a small carrier`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.DefaultSessionService", """
			public class DefaultSessionService {
				// record Key(String userId) would be top level here
				private record Key(String userId, String deviceId) {
				}

				private final String record = "record Key(";
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a top-level record is refused in a module and in the assembly`() {
		val fixture = LayoutFixture.service(directory)
			.java(
				"session-api", "main", "com.example.session.model.Session", "public record Session(String sessionId, String userId) {\n}\n",
			)
			.java("", "main", "com.example.session.Page", "@Deprecated\nrecord Page<T>(java.util.List<T> items) {\n}\n")

		assertThat(fixture.breaches(rule)).containsExactly(
			"top-level-record src/main/java/com/example/session/Page.java declares a top-level record; $ASKED",
			"top-level-record session-api/src/main/java/com/example/session/model/Session.java declares a top-level record; $ASKED",
		)
	}

	private companion object {
		const val ASKED = "write a class, or nest the record in the type it belongs to"
	}
}
