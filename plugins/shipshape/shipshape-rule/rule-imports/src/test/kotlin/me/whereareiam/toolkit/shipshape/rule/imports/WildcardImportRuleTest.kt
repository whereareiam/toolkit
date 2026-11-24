package me.whereareiam.toolkit.shipshape.rule.imports

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class WildcardImportRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = WildcardImportRule()

	@Test
	fun `an import that names what it brings passes`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.DefaultSessionService", """
			import java.util.List;

			// import java.util.*;
			public class DefaultSessionService {
				String pattern = "import java.io.*;";
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a package or a class imported whole is refused, in main code and in tests`() {
		val fixture = LayoutFixture.service(directory)
			.java(
				"session-common", "main", "com.example.session.DefaultSessionService",
				"import java.util.*;\n\npublic class DefaultSessionService {\n}\n",
			)
			.java(
				"session-common", "test", "com.example.session.DefaultSessionServiceTest",
				"import static org.assertj.core.api.Assertions.*;\n\nclass DefaultSessionServiceTest {\n}\n",
			)

		assertThat(fixture.breaches(rule)).containsExactly(
			"wildcard-import session-common/src/main/java/com/example/session/DefaultSessionService.java:3 imports java.util.* whole; " +
				"import each name it uses",
			"wildcard-import session-common/src/test/java/com/example/session/DefaultSessionServiceTest.java:3 imports " +
				"org.assertj.core.api.Assertions.* whole; import each name it uses",
		)
	}
}
