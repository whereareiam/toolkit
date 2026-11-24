package me.whereareiam.toolkit.shipshape.rule.tests

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class FixedSleepRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = FixedSleepRule()

	@Test
	fun `a test that moves its clock waits for nothing, and main code is not this rule's`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "test", "com.example.session.DefaultSessionServiceTest", """
				class DefaultSessionServiceTest {
					void expires() {
						clock.move(java.time.Duration.ofHours(12));
						// Thread.sleep(1000) would wait for the real one
					}
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.SessionSweeper", SLEEPING.format("public class SessionSweeper"))

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a fixed wait in the code of tests is refused`() {
		val fixture = LayoutFixture.service(directory)
			.java(
				"session-common", "test", "com.example.session.DefaultSessionServiceTest",
				SLEEPING.format("class DefaultSessionServiceTest"),
			)

		assertThat(fixture.breaches(rule)).containsExactly(
			"fixed-sleep session-common/src/test/java/com/example/session/DefaultSessionServiceTest.java:5 waits a fixed time with " +
				"Thread.sleep; wait for what the test expects, up to a deadline, or move the clock",
		)
	}

	private companion object {
		const val SLEEPING = "%s {\n\tvoid waits() throws InterruptedException {\n\t\tThread.sleep(200);\n\t}\n}\n"
	}
}
