package me.whereareiam.toolkit.shipshape.rule.models

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ImmutableModelRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = ImmutableModelRule()

	@Test
	fun `a model of final fields passes, and what is no model may change`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.Session", """
				import lombok.Builder;
				import lombok.Getter;

				@Getter
				@Builder
				public class Session {
					private static int made;

					private final String userId;
					private final int devices;

					public Session withDevices(int devices) {
						return new Session(userId, devices);
					}

					public static void setMade(int count) {
						made = count;
					}

					public static class SessionBuilder {
						private String note;
					}

					public record Device(String id) {
					}
				}
			""".trimIndent())
			.java("session-api", "main", "com.example.session.model.Token", "@lombok.Value\npublic class Token {\n\tString value;\n}\n")
			.java(
				"session-common", "main", "com.example.session.DefaultSessionService", MUTABLE.replace("Session", "DefaultSessionService"),
			)
			.java("session-api", "test", "com.example.session.model.SessionFixture", MUTABLE.replace("Session", "SessionFixture"))

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a model with a field that is not final, with a setter or with one of Lombok's is refused`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.Session", MUTABLE)
			.java("session-api", "main", "com.example.session.model.device.Device", """
				import lombok.Data;
				import lombok.Setter;

				@Data
				public class Device {
					@Setter
					private final String id = "";

					public static class Name {
						private String shown;
					}
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"immutable-model $MODEL/Session.java:4 declares the field userId of Session without final; $ASKED",
			"immutable-model $MODEL/Session.java:6 declares the setter setUserId() of Session; $ASKED",
			"immutable-model $MODEL/device/Device.java:6 carries @Data on the class Device; $ASKED",
			"immutable-model $MODEL/device/Device.java:8 carries @Setter on the field id of Device; $ASKED",
			"immutable-model $MODEL/device/Device.java:12 declares the field shown of Name without final; $ASKED",
		)
	}

	private companion object {
		const val MODEL = "session-api/src/main/java/com/example/session/model"
		const val ASKED = "a model is immutable: its fields are final, and a changed value is a new model"

		/** A class with a field that can change and the setter that changes it. */
		const val MUTABLE = "public class Session {\n\tprivate String userId;\n\n" +
			"\tpublic void setUserId(String userId) {\n\t\tthis.userId = userId;\n\t}\n}\n"
	}
}
