package me.whereareiam.toolkit.shipshape.api.java

import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.source.tree.MethodTree
import com.sun.source.tree.ReturnTree
import com.sun.source.tree.VariableTree
import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import me.whereareiam.toolkit.shipshape.verification.task.JdkParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ParsedSourceTest {
	@TempDir
	lateinit var directory: Path

	private val parsed by lazy {
		val fixture = LayoutFixture.service(directory).java("session-api", "main", "com.example.session.model.Session", """
			public class Session {
				private static final int LIMIT = 3;

				@lombok.Singular
				private final java.util.List<String> devices;

				@Deprecated
				Session(String userId) {
				}

				String userId() {
					return this.userId;
				}

				String shown() {
					return this
						.userId()
						.strip();
				}

				enum Kind {
					WEB
				}

				record Device(String id, int number) {
					static final Device NONE = new Device("", 0);
				}
			}

			interface Sessions {
			}
		""".trimIndent())

		JdkParser(null).parse(fixture.repository().all).single()
	}

	@Test
	fun `every type of a file is found, the nested ones behind the type that holds them`() {
		assertThat(parsed.types.map { it.simpleName.toString() }).containsExactly("Session", "Kind", "Device", "Sessions")
		assertThat(parsed.topLevel.map { it.simpleName.toString() }).containsExactly("Session", "Sessions")
		assertThat(parsed.source.typeName).isEqualTo("Session")
	}

	@Test
	fun `the fields of an instance are those that are not static, and of a record its components`() {
		val device = parsed.types.single { it.simpleName.contentEquals("Device") }

		assertThat(ParsedSource.instanceFields(parsed.types.first()).map { it.name.toString() }).containsExactly("devices")
		assertThat(ParsedSource.instanceFields(device).map { it.name.toString() }).containsExactly("id", "number")
	}

	@Test
	fun `a call is placed at the line its name is written on, wherever its chain began`() {
		val shown = members<MethodTree>().single { it.name.contentEquals("shown") }
		val strip = (shown.body.statements.single() as ReturnTree).expression as MethodInvocationTree
		val userId = (strip.methodSelect as MemberSelectTree).expression as MethodInvocationTree

		assertThat(parsed.lineOf(strip)).isEqualTo(18)
		assertThat(parsed.lineOfName(userId)).isEqualTo(19)
		assertThat(parsed.lineOfName(strip)).isEqualTo(20)
	}

	@Test
	fun `a tree is placed at the line it begins on, its annotations included`() {
		assertThat(members<MethodTree>().map { "${it.name} ${parsed.lineOf(it)}" }).containsExactly("<init> 9", "userId 13", "shown 17")
	}

	@Test
	fun `a name is read without its package, and a field through this`() {
		val field = members<VariableTree>().last()
		val returned = (members<MethodTree>()[1].body.statements.single() as ReturnTree).expression

		assertThat(ParsedSource.simpleName(field.type)).isNull()
		assertThat(ParsedSource.names(field.modifiers.annotations)).containsExactly("Singular")
		assertThat(ParsedSource.annotated(field.modifiers, setOf("Singular"))).isTrue()
		assertThat(ParsedSource.annotated(field.modifiers, setOf("NotNull"))).isFalse()
		assertThat(ParsedSource.ownField(returned)).isEqualTo("userId")
		assertThat(ParsedSource.simpleName(returned)).isEqualTo("userId")
		assertThat(ParsedSource.ownField(field.type)).isNull()
	}

	@Test
	fun `an annotation is read by its full name, written in full or imported by name or with its package`() {
		val fixture = LayoutFixture.service(directory).java("session-common", "main", "com.example.session.Sessions", """
			import jakarta.inject.Inject;
			import org.spongepowered.asm.mixin.injection.*;

			public class Sessions {
				@Inject
				void injected() {
				}

				@Redirect
				void redirected() {
				}

				@com.llamalad7.mixinextras.injector.ModifyReceiver
				void received() {
				}

				@Unknown
				void unknown() {
				}
			}
		""".trimIndent())
		val sessions = JdkParser(null).parse(fixture.repository().all).single()
		val wanted = setOf(
			"org.spongepowered.asm.mixin.injection.Inject",
			"org.spongepowered.asm.mixin.injection.Redirect",
			"com.llamalad7.mixinextras.injector.ModifyReceiver",
		)

		assertThat(sessions.methods.map { (_, method) -> "${method.name} ${sessions.annotatedAs(method.modifiers, wanted)}" })
			.containsExactly("injected false", "redirected true", "received true", "unknown false")
	}

	@Test
	fun `a constructor is told from a method`() {
		assertThat(members<MethodTree>().map(ParsedSource::isConstructor)).containsExactly(true, false, false)
	}

	private inline fun <reified T> members(): List<T> = parsed.types.first().members.filterIsInstance<T>()
}
