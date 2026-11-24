package me.whereareiam.toolkit.shipshape.rule.lombok

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class EmptyPrivateConstructorRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = EmptyPrivateConstructorRule()

	@Test
	fun `Lombok writes the constructor that keeps a class from being made, and one that does something is written out`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.OpaqueTokens", """
				import lombok.AccessLevel;
				import lombok.NoArgsConstructor;

				@NoArgsConstructor(access = AccessLevel.PRIVATE)
				public final class OpaqueTokens {
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.ReplaySeal", """
				public final class ReplaySeal {
					private final String key;

					private ReplaySeal() {
						this.key = "none";
					}

					private ReplaySeal(String key) {
					}
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `an empty private constructor written by hand is refused`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.OpaqueTokens", """
			public final class OpaqueTokens {
				private OpaqueTokens() {
					// never made
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"empty-private-constructor session-common/src/main/java/com/example/session/OpaqueTokens.java:4 writes the empty private " +
				"constructor of OpaqueTokens by hand; @NoArgsConstructor(access = AccessLevel.PRIVATE) writes it",
		)
	}
}
