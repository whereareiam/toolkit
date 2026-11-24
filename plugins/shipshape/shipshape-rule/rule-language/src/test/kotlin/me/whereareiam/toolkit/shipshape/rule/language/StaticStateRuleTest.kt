package me.whereareiam.toolkit.shipshape.rule.language

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class StaticStateRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = StaticStateRule()

	@Test
	fun `constants, instance state and a test's static state pass, as do the fields of an interface and an enum`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.Sessions", """
				public class Sessions {
					private static final int LIMIT = 8;
					private static final List<String> NAMES = List.of("open");

					private final Map<String, Session> open = new HashMap<>();
					private int count;

					interface Listener {
						int RETRIES = 3;
						static int unused = 0;
					}

					enum Phase {
						OPEN, CLOSED;

						private int seen;
					}
				}
			""".trimIndent())
			.java("session-common", "test", "com.example.session.SessionsTest", """
				class SessionsTest {
					private static Sessions shared;
					private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a static field that is not final and a static state per thread are refused, also in a member type`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.SessionRegistry", """
			public class SessionRegistry {
				private static SessionRegistry instance;
				static volatile int opened = 0;
				private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();

				static class Holder {
					static final java.lang.InheritableThreadLocal<String> CALLER = new InheritableThreadLocal<>();
					public static Map<String, Session> byId;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			breach(4, "declares the static field instance, which is not final", "SessionRegistry.instance"),
			breach(5, "declares the static field opened, which is not final", "SessionRegistry.opened"),
			breach(6, "holds state per thread in the static field CURRENT", "SessionRegistry.CURRENT"),
			breach(9, "holds state per thread in the static field CALLER", "SessionRegistry.Holder.CALLER"),
			breach(10, "declares the static field byId, which is not final", "SessionRegistry.Holder.byId"),
		)
	}

	@Test
	fun `a static field the root build file declares forced by a framework passes`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.SessionDoor", DOOR)
			.excusing(StaticStateRule.FORCED_STATIC, "com.example.session.SessionDoor.door" to "The mixins reach the mod only statically")

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a declaration that excuses nothing is a breach itself`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.SessionDoor")
			.excusing(StaticStateRule.FORCED_STATIC, "com.example.session.SessionDoor.door" to "The mixins reach the mod only statically")

		assertThat(fixture.breaches(rule)).containsExactly(
			"unused-exemption build.gradle.kts declares forcedStatic(\"com.example.session.SessionDoor.door\") in shipshape, and " +
				"nothing there needs it; remove the declaration",
		)
	}

	private fun breach(line: Int, what: String, field: String): String =
		"static-state session-common/src/main/java/com/example/session/SessionRegistry.java:$line $what; " +
			"static state has no owner: give it an owner the program makes, or declare what forces it: " +
			"forcedStatic(\"com.example.session.$field\", why)"

	private companion object {
		const val DOOR = """public class SessionDoor {
	public static SessionDoor door;
}
"""
	}
}
