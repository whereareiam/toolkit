package me.whereareiam.toolkit.shipshape.rule.tests

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ModuleTestRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = ModuleTestRule()

	@Test
	fun `a module with a test passes, and so does one whose classes have nothing to run`() {
		val fixture = LayoutFixture.service(directory)
			.java(GRPC, "main", "com.example.session.adapter.grpc.SessionMessageMapper", doing("SessionMessageMapper"))
			.java(GRPC, "test", "com.example.session.adapter.grpc.SessionMessageMapperTest", TEST)
			.java("session-api", "main", "com.example.session.SessionService", "public interface SessionService {\n}\n")
			.java("session-api", "main", "com.example.session.model.Session", "@lombok.Value\npublic class Session {\n}\n")
			.java("session-api", "main", "com.example.session.exception.SessionException", doing("SessionException"))

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a module whose classes do something and that has no test is refused, a test of another source set being none`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.SessionService", "public interface SessionService {\n}\n")
			.java("session-api", "main", "com.example.session.config.SessionOptions", doing("SessionOptions"))
			.java("session-api", "testFixtures", "com.example.session.FakeSessionService", doing("FakeSessionService"))

		assertThat(fixture.breaches(rule)).containsExactly(
			"module-test session-api/src/main/java holds SessionOptions, which does something, and :session-api has no test; a module " +
				"whose classes do something holds a test",
		)
	}

	@Test
	fun `the root project is named by its name`() {
		val fixture = LayoutFixture.service(directory)
			.java("", "main", "com.example.session.SessionServiceApplication", doing("SessionServiceApplication"))

		assertThat(fixture.breaches(rule)).containsExactly(
			"module-test src/main/java holds SessionServiceApplication, which does something, and session-service has no test; a module " +
				"whose classes do something holds a test",
		)
	}

	/** A class with one method that has a body. */
	private fun doing(type: String): String = "public class $type {\n\tvoid run() {\n\t}\n}\n"

	private companion object {
		const val GRPC = "session-adapter/adapter-grpc"
		const val TEST = "import org.junit.jupiter.api.Test;\n\nclass SessionMessageMapperTest {\n\t@Test\n\tvoid works() {\n\t}\n}\n"
	}
}
