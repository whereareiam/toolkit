package me.whereareiam.toolkit.shipshape.rule.imports

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ImportOrderRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = ImportOrderRule()

	@Test
	fun `imports sorted in one group, the JDK's after a blank line and the static ones last, pass`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.Sessions", """
				import com.example.session.model.Session;
				import lombok.RequiredArgsConstructor;
				import org.jetbrains.annotations.NotNull;

				import java.util.List;
				import javax.annotation.processing.Generated;

				public class Sessions {
				}
			""".trimIndent())
			.java("session-common", "test", "com.example.session.SessionsTest", """
				import org.junit.jupiter.api.Test;

				import static org.assertj.core.api.Assertions.assertThat;

				class SessionsTest {
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `an import out of its order, a JDK import among the others and a missing blank line are refused`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.Sessions", """
				import org.jetbrains.annotations.NotNull;
				import lombok.RequiredArgsConstructor;

				public class Sessions {
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.Devices", """
				import lombok.RequiredArgsConstructor;
				import java.util.List;

				public class Devices {
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"import-order session-common/src/main/java/com/example/session/Devices.java:4 imports out of their order; $ASKED",
			"import-order session-common/src/main/java/com/example/session/Sessions.java:3 imports out of their order; $ASKED",
		)
	}

	private companion object {
		const val ASKED = "imports stand alphabetically, java and javax after a blank line, then the static ones after another"
	}
}
