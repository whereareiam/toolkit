package me.whereareiam.toolkit.shipshape.rule.models

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CollectionCopyRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = CollectionCopyRule()

	@Test
	fun `a model copies the collection it is handed, or lets the builder collect it`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.Session", """
				import java.util.List;
				import java.util.Map;
				import java.util.Set;
				import lombok.Builder;
				import lombok.Singular;

				@Builder
				public class Session {
					private static final List<String> KINDS = List.of("web");

					@Singular
					private final List<String> devices;
					private final Set<String> scopes;
					private final Map<String, String> labels;
					private final List<String> notes = new java.util.ArrayList<>();
					private final List<String> names;
					private final List<String> tags;
					private final String userId;

					Session(List<String> devices, Set<String> scopes, Map<String, String> labels, List<String> names, String userId) {
						this.devices = devices;
						this.scopes = Set.copyOf(scopes);
						this.labels = labels == null ? Map.of() : Map.copyOf(labels);
						this.names = names.stream().map(String::strip).toList();
						this.tags = (names == null ? null : new java.util.ArrayList<>(names));
						this.userId = userId;
					}
				}
			""".trimIndent())
			.java("session-common", "main", "com.example.session.SessionSweeper", HOLDING.format("SessionSweeper"))

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a new collection made of the one handed in is a copy, and the builder collects for the constructor it calls`() {
		val fixture = LayoutFixture.service(directory).java("session-api", "main", "com.example.session.model.Caller", """
			import java.util.Collections;
			import java.util.LinkedHashSet;
			import java.util.Set;
			import lombok.Builder;
			import lombok.Singular;

			public class Caller {
				private final Set<String> groups;
				private final Set<String> scopes;

				@Builder
				private Caller(@Singular Set<String> groups, Set<String> scopes) {
					this.groups = groups;
					this.scopes = Collections.unmodifiableSet(new LinkedHashSet<>(scopes));
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a model that keeps the collection it was handed is refused, on one side of a choice, as a view and as a default of the builder`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.Session", HOLDING.format("Session"))
			.java("session-api", "main", "com.example.session.model.device.DeviceList", """
				import java.util.Collections;
				import java.util.List;

				public class DeviceList {
					private final java.util.Map<String, List<String>> byUser;

					DeviceList(java.util.Map<String, List<String>> byUser) {
						this.byUser = Collections.unmodifiableMap(byUser);
					}
				}
			""".trimIndent())
			.java("session-api", "main", "com.example.session.model.device.Device", """
				import java.util.List;
				import lombok.Builder;

				@Builder
				public class Device {
					@Builder.Default
					private final List<String> names = List.of();
				}
			""".trimIndent())
			.java("session-api", "main", "com.example.session.model.device.DeviceNames", """
				import java.util.ArrayList;
				import java.util.List;

				public class DeviceNames {
					private final List<String> names;

					DeviceNames(List<String> names) {
						this.names = names == null ? new ArrayList<>() : names;
					}
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"collection-copy $MODEL/Session.java:8 keeps the List devices as it was handed in; $ASKED",
			"collection-copy $MODEL/device/Device.java:8 keeps the List names as it was handed in; $ASKED",
			"collection-copy $MODEL/device/DeviceList.java:7 keeps the Map byUser as it was handed in; $ASKED",
			"collection-copy $MODEL/device/DeviceNames.java:7 keeps the List names as it was handed in; $ASKED",
		)
	}

	private companion object {
		const val MODEL = "session-api/src/main/java/com/example/session/model"
		const val ASKED = "copy it where it is assigned (copyOf), or let the builder collect it (@Singular)"
		const val HOLDING = "import java.util.List;\nimport lombok.Value;\n\n@Value\npublic class %s {\n\tList<String> devices;\n}\n"
	}
}
