package me.whereareiam.toolkit.shipshape.rule.tests

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TestNameRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = TestNameRule()

	@Test
	fun `a test is named after a class of the same package in the same module`() {
		val fixture = LayoutFixture.service(directory)
			.java(COMMON, "main", "com.example.session.DefaultSessionService")
			.java(COMMON, "test", "com.example.session.DefaultSessionServiceTest", test("DefaultSessionServiceTest"))
			.java(COMMON, "test", "com.example.session.FakeSessionStore")
			.java("", "main", "com.example.session.SessionServiceApplication")
			.java("", "test", "com.example.session.SessionServiceApplicationTest", test("SessionServiceApplicationTest"))

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a test of a class of another package or module mirrors nothing`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.Session")
			.java(COMMON, "main", "com.example.session.DefaultSessionService")
			.java(COMMON, "test", "com.example.session.SessionTest", test("SessionTest"))
			.java(COMMON, "test", "com.example.session.store.DefaultSessionServiceTest", test("DefaultSessionServiceTest"))

		assertThat(fixture.breaches(rule)).containsExactly(
			"test-name $COMMON/$TESTS/SessionTest.java is named after no class of com.example.session in :session-common",
			"test-name $COMMON/$TESTS/store/DefaultSessionServiceTest.java is named after no class of com.example.session.store in " +
				":session-common",
		)
	}

	@Test
	fun `a class that holds tests is named as a test`() {
		val fixture = LayoutFixture.service(directory)
			.java(COMMON, "test", "com.example.session.SessionScenarios", test("SessionScenarios"))

		assertThat(fixture.breaches(rule)).containsExactly(
			"test-name $COMMON/$TESTS/SessionScenarios.java holds tests and is not named <Class>Test or <Class>IntegrationTest",
		)
	}

	@Test
	fun `the tag integration and the name IntegrationTest go together`() {
		val fixture = LayoutFixture.service(directory)
			.java(COMMON, "main", "com.example.session.DefaultSessionService")
			.java(
				COMMON, "test", "com.example.session.DefaultSessionServiceIntegrationTest", tagged("DefaultSessionServiceIntegrationTest"),
			)
			.java(COMMON, "test", "com.example.session.DefaultSessionServiceTest", tagged("DefaultSessionServiceTest"))
			.java(GRPC, "main", "com.example.session.adapter.grpc.GrpcSessionService")
			.java(
				GRPC, "test", "com.example.session.adapter.grpc.GrpcSessionServiceIntegrationTest",
				test("GrpcSessionServiceIntegrationTest"),
			)

		assertThat(fixture.breaches(rule)).containsExactly(
			"test-name $COMMON/$TESTS/DefaultSessionServiceTest.java is tagged integration and is not named <Class>IntegrationTest",
			"test-name $GRPC/$TESTS/adapter/grpc/GrpcSessionServiceIntegrationTest.java is named <Class>IntegrationTest and does not " +
				"carry @Tag(\"integration\")",
		)
	}

	@Test
	fun `the plain test of a class named Integration needs no tag`() {
		val fixture = LayoutFixture.service(directory)
			.java(COMMON, "main", "com.example.session.AccountIntegration")
			.java(COMMON, "test", "com.example.session.AccountIntegrationTest", test("AccountIntegrationTest"))

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	/** A class with one test. */
	private fun test(type: String): String = "import org.junit.jupiter.api.Test;\n\nclass $type {\n\t@Test\n\tvoid works() {\n\t}\n}\n"

	/** A class with one test, tagged as one that needs Docker. */
	private fun tagged(type: String): String =
		"import org.junit.jupiter.api.Tag;\n${test(type).replace("\n\nclass ", "\n\n@Tag(\"integration\")\nclass ")}"

	private companion object {
		const val COMMON = "session-common"
		const val GRPC = "session-adapter/adapter-grpc"
		const val TESTS = "src/test/java/com/example/session"
	}
}
