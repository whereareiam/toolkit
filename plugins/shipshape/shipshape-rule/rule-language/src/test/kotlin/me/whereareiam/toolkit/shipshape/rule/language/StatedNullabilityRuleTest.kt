package me.whereareiam.toolkit.shipshape.rule.language

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class StatedNullabilityRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = StatedNullabilityRule()

	@Test
	fun `a signature that states what may be null passes, and so does what is no signature of a module's own`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.SessionService", """
				import org.jetbrains.annotations.NotNull;
				import org.jetbrains.annotations.Nullable;

				public interface SessionService {
					@NotNull Session open(@NotNull String userId, @Nullable String deviceId, boolean remember);

					@Nullable
					Session find(@NotNull String sessionId);

					void end(@NotNull String sessionId, int reason);

					byte @NotNull [] seal(byte @Nullable [] answer);
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.DefaultSessionService", """
				public class DefaultSessionService implements SessionService {
					@Override
					public Session find(String sessionId) {
						return null;
					}

					private Session read(String sessionId) {
						return null;
					}

					Session peek(String sessionId) {
						return null;
					}

					public int count() {
						return 0;
					}
				}
			""".trimIndent())
			.java("", "main", "com.example.session.SessionServiceConfiguration", unstated("SessionServiceConfiguration"))
			.java("session-common", "test", "com.example.session.FakeSessionStore", unstated("FakeSessionStore"))

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a field states what may be null where it is the signature, and a field only its class sees states nothing`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.Session", """
				import lombok.Getter;
				import lombok.RequiredArgsConstructor;
				import org.jetbrains.annotations.NotNull;
				import org.jetbrains.annotations.Nullable;

				@Getter
				@RequiredArgsConstructor
				public class Session {
					private static final String NONE = "";

					@NotNull
					private final String userId;
					private final @Nullable String deviceId;
					private final byte @NotNull [] token;
					private final boolean remembered;

					public record Device(@NotNull String id, int number) {
					}
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.DefaultSessionService", """
				import lombok.AccessLevel;
				import lombok.Getter;
				import lombok.RequiredArgsConstructor;

				@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
				public class DefaultSessionService {
					private final Object store;
					@Getter(AccessLevel.NONE)
					private final Object clock;
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.SessionSweeper", """
				import lombok.RequiredArgsConstructor;
				import org.jetbrains.annotations.NotNull;

				@RequiredArgsConstructor
				public class SessionSweeper {
					private final @NotNull Object store;
					private final Object lock = new Object();
					private Object last;
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a public signature of a module that leaves a reference unstated is refused`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.SessionService", """
				import org.jetbrains.annotations.NotNull;

				public interface SessionService {
					Session open(@NotNull String userId, String deviceId);

					@NotNull String describe(java.util.List<String> sessions);
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.OpaqueTokens", """
				public class OpaqueTokens {
					public OpaqueTokens(java.security.SecureRandom random, int bits) {
					}

					protected byte[] next() {
						return new byte[0];
					}
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"stated-nullability session-api/src/main/java/com/example/session/SessionService.java:6 declares open() of SessionService " +
				"without @NotNull or @Nullable on deviceId, its result; state what may be null",
			"stated-nullability session-api/src/main/java/com/example/session/SessionService.java:8 declares describe() of " +
				"SessionService without @NotNull or @Nullable on sessions; state what may be null",
			"stated-nullability session-common/src/main/java/com/example/session/OpaqueTokens.java:4 declares the constructor of " +
				"OpaqueTokens without @NotNull or @Nullable on random; state what may be null",
			"stated-nullability session-common/src/main/java/com/example/session/OpaqueTokens.java:7 declares next() of OpaqueTokens " +
				"without @NotNull or @Nullable on its result; state what may be null",
		)
	}

	@Test
	fun `a field of a model, and one Lombok writes a constructor or an accessor from, is refused without its nullability`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.Session", """
				public class Session {
					private final String userId = "";
					private final int devices = 0;

					public record Device(String id) {
					}
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.DefaultSessionService", """
				import lombok.Getter;
				import lombok.RequiredArgsConstructor;

				@RequiredArgsConstructor
				public class DefaultSessionService {
					private final Object store;
					@Getter
					private Object last;
					private Object lock = new Object();
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"stated-nullability $MODEL:4 declares the field userId of Session without @NotNull or @Nullable; a model states it on every " +
				"field",
			"stated-nullability $MODEL:7 declares the field id of Device without @NotNull or @Nullable; a model states it on every field",
			"stated-nullability $SERVICE:8 declares the field store of DefaultSessionService without @NotNull or @Nullable; $LOMBOK",
			"stated-nullability $SERVICE:9 declares the field last of DefaultSessionService without @NotNull or @Nullable; $LOMBOK",
		)
	}

	/** A class with a public method that states nothing of what may be null. */
	private fun unstated(type: String): String =
		"public class $type {\n\tpublic Object find(String sessionId) {\n\t\treturn null;\n\t}\n}\n"

	private companion object {
		const val MODEL = "session-api/src/main/java/com/example/session/model/Session.java"
		const val SERVICE = "session-common/src/main/java/com/example/session/DefaultSessionService.java"
		const val LOMBOK = "Lombok writes a signature from the field, and copies the annotation to it"
	}
}
