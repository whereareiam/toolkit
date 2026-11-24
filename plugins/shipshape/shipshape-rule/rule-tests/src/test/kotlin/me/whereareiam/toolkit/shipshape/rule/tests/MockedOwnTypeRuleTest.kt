package me.whereareiam.toolkit.shipshape.rule.tests

import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class MockedOwnTypeRuleTest {
	@TempDir
	lateinit var directory: Path

	private val rule = MockedOwnTypeRule()

	@Test
	fun `Mockito stands in for a foreign type`() {
		val fixture = LayoutFixture.service(directory).java(GRPC, "test", "com.example.session.adapter.grpc.GrpcSessionServiceTest", """
			import io.grpc.stub.StreamObserver;
			import jakarta.servlet.http.HttpServletRequest;

			import static org.mockito.Mockito.mock;

			class GrpcSessionServiceTest {
				private final HttpServletRequest request = mock(HttpServletRequest.class);
				private final StreamObserver<?> observer = mock(StreamObserver.class);
				private final Runnable task = mock(Runnable.class);
			}
		""".trimIndent())

		assertThat(fixture.breaches(rule)).isEmpty()
	}

	@Test
	fun `a type of the build's own is faked, not mocked`() {
		val fixture = LayoutFixture.service(directory)
			.java(GRPC, "main", "com.example.session.adapter.grpc.SessionMessageMapper")
			.java(GRPC, "test", "com.example.session.adapter.grpc.GrpcSessionServiceTest", """
				import com.example.session.SessionService;

				import static org.mockito.Mockito.mock;
				import static org.mockito.Mockito.spy;

				class GrpcSessionServiceTest {
					private final SessionService sessions = mock(SessionService.class);
					private final SessionMessageMapper mapper = spy(SessionMessageMapper.class);
					private final Object stub = mock(com.example.protocol.session.v1.SessionServiceGrpc.SessionServiceBlockingStub.class);
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"mocked-own-type $GRPC/src/test/java/com/example/session/adapter/grpc/GrpcSessionServiceTest.java:9 " +
				"mocks SessionService, a type of the build's own; write a fake of it, Mockito stands in for a foreign type only",
			"mocked-own-type $GRPC/src/test/java/com/example/session/adapter/grpc/GrpcSessionServiceTest.java:10 " +
				"mocks SessionMessageMapper, a type of the build's own; write a fake of it, Mockito stands in for a foreign type only",
			"mocked-own-type $GRPC/src/test/java/com/example/session/adapter/grpc/GrpcSessionServiceTest.java:11 " +
				"mocks com.example.protocol.session.v1.SessionServiceGrpc.SessionServiceBlockingStub, a type of the build's own; write a " +
					"fake of it, Mockito stands in for a foreign type only",
		)
	}

	@Test
	fun `every package root of the build is its own, and a package that only begins like one is foreign`() {
		val fixture = LayoutFixture(directory, "session-service", packageRoots = setOf("com.example", "org.partner"))
			.module(":session-common", ArchitectureKind.IMPLEMENTATION)
			.java("session-common", "test", "com.example.session.DefaultSessionServiceTest", """
				import org.partner.catalogue.ProblemCatalogue;
				import org.partnership.Agreement;

				import static org.mockito.Mockito.mock;

				class DefaultSessionServiceTest {
					private final ProblemCatalogue problems = mock(ProblemCatalogue.class);
					private final Agreement agreement = mock(Agreement.class);
				}
			""".trimIndent())

		assertThat(fixture.breaches(rule)).containsExactly(
			"mocked-own-type session-common/src/test/java/com/example/session/DefaultSessionServiceTest.java:9 mocks ProblemCatalogue, " +
				"a type of the build's own; write a fake of it, Mockito stands in for a foreign type only",
		)
	}

	private companion object {
		const val GRPC = "session-adapter/adapter-grpc"
	}
}
