package me.whereareiam.toolkit.shipshape.rule.imports

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class StaticImportRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = StaticImportRule()

	@Test
	fun `a test imports its assertions statically`() {
		val fixture = LayoutFixture.service(directory)
			.java(
				"session-common", "test", "com.example.session.DefaultSessionServiceTest",
				"import static org.assertj.core.api.Assertions.assertThat;\n\nclass DefaultSessionServiceTest {\n}\n",
			)
			.java(
				"session-common", "testFixtures", "com.example.session.FakeSessionStore",
				"import static java.util.Objects.requireNonNull;\n\npublic class FakeSessionStore {\n}\n",
			)

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `main code names the class a member comes from`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.DefaultSessionService", """
			import java.util.Objects;
			import static java.util.Objects.requireNonNull;

			public class DefaultSessionService {
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"static-import session-common/src/main/java/com/example/session/DefaultSessionService.java:4 imports " +
				"java.util.Objects.requireNonNull statically; main code names the class, a static import stands in tests only",
		)
	}
}
