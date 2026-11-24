package me.whereareiam.toolkit.shipshape.rule.language

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ReflectionRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = ReflectionRule()

	@Test
	fun `an explicit choice passes, and a test may reflect`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.Environments", """
				/** Says Class.forName( in a comment, which is no call. */
				public class Environments {
					Environment of(Caller caller) {
						return caller.staff() ? staff : users;
					}
				}
			""".trimIndent())
			.java("session-common", "test", "com.example.session.EnvironmentsTest", """
				import java.lang.reflect.Field;

				class EnvironmentsTest {
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a method a library hands over is no reflection, nor is a getter named as a lookup`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.AccountRequirement", """
			import java.lang.reflect.Method;

			public class AccountRequirement {
				boolean required(Method method) {
					return method.isAnnotationPresent(Account.class) && mask.getFields().isEmpty();
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `reflection and a dynamic proxy are refused in main code`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.EnvironmentRouter", """
			import java.lang.reflect.Proxy;

			public class EnvironmentRouter {
				Object route(Class<?> type) throws Exception {
					Class.forName("com.example.session.Sessions").getDeclaredMethod("open").setAccessible(true);
					type.getConstructor().newInstance();
					type.getField("open");
					MethodHandles.privateLookupIn(type, MethodHandles.lookup());

					return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, null);
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"reflection $ROUTER:7 calls Class.forName; $ASKED",
			"reflection $ROUTER:7 calls getDeclaredMethod; $ASKED",
			"reflection $ROUTER:7 calls setAccessible; $ASKED",
			"reflection $ROUTER:8 calls getConstructor; $ASKED",
			"reflection $ROUTER:9 calls getField; $ASKED",
			"reflection $ROUTER:10 calls MethodHandles.privateLookupIn; $ASKED",
			"reflection $ROUTER:10 calls MethodHandles.lookup; $ASKED",
			"reflection $ROUTER:12 calls Proxy.newProxyInstance; $ASKED",
		)
	}

	private companion object {
		const val ROUTER = "session-common/src/main/java/com/example/session/EnvironmentRouter.java"

		const val ASKED = "own code reflects on nothing; make the choice explicit, and fix the library that lacks the capability"
	}
}
