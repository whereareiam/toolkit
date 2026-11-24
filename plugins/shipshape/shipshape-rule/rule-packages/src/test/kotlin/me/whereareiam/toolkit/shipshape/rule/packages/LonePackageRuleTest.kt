package me.whereareiam.toolkit.shipshape.rule.packages

import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LonePackageRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = LonePackageRule()

	@Test
	fun `a fixed role folder, a module's own package and a package that groups others may hold one file`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.SessionService")
			.java("session-api", "main", "com.example.session.config.SessionOptions")
			.java("session-api", "main", "com.example.session.exception.SessionException")
			.java("session-api", "main", "com.example.session.model.Session")
			.java("session-api", "main", "com.example.session.type.SessionKind")
			.java("session-adapter/adapter-grpc", "main", "com.example.session.adapter.grpc.GrpcSessionService")
			.java("session-common", "main", "com.example.session.DefaultSessionService")
			.java("session-common", "main", "com.example.session.token.OpaqueTokens")
			.java("session-common", "main", "com.example.session.token.seal.ReplaySeal")
			.java("session-common", "main", "com.example.session.token.seal.SealedAnswer")

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a package made for one file is refused`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.DefaultSessionService")
			.java("session-common", "main", "com.example.session.token.OpaqueTokens")
			.java("session-api", "main", "com.example.session.SessionService")
			.java("session-api", "main", "com.example.session.model.token.TokenPair")

		assertThat(fixture.breaches(rule)).containsExactly(
			"lone-package session-api/src/main/java/com/example/session/model/token is a package made for TokenPair alone; put the file " +
				"where it belongs, a package is made for two or more",
			"lone-package session-common/src/main/java/com/example/session/token is a package made for OpaqueTokens alone; put the file " +
				"where it belongs, a package is made for two or more",
		)
	}

	@Test
	fun `a module's own package is the one every package of its main code lies in, also where it holds one file`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-api", "main", "com.example.session.model.token.TokenPair")
			.java("session-integration/integration-account", "main", "com.example.session.integration.account.AccountClient")
			.java("session-integration/integration-account", "test", "com.example.session.integration.account.AccountClientTest")

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `the one test of a package that main code has mirrors it, and a package only tests have is held`() {
		val fixture = LayoutFixture.service(directory)
			.java("session-common", "main", "com.example.session.token.OpaqueTokens")
			.java("session-common", "main", "com.example.session.token.ReplaySeal")
			.java("session-common", "test", "com.example.session.token.OpaqueTokensTest")
			.java("session-common", "test", "com.example.session.fake.FakeSessionStore")

		assertThat(fixture.breaches(rule)).containsExactly(
			"lone-package session-common/src/test/java/com/example/session/fake is a package made for FakeSessionStore alone; put the " +
				"file where it belongs, a package is made for two or more",
		)
	}
}
