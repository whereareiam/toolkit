package me.whereareiam.toolkit.shipshape.rule.models

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class NestedRecordRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = NestedRecordRule()

	@Test
	fun `a private record, a record of a method and a small part of a model are small carriers`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.DefaultSessionService", """
				public class DefaultSessionService {
					private record Key(String userId, String deviceId) {
					}

					void open() {
						record Pair(String left, String right) {
						}
					}

					private final String record = "record Key(";
				}
			""".trimIndent())
			.java("session-api", "main", "com.example.session.model.Session", """
				public class Session {
					public record Device(String id, String name, java.util.Map<String, String> labels) {
						static final int LIMIT = 3;
					}
				}
			""".trimIndent())
			.java(
				"session-common", "test", "com.example.session.DefaultSessionServiceTest",
				"class DefaultSessionServiceTest {\n\trecord Sample(String id) {\n\t}\n}\n",
			)

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a record other classes reach outside of a model, and a part of more than three components, break nested-record`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.DefaultSessionService", """
				public class DefaultSessionService {
					record Key(String userId, String deviceId) {
					}
				}
			""".trimIndent())
			.java("session-api", "main", "com.example.session.model.Session", """
				public class Session {
					public record Device(String id, String name, String platform, @Nullable String model) {
					}
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"nested-record session-api/src/main/java/com/example/session/model/Session.java:4 declares the record Device with 4 " +
				"components; a part of a model has at most 3, a larger one is a class",
			"nested-record session-common/src/main/java/com/example/session/DefaultSessionService.java:4 declares the record Key open to " +
				"other classes; a record is private to its class, or a part nested in its model",
		)
	}

	@Test
	fun `a record that is a member of a nested class is read as one of the top class is`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.DefaultSessionService", """
			public class DefaultSessionService {
				private static final class Opened {
					record Key(String userId) {
					}
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"nested-record session-common/src/main/java/com/example/session/DefaultSessionService.java:5 declares the record Key open to " +
				"other classes; a record is private to its class, or a part nested in its model",
		)
	}
}
