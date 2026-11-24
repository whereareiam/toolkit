package me.whereareiam.toolkit.shipshape.rule.size

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ParameterRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = ParameterRule()

	@Test
	fun `five parameters pass, and an override takes what its method declares`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", """
			public class Sessions {
				void open(int a, int b, int c, int d, int e) {
				}

				@Override
				public void close(int a, int b, int c, int d, int e, int f) {
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a method or a constructor of more than five parameters is refused`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", """
			public class Sessions {
				Sessions(int a, int b, int c, int d, int e, int f) {
				}

				void open(int a, int b, int c, int d, int e, int f, int g) {
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"parameters session-common/src/main/java/com/example/session/Sessions.java:4 declares the constructor of Sessions with 6 " +
				"parameters; a method takes at most 5, more are one model",
			"parameters session-common/src/main/java/com/example/session/Sessions.java:7 declares open() with 7 parameters; a method " +
				"takes at most 5, more are one model",
		)
	}
}
