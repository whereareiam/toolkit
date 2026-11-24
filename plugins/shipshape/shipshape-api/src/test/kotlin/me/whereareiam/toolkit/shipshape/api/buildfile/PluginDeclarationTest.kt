package me.whereareiam.toolkit.shipshape.api.buildfile

import me.whereareiam.toolkit.shipshape.api.buildfile.PluginDeclaration.Kind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PluginDeclarationTest {
	@Test
	fun `a plugin Gradle brings is written by its accessor`() {
		assertThat(applied("`java-test-fixtures`").let { it.name to it.kind }).isEqualTo("java-test-fixtures" to Kind.BUILT_IN)
		assertThat(applied("base").let { it.name to it.kind }).isEqualTo("base" to Kind.BUILT_IN)
		assertThat(applied("""id("org.gradle.maven-publish")""").kind).isEqualTo(Kind.BUILT_IN)
	}

	@Test
	fun `a plugin of somebody else is written by its id, by an alias or as Kotlin's`() {
		assertThat(applied("""id("build.buf")""").let { it.name to it.kind }).isEqualTo("build.buf" to Kind.EXTERNAL)
		assertThat(applied("alias(libs.plugins.buf)").let { it.name to it.kind }).isEqualTo("buf" to Kind.EXTERNAL)
		assertThat(applied("""kotlin("jvm")""").let { it.name to it.kind }).isEqualTo("kotlin-jvm" to Kind.EXTERNAL)
	}

	@Test
	fun `a convention lies in a namespace the build names its conventions by, or has an id of one word`() {
		assertThat(applied("""id("com.example.convention.adapter")""").kind).isEqualTo(Kind.CONVENTION)
		assertThat(applied("""id("com.example.convention")""").kind).isEqualTo(Kind.CONVENTION)
		assertThat(applied("""id("problem-catalogue")""").kind).isEqualTo(Kind.CONVENTION)
		assertThat(applied("""id("com.example.conventions")""").kind).isEqualTo(Kind.EXTERNAL)
		assertThat(PluginDeclaration.of(2, """id("com.example.convention.adapter")""")?.kind).isEqualTo(Kind.EXTERNAL)
	}

	@Test
	fun `a version is read where a line gives one, and a plugin that is only named has none`() {
		assertThat(applied("""id("org.openapi.generator") version "7.25.0"""").version).isEqualTo("7.25.0")
		assertThat(applied("""id("build.buf") version "0.11.1" apply false""").version).isEqualTo("0.11.1")
		assertThat(applied("""id("build.buf") apply false""").let { it.name to it.version }).isEqualTo("build.buf" to null)
		assertThat(applied("`version-catalog`").let { it.name to it.version }).isEqualTo("version-catalog" to null)
	}

	@Test
	fun `a line that applies nothing in a known form is no declaration`() {
		assertThat(PluginDeclaration.of(2, """register("convention") {""")).isNull()
	}

	private fun applied(line: String): PluginDeclaration = PluginDeclaration.of(2, "\t$line", setOf("com.example.convention"))!!
}
