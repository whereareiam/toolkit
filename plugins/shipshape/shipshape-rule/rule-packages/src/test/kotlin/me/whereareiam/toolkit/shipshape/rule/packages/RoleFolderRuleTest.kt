package me.whereareiam.toolkit.shipshape.rule.packages

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RoleFolderRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = RoleFolderRule()

	@Test
	fun `an api's enum lies below type, its options below config, and an exception below exception in every module`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.type.SessionKind", "public enum SessionKind {\n\tWEB\n}\n")
			.java("session-api", "main", "com.example.session.type.token.TokenKind", "public enum TokenKind {\n\tOPAQUE\n}\n")
			.java("session-api", "main", "com.example.session.config.SessionOptions")
			.java("session-api", "main", "com.example.session.exception.SessionException", thrown("SessionException", "RuntimeException"))
			.java(
				"session-api", "main", "com.example.session.model.Session",
				"public class Session {\n\tpublic enum State {\n\t\tOPEN\n\t}\n}\n",
			)
			.java(
				GRPC, "main", "com.example.session.adapter.grpc.exception.StatusFailure", thrown("StatusFailure", "IllegalStateException"),
			)
			.java(GRPC, "main", "com.example.session.adapter.grpc.CallKind", "enum CallKind {\n\tUNARY\n}\n")
			.java(GRPC, "main", "com.example.session.adapter.grpc.ChannelOptions")
			.java("session-api", "test", "com.example.session.FakeFailure", thrown("FakeFailure", "RuntimeException"))

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a type of such a kind outside of its folder is refused`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.SessionKind", "public enum SessionKind {\n\tWEB\n}\n")
			.java("session-api", "main", "com.example.session.model.SessionOptions")
			.java("session-api", "main", "com.example.session.SessionException", thrown("SessionException", "RuntimeException"))
			.java(GRPC, "main", "com.example.session.adapter.grpc.StatusFailure", thrown("StatusFailure", "java.io.IOException"))
			.java("", "main", "com.example.session.StartFailure", thrown("StartFailure", "Error"))

		assertThat(fixture.breaches(rule)).containsExactly(
			"role-folder src/main/java/com/example/session/StartFailure.java:3 declares StartFailure outside of exception/; $EXCEPTION",
			"role-folder $API/SessionException.java:3 declares SessionException outside of exception/; $EXCEPTION",
			"role-folder $API/SessionKind.java:3 declares SessionKind outside of type/; an enum of an api lies below type/",
			"role-folder $API/model/SessionOptions.java:3 declares SessionOptions outside of config/; the options of an api lie below " +
				"config/",
			"role-folder $GRPC/src/main/java/com/example/session/adapter/grpc/StatusFailure.java:3 declares StatusFailure outside of " +
				"exception/; $EXCEPTION",
		)
	}

	/** A class that extends [parent] and holds nothing. */
	private fun thrown(type: String, parent: String): String = "public class $type extends $parent {\n}\n"

	private companion object {
		const val GRPC = "session-adapter/adapter-grpc"
		const val API = "session-api/src/main/java/com/example/session"
		const val EXCEPTION = "an exception lies below exception/, also a single one"
	}
}
