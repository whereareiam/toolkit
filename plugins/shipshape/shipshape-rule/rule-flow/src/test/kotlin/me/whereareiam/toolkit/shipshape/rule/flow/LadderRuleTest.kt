package me.whereareiam.toolkit.shipshape.rule.flow

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LadderRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = LadderRule()

	@Test
	fun `two alike and a run of guards that test different things pass`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Acts", """
			public class Acts {
				java.util.List<String> acts(Topic topic, User user) {
					if (topic == null) throw new IllegalArgumentException("topic");
					if (user == null) throw new IllegalArgumentException("user");
					if (user.banned()) return java.util.List.of();

					java.util.List<String> can = new java.util.ArrayList<>();
					if (topic.open()) can.add("reply");
					if (user.staff()) can.add("close");

					return can;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `three ifs in a row that do the same thing, or test the same thing, are a ladder`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Acts", """
			public class Acts {
				java.util.List<String> acts(Topic topic, User user) {
					java.util.List<String> can = new java.util.ArrayList<>();
					if (topic.open()) can.add("reply");
					if (user.staff()) can.add("close");
					if (user.owns(topic)) can.add("edit");

					return can;
				}

				String wrong(String name) {
					if (name.equals("id")) return "an id";
					if (name.equals("ids")) return "ids";
					if (name.endsWith("_at")) return "a time";

					return null;
				}
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"ladder session-common/src/main/java/com/example/session/Acts.java:6 begins a ladder of ifs that call can.add; a ladder " +
				"is a table, behaviour on an enum, or a sealed type with a pattern switch",
			"ladder session-common/src/main/java/com/example/session/Acts.java:14 begins a ladder of ifs that test name; a ladder is a " +
				"table, behaviour on an enum, or a sealed type with a pattern switch",
			"ladder session-common/src/main/java/com/example/session/Acts.java:14 begins a ladder of ifs that return a text; a ladder " +
				"is a table, behaviour on an enum, or a sealed type with a pattern switch",
		)
	}
}
