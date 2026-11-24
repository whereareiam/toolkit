package me.whereareiam.toolkit.shipshape.rule.lombok

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BuilderPlaceRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = BuilderPlaceRule()

	@Test
	fun `a model, options, a member type and a test's class are built`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.Session", "@Builder\npublic class Session {\n}\n")
			.java("session-api", "main", "com.example.session.config.SessionOptions", built("SessionOptions"))
			.java("session-common", "main", "com.example.session.Sessions", holding("@Builder\n\tstatic class Page {\n\t}"))
			.java("session-common", "test", "com.example.session.SessionFixture", "@Builder\npublic class SessionFixture {\n}\n")

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a class of logic is not built, on the class nor on its constructor`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.DefaultSessionService", built("DefaultSessionService"))
			.java("session-common", "main", "com.example.session.Sessions", holding("@Builder\n\tSessions(int a) {\n\t}"))

		assertThat(fixture.breaches(rule)).containsExactly(
			"builder-place session-common/src/main/java/com/example/session/DefaultSessionService.java:3 builds DefaultSessionService " +
				"with @Builder; $ASKED",
			"builder-place session-common/src/main/java/com/example/session/Sessions.java:4 builds Sessions with @Builder; $ASKED",
		)
	}

	/** A class named [name] that carries `@Builder`. */
	private fun built(name: String): String = "@Builder\npublic class $name {\n}\n"

	/** The class `Sessions` with [member] in it. */
	private fun holding(member: String): String = "public class Sessions {\n\t$member\n}\n"

	private companion object {
		const val ASKED = "a builder makes a value below model/ or config/, a class of logic takes its collaborators through " +
			"@RequiredArgsConstructor"
	}
}
