package me.whereareiam.toolkit.shipshape.rule.packages

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class UtilPackageRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = UtilPackageRule()

	@Test
	fun `a module's util package holds classes of static operations`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.util.Hashes", """
				import lombok.AccessLevel;
				import lombok.NoArgsConstructor;

				@NoArgsConstructor(access = AccessLevel.PRIVATE)
				public final class Hashes {
					private static final int[] ROUNDS = {1, 2};

					static {
						ROUNDS[0] = 3;
					}

					public static String sha256(byte[] content) {
						return content.length == 0 ? "" : "…";
					}

					private record Pair(String left, String right) {
					}
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.util.Texts", """
				public final class Texts {
					private Texts() {
					}

					@SuppressWarnings({"unused"})
					public static String trimmed(String text) {
						return text.trim();
					}
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.util.Numbers", """
				import lombok.experimental.UtilityClass;

				@UtilityClass
				public class Numbers {
					int half(int number) {
						return number / 2;
					}
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `what needs an instance is no stateless operation`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.util.Hashes", """
				public final class Hashes {
					private final String salt = "s";

					public Hashes() {
					}

					public String sha256(byte[] content) {
						return salt;
					}

					public static String hex(byte[] content) {
						return "";
					}
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.util.HashKind", "public enum HashKind {\n\tSHA_256\n}\n")

		assertThat(fixture.breaches(rule)).containsExactly(
			"util-package $UTIL/HashKind.java declares the enum HashKind; a util package holds classes of static operations only",
			"util-package $UTIL/Hashes.java:4 declares salt without static; $STATELESS",
			"util-package $UTIL/Hashes.java:6 declares a constructor that other classes can call; $STATELESS",
			"util-package $UTIL/Hashes.java:9 declares sha256 without static; $STATELESS",
		)
	}

	@Test
	fun `an assembly has no util package, which every module it assembles would reach`() {
		val fixture = LayoutFixture.service(directory).java(
			"", "main", "com.example.session.util.Hashes", "public final class Hashes {\n}\n",
		)

		assertThat(fixture.breaches(rule)).containsExactly(
			"util-package src/main/java/com/example/session/util/Hashes.java lies in a util package of the assembly session-service, a " +
				"catch-all for every module it assembles; a util package belongs to one module",
		)
	}

	private companion object {
		const val UTIL = "session-common/src/main/java/com/example/session/util"
		const val STATELESS = "a util package holds stateless operations, state belongs to the type it is about"
	}
}
