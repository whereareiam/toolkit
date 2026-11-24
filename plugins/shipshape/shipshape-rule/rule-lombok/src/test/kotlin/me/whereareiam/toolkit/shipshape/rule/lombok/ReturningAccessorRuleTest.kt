package me.whereareiam.toolkit.shipshape.rule.lombok

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ReturningAccessorRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = ReturningAccessorRule()

	@Test
	fun `Lombok writes the getter, and a method that does more than return its field or is named by what it overrides is written out`() {
		val fixture = LayoutFixture.service(directory).java("session-api", "main", "com.example.session.model.Session", """
			import lombok.Getter;

			@Getter
			public class Session {
				private static final Session NONE = new Session();

				private final String userId = "";
				private final String gateway = "";
				private final java.util.List<String> devices = java.util.List.of();

				public java.util.List<String> getDevices() {
					return java.util.List.copyOf(devices);
				}

				public String owner() {
					return userId;
				}

				public String getUserId(String fallback) {
					return userId;
				}

				public static Session none() {
					return NONE;
				}

				@JsonProperty("user")
				public String getUserId() {
					return userId;
				}

				@Override
				public String gateway() {
					return gateway;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a final accessor of a class that can be extended is written out, as Lombok writes no final method`() {
		val fixture = LayoutFixture.service(directory).java("session-api", "main", "com.example.session.view.View", """
			public abstract class View {
				private final String key = "";

				public final String key() {
					return key;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a final accessor of a final class is refused, as the word changes nothing there`() {
		val fixture = LayoutFixture.service(directory).java("session-api", "main", "com.example.session.view.Toast", """
			public final class Toast {
				private final long shownMillis = 0;

				public final long shownMillis() {
					return shownMillis;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"returning-accessor session-api/src/main/java/com/example/session/view/Toast.java:6 writes shownMillis() of Toast by hand, " +
				"and it only returns the field shownMillis; @Getter writes it",
		)
	}

	@Test
	fun `an accessor that only returns the field it is named after is refused in main code`() {
		val fixture = LayoutFixture.service(directory).java("session-api", "main", "com.example.session.model.Session", """
			public class Session {
				private final String userId = "";
				private final boolean remembered = false;
				private final String device = "";

				public @NotNull String getUserId() {
					return userId;
				}

				public boolean isRemembered() {
					return this.remembered;
				}

				@Override
				public String getDevice() {
					return device;
				}

				public String device() {
					return device;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"returning-accessor session-api/src/main/java/com/example/session/model/Session.java:8 writes getUserId() of Session by " +
				"hand, and it only returns the field userId; @Getter writes it",
			"returning-accessor session-api/src/main/java/com/example/session/model/Session.java:12 writes isRemembered() of Session by " +
				"hand, and it only returns the field remembered; @Getter writes it",
			"returning-accessor session-api/src/main/java/com/example/session/model/Session.java:16 writes getDevice() of Session by " +
				"hand, and it only returns the field device; @Getter writes it",
			"returning-accessor session-api/src/main/java/com/example/session/model/Session.java:21 writes device() of Session by hand, " +
				"and it only returns the field device; @Getter writes it",
		)
	}
}
