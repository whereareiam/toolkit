package me.whereareiam.toolkit.shipshape.api.java

import me.whereareiam.toolkit.shipshape.api.type.RoleFolder
import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class JavaSourceTest {
	@TempDir
	lateinit var directory: Path

	private val source by lazy {
		LayoutFixture.service(directory).java("session-api", "test", "com.example.session.model.token.TokenPairTest", """
			import java.util.List;
			import static org.assertj.core.api.Assertions.*;

			// sleep("comment")
			class TokenPairTest {
				String word = "sleep(\"literal\")";

				void waits() {
					sleep("code");
				}
			}
		""".trimIndent()).repository().all.single()
	}

	@Test
	fun `a file knows where it lies and what it declares`() {
		assertThat(source.path).isEqualTo("session-api/src/test/java/com/example/session/model/token/TokenPairTest.java")
		assertThat(source.folder).isEqualTo("session-api/src/test/java/com/example/session/model/token")
		assertThat(source.module.path).isEqualTo(":session-api")
		assertThat(source.packageName).isEqualTo("com.example.session.model.token").isEqualTo(source.folderPackage)
		assertThat(source.qualifiedName).isEqualTo("com.example.session.model.token.TokenPairTest")
		assertThat(source.imports).containsExactly("java.util.List", "org.assertj.core.api.Assertions.*")
	}

	@Test
	fun `a file is of one source set, and only main is delivered`() {
		assertThat(source.main).isFalse()
		assertThat(source.sourceSet).isEqualTo(JavaSource.TEST)
		assertThat(source.isOf(JavaSource.TEST)).isTrue()
		assertThat(source.siblingFolder(JavaSource.MAIN))
			.isEqualTo(directory.resolve("session-api/src/main/java/com/example/session/model/token").toFile())
	}

	@Test
	fun `a file lies below a role folder where its package holds that folder's name`() {
		assertThat(RoleFolder.entries.filter(source::isBelow)).containsExactly(RoleFolder.MODEL)
	}

	@Test
	fun `a mapper is told by its name`() {
		val sources = directory.resolve("src/main/java").toFile()
		val mapper = JavaSource(sources.resolve("com/example/session/SessionMessageMapper.java"), sources, directory.toFile(), ROOT)

		assertThat(mapper.mapper).isTrue()
		assertThat(source.mapper).isFalse()
	}

	@Test
	fun `a pattern is found in code with its line, in a literal only when asked, and never in a comment`() {
		val call = Regex("""sleep\("?(\w*)""")

		assertThat(source.find(call).map { "${it.line} ${it.group(1)}" }).containsExactly("11 ")
		assertThat(source.find(call, literals = true).map { "${it.line} ${it.group(1)}" }).containsExactly("8 ", "11 code")
		assertThat(source.lines[5]).isEqualTo("// sleep(\"comment\")")
	}

	private companion object {
		val ROOT = JavaModule(":", "", "session-service")
	}
}
