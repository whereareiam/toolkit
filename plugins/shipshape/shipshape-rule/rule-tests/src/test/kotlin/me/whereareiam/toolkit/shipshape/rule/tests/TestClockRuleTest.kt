package me.whereareiam.toolkit.shipshape.rule.tests

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TestClockRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = TestClockRule()

	@Test
	fun `a test takes a clock that exists, and main code may write one`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "test", "com.example.session.DefaultSessionServiceTest", """
				import java.time.Clock;

				class DefaultSessionServiceTest {
					private final Clock clock = Clock.systemUTC();
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.SkewedClock", CLOCK.format("public class SkewedClock", "Clock"))

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a clock written for the tests of a build is refused, nested or in a fixture as well`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "test", "com.example.session.MutableClock", CLOCK.format("final class MutableClock", "Clock"))
			.java(
				"session-common", "testFixtures", "com.example.session.LocalIssuer",
				"public class LocalIssuer {\n\tprivate static class Ticking extends java.time.Clock {\n\t}\n}\n",
			)

		assertThat(fixture.breaches(rule)).containsExactly(
			"test-clock session-common/src/test/java/com/example/session/MutableClock.java:3 writes the clock MutableClock for its " +
				"tests; take the one of a library of test fixtures, a helper of tests is written once",
			"test-clock session-common/src/testFixtures/java/com/example/session/LocalIssuer.java:4 writes the clock Ticking for its " +
				"tests; take the one of a library of test fixtures, a helper of tests is written once",
		)
	}

	private companion object {
		const val CLOCK = "%s extends %s {\n}\n"
	}
}
