package me.whereareiam.toolkit.shipshape.rule.lombok

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class AccessorsAnnotationRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = AccessorsAnnotationRule()

	@Test
	fun `a model has Lombok's ordinary getters`() {
		val fixture = LayoutFixture.service(directory).java("session-api", "main", "com.example.session.model.Session", """
			import lombok.Getter;

			// @Accessors(fluent = true) was the other style
			@Getter
			public class Session {
				private final String note = "@Accessors";
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a file that sets its own accessor style is refused, on a type and on a field, in main code and in tests`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.Session", """
				import lombok.Getter;
				import lombok.experimental.Accessors;

				@Getter
				@Accessors(fluent = true)
				public class Session {
					@Accessors(chain = true)
					private String userId;
				}
			""".trimIndent())
			.java(
				"session-common", "test", "com.example.session.FakeSessionStore",
				"@lombok.experimental.Accessors(fluent = true)\nclass FakeSessionStore {\n}\n",
			)

		assertThat(fixture.breaches(rule)).containsExactly(
			"accessors-annotation session-api/src/main/java/com/example/session/model/Session.java:7 sets its own accessor style with " +
				"@Accessors; remove it, an accessor is Lombok's getter everywhere",
			"accessors-annotation session-api/src/main/java/com/example/session/model/Session.java:9 sets its own accessor style with " +
				"@Accessors; remove it, an accessor is Lombok's getter everywhere",
			"accessors-annotation session-common/src/test/java/com/example/session/FakeSessionStore.java:3 sets its own accessor style " +
				"with @Accessors; remove it, an accessor is Lombok's getter everywhere",
		)
	}
}
