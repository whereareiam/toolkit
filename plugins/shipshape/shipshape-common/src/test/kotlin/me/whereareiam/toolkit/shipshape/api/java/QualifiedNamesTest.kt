package me.whereareiam.toolkit.shipshape.api.java

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class QualifiedNamesTest {
	@TempDir
	lateinit var directory: Path

	@Test
	fun `a name written in full is found with its line, and an import, a comment, a literal and the path to a field are none`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.DefaultSessionService", """
			import java.util.List;

			/** Fails with {@link java.io.IOException}. */
			public class DefaultSessionService {
				private final String note = "java.util.Map";

				@org.jetbrains.annotations.NotNull java.time.Instant now() {
					return this.clock.zone.Offset == null ? null : java.time.Instant.now();
				}
			}
		""".trimIndent())

		assertThat(written(fixture)).containsExactly(
			"9 org.jetbrains.annotations.NotNull NotNull importable=true",
			"9 java.time.Instant Instant importable=true",
			"10 java.time.Instant Instant importable=true",
		)
	}

	@Test
	fun `an import does until another import, a type of the file, a type of the package or a second name answers to the same word`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.Clock")
			.java("session-common", "main", "com.example.session.DefaultSessionService", """
				import com.example.session.model.Session;

				public class DefaultSessionService {
					com.example.protocol.session.v1.Session message;
					java.time.Clock clock;
					java.time.Duration life;
					java.util.Date from;
					java.sql.Date until;
					com.example.session.model.Device device;
					com.example.protocol.session.v1.Device sent;

					interface Duration {
					}
				}
			""".trimIndent())

		assertThat(written(fixture)).containsExactly(
			"6 com.example.protocol.session.v1.Session Session importable=false",
			"7 java.time.Clock Clock importable=false",
			"8 java.time.Duration Duration importable=false",
			"9 java.util.Date Date importable=false",
			"10 java.sql.Date Date importable=false",
			"11 com.example.session.model.Device Device importable=false",
			"12 com.example.protocol.session.v1.Device Device importable=false",
		)
	}

	@Test
	fun `of two names in full that answer to one word nothing says which an import takes, and each knows the other`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.DefaultSessionService", """
			public class DefaultSessionService {
				com.example.protocol.account.v1.Account message;
				com.example.session.model.Account account;
			}
		""".trimIndent())
		val logic = fixture.repository().all.single()

		assertThat(QualifiedNames(fixture.repository()).of(logic).map { "${it.name} importable=${it.importable} beside=${it.beside}" })
			.containsExactly(
				"com.example.protocol.account.v1.Account importable=false beside=[com.example.session.model.Account]",
				"com.example.session.model.Account importable=false beside=[com.example.protocol.account.v1.Account]",
			)
	}

	/** What the logic of the fixture writes in full, each name as one line of what is known of it. */
	private fun written(fixture: LayoutFixture): List<String> {
		val repository = fixture.repository()
		val logic = repository.all.single { it.typeName == "DefaultSessionService" }

		return QualifiedNames(repository).of(logic).map { "${it.line} ${it.name} ${it.simpleName} importable=${it.importable}" }
	}
}
