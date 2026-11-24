package me.whereareiam.toolkit.shipshape.rule.size

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CollaboratorRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = CollaboratorRule()

	@Test
	fun `eight collaborators pass, and a model or options hold as many fields as they are made of`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.DefaultSessionService", holding("DefaultSessionService", 8))
			.java("session-api", "main", "com.example.session.model.Session", holding("Session", 12))
			.java("session-api", "main", "com.example.session.config.SessionOptions", holding("SessionOptions", 12))

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a class of more than eight collaborators is refused, however they are handed in, and so is a wiring method of more`() {
		val beans = "public class SessionConfiguration {\n\t@Bean\n\tObject sessions(${(1..9).joinToString { "Object a$it" }}) {\n" +
			"\t\treturn null;\n\t}\n}\n"
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.DefaultSessionService", holding("DefaultSessionService", 9))
			.java("", "main", "com.example.session.SessionConfiguration", beans)

		assertThat(fixture.breaches(rule)).containsExactly(
			"collaborators src/main/java/com/example/session/SessionConfiguration.java:4 the wiring method sessions of " +
				"SessionConfiguration takes 9; a class has at most 8, split it by use case, each part with what it needs",
			"collaborators session-common/src/main/java/com/example/session/DefaultSessionService.java:3 DefaultSessionService holds 9 " +
				"collaborators; a class has at most 8, split it by use case, each part with what it needs",
		)
	}

	/** A class named [name] that takes [count] collaborators through Lombok's constructor. */
	private fun holding(name: String, count: Int): String =
		"@RequiredArgsConstructor\npublic class $name {\n" + (1..count).joinToString("") { "\tprivate final Object part$it;\n" } + "}\n"
}
