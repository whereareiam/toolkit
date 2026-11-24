package me.whereareiam.toolkit.shipshape.verification.task

import com.sun.source.tree.MethodTree
import me.whereareiam.toolkit.shipshape.api.java.JavaModule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

class JdkParserTest {
	@TempDir
	lateinit var directory: Path

	@Test
	fun `the JDK that runs the tests reads its own language level and every older one, and says so of a newer one`() {
		val running = Runtime.version().feature()

		assertThat(JdkParser(null).obstacle).isNull()
		assertThat(JdkParser(running).obstacle).isNull()
		assertThat(JdkParser(17).obstacle).isNull()
		assertThat(JdkParser(running + 1).obstacle).isEqualTo("cannot parse Java ${running + 1}")
	}

	@Test
	fun `a file is read as it is written, without what Lombok would add and with nothing resolved`() {
		val parsed = JdkParser(null).parse(listOf(source("Session", """
			package com.example.session.model;

			import lombok.Getter;
			import com.example.nowhere.Missing;

			@Getter
			public class Session {
				private final Missing owner;

				sealed interface Kind permits Web, Program {
				}

				String describe(Object value) {
					return switch (value) {
						case String text when !text.isEmpty() -> text;
						case null, default -> ${"\"\"\""}
							none
							${"\"\"\""};
					};
				}
			}
		"""))).single()

		assertThat(parsed.types.map { it.simpleName.toString() }).containsExactly("Session", "Kind")
		val methods = parsed.types.first().members.filterIsInstance<MethodTree>()

		assertThat(methods.map { "${it.name}:${parsed.lineOf(it)}" }).containsExactly("describe:13")
	}

	@Test
	fun `a file with a syntax error is left to the compiler, and the others are read`() {
		val broken = source("Broken", "package com.example.session;\n\npublic class Broken {\n\tvoid open( {\n}\n")
		val whole = source("Whole", "package com.example.session;\n\npublic class Whole {\n}\n")

		val parsed = JdkParser(null).parse(listOf(broken, whole))

		assertThat(parsed.map { it.source.typeName }).containsExactly("Whole")
	}

	@Test
	fun `nothing to read is nothing read`() {
		assertThat(JdkParser(null).parse(emptyList())).isEmpty()
	}

	private fun source(type: String, text: String): JavaSource {
		val root = directory.resolve("src/main/java")
		val file = root.resolve("$type.java").createParentDirectories()
		file.writeText(text.trimIndent())

		return JavaSource(file.toFile(), root.toFile(), directory.toFile(), JavaModule(":", "", "session-service"))
	}
}
