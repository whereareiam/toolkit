package me.whereareiam.toolkit.shipshape.rule.lombok

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class HandWrittenLoggerRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = HandWrittenLoggerRule()

	@Test
	fun `Lombok writes the logger, and a test may ask for one to listen to`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.DefaultSessionService", """
				import lombok.extern.slf4j.Slf4j;

				@Slf4j
				public class DefaultSessionService {
					void open() {
						log.info("opened");
					}
				}
			""".trimIndent())
			.java("session-common", "test", "com.example.session.DefaultSessionServiceTest", """
				class DefaultSessionServiceTest {
					private final Logger logger = (Logger) LoggerFactory.getLogger(DefaultSessionService.class);
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a logger declared by hand in main code is refused`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.DefaultSessionService", """
			import org.slf4j.Logger;
			import org.slf4j.LoggerFactory;

			public class DefaultSessionService {
				private static final Logger LOGGER =
					LoggerFactory.getLogger(DefaultSessionService.class);
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"hand-written-logger session-common/src/main/java/com/example/session/DefaultSessionService.java:7 declares the logger " +
				"LOGGER by hand; @Slf4j writes it",
		)
	}
}
