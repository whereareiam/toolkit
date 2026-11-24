package me.whereareiam.toolkit.shipshape.rule.documentation

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TypeJavadocRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = TypeJavadocRule()

	@Test
	fun `a Javadoc stands above the type and its annotations`() {
		val fixture = LayoutFixture.service(directory)
			.java(
				"session-api", "main", "com.example.session.SessionService",
				"/** Opens and ends sessions. */\npublic interface SessionService {\n}\n",
			)
			.java("", "main", "com.example.session.SessionServiceConfiguration", """
				/**
				 * Wires the modules into the service.
				 */
				@Configuration(proxyBeanMethods = false)
				@Import({
					SessionStores.class,
					SessionClients.class,
				})

				public final class SessionServiceConfiguration {
					class Nested {
					}
				}
			""".trimIndent())
			.java("session-common", "test", "com.example.session.DefaultSessionServiceTest", "class DefaultSessionServiceTest {\n}\n")

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a top-level type of main code without a Javadoc is refused`() {
		val fixture = LayoutFixture.service(directory)
			.java(
				"session-common", "main", "com.example.session.DefaultSessionService", "@Slf4j\npublic class DefaultSessionService {\n}\n",
			)
			.java(
				"session-api", "main", "com.example.session.type.SessionKind",
				"/* The kinds of a session. */\npublic enum SessionKind {\n}\n",
			)
			.java(
				"session-api", "main", "com.example.session.model.Session",
				"/** Imports first. */\nimport lombok.Getter;\n\n@Getter\npublic class Session {\n}\n",
			)

		assertThat(fixture.breaches(rule)).containsExactly(
			"type-javadoc session-api/src/main/java/com/example/session/model/Session.java:7 declares Session without a Javadoc; say " +
				"what the type is for",
			"type-javadoc session-api/src/main/java/com/example/session/type/SessionKind.java:4 declares SessionKind without a Javadoc; " +
				"say what the type is for",
			"type-javadoc session-common/src/main/java/com/example/session/DefaultSessionService.java:4 declares DefaultSessionService " +
				"without a Javadoc; say what the type is for",
		)
	}
}
