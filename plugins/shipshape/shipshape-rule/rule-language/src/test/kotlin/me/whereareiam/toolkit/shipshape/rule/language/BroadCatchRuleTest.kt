package me.whereareiam.toolkit.shipshape.rule.language

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BroadCatchRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = BroadCatchRule()

	@Test
	fun `what a call throws is caught, and a test catches what it likes`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.DefaultSessionService", """
				public class DefaultSessionService {
					void open() {
						try {
							store.open();
						} catch (IllegalStateException | java.io.UncheckedIOException closed) {
							// catch (Exception ignored) would hide a failure nobody foresaw
						}
					}
				}
			""".trimIndent())
			.java(
				"session-common", "test", "com.example.session.DefaultSessionServiceTest",
				CATCHING.format("DefaultSessionServiceTest", "Exception"),
			)

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a broad catch in main code is refused, also as one of several`() {
		val fixture = LayoutFixture.service(directory)
			.java(
				"session-common", "main", "com.example.session.DefaultSessionService",
				CATCHING.format("DefaultSessionService", "final RuntimeException"),
			)
			.java(
				"session-common", "main", "com.example.session.SessionSweeper",
				CATCHING.format("SessionSweeper", "java.io.IOException | Throwable"),
			)

		assertThat(fixture.breaches(rule)).containsExactly(
			"broad-catch session-common/src/main/java/com/example/session/DefaultSessionService.java:7 catches RuntimeException; " +
				"catch what the call throws, or declare the class a boundary: " +
					"catchesEverything(\"com.example.session.DefaultSessionService\", why)",
			"broad-catch session-common/src/main/java/com/example/session/SessionSweeper.java:7 catches Throwable; " +
				"catch what the call throws, or declare the class a boundary: catchesEverything(\"com.example.session.SessionSweeper\", " +
					"why)",
		)
	}

	@Test
	fun `a class the root build file declares a boundary catches whatever is thrown`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.SessionSweeper", CATCHING.format("SessionSweeper", "RuntimeException"))
			.excusing(BroadCatchRule.CATCHES_EVERYTHING, SWEEPER to "A sweep that fails must not end the sweeper")

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a declaration that excuses nothing is a breach itself`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.SessionSweeper")
			.excusing(BroadCatchRule.CATCHES_EVERYTHING, SWEEPER to "A sweep that fails must not end the sweeper")

		assertThat(fixture.breaches(rule)).containsExactly(
			"unused-exemption build.gradle.kts declares catchesEverything(\"com.example.session.SessionSweeper\") in shipshape, and " +
				"nothing there needs it; remove the declaration",
		)
	}

	private companion object {
		const val SWEEPER = "com.example.session.SessionSweeper"
		const val CATCHING = "public class %s {\n\tvoid run() {\n\t\ttry {\n\t\t\twork();\n\t\t} catch (%s failure) {\n\t\t}\n\t}\n}\n"
	}
}
