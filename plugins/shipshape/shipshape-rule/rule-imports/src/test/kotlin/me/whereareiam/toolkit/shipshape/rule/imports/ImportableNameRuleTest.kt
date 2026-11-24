package me.whereareiam.toolkit.shipshape.rule.imports

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ImportableNameRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = ImportableNameRule()

	@Test
	fun `a name that is imported, or only mentioned, is not written in full`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.DefaultSessionService", """
			import java.net.SocketTimeoutException;

			/** Fails with {@link java.io.IOException} when the store is gone. */
			public class DefaultSessionService {
				// java.util.List would do as well
				private final String note = "java.util.Map";

				void open() throws SocketTimeoutException {
					this.options.limits.Daily.check();
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a name nothing clashes with is imported, in main code and in tests`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.DefaultSessionService", """
				import java.util.List;

				public class DefaultSessionService {
					void open() throws java.net.SocketTimeoutException {
						java.util.List<String> names = List.of();
					}
				}
			""".trimIndent())
			.java("session-common", "test", "com.example.session.DefaultSessionServiceTest", """
				class DefaultSessionServiceTest {
					@org.junit.jupiter.api.Test
					void opens() {
					}
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"importable-name session-common/src/main/java/com/example/session/DefaultSessionService.java:6 writes " +
				"java.net.SocketTimeoutException in full; import it, nothing else here is called SocketTimeoutException",
			"importable-name session-common/src/main/java/com/example/session/DefaultSessionService.java:7 writes java.util.List in " +
				"full; import it, nothing else here is called List",
			"importable-name session-common/src/test/java/com/example/session/DefaultSessionServiceTest.java:4 writes " +
				"org.junit.jupiter.api.Test in full; import it, nothing else here is called Test",
		)
	}

	@Test
	fun `a name another import, a type of the file or a type of its package answers to stays written in full`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.Session")
			.java("session-api", "main", "com.example.session.model.Clock")
			.java("session-api", "main", "com.example.session.model.Sessions", """
				import com.example.session.exception.Session;

				public class Sessions {
					com.example.protocol.session.v1.Session message;
					java.time.Clock clock;
					java.time.Duration life;

					interface Duration {
					}
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `of two names written in full that answer to one word neither is imported, as nothing says which an import takes`() {
		val fixture = LayoutFixture.service(directory).java(GRPC, "main", "com.example.session.adapter.grpc.SessionMessageMapper", """
			public class SessionMessageMapper {
				com.example.session.model.Session model(com.example.protocol.session.v1.Session message) {
					java.util.Date from = new java.sql.Date(0);
					return null;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	private companion object {
		const val GRPC = "session-adapter/adapter-grpc"
	}
}
